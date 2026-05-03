#!/usr/bin/env bash
# Build the mod, fetch required Fabric API jar (cached), and push everything
# to a connected Android device's PojavLauncher mods directory.
#
# Usage: scripts/deploy.sh [--skip-build] [--mods-dir <path>] [--serial <adb-serial>]
#
# Reads minecraft_version / loader_version / fabric_version / mod_version
# from gradle.properties so it stays in sync with the project.

set -euo pipefail

# ---- Config ---------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CACHE_DIR="${SUPER_TNT_DEPLOY_CACHE:-$HOME/.cache/super-tnt-mod-deploy}"

# Legacy world-readable PojavLauncher path (older versions, no scoped storage).
LEGACY_POJAV_MINECRAFT=(
  "/sdcard/games/PojavLauncher/.minecraft"
  "/storage/emulated/0/games/PojavLauncher/.minecraft"
)

# Modern (Android 11+) PojavLauncher and forks live under scoped storage:
#   /sdcard/Android/data/<package>/files/.minecraft
# Discovered dynamically via `pm list packages`.
POJAV_PACKAGE_PATTERN='pojavlaunch|zalithlauncher|pojav'

MODRINTH_FABRIC_API_PROJECT_ID="P7dR8mSH"

# ANSI colors
C_INFO=$'\033[1;34m'
C_WARN=$'\033[1;33m'
C_ERR=$'\033[1;31m'
C_OK=$'\033[1;32m'
C_OFF=$'\033[0m'

# ---- Args -----------------------------------------------------------------
SKIP_BUILD=0
MODS_DIR_OVERRIDE=""
ADB_SERIAL=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build) SKIP_BUILD=1; shift ;;
    --mods-dir)   MODS_DIR_OVERRIDE="$2"; shift 2 ;;
    --serial)     ADB_SERIAL="$2"; shift 2 ;;
    -h|--help)
      sed -n '2,9p' "$0"; exit 0 ;;
    *) echo "Unknown arg: $1" >&2; exit 2 ;;
  esac
done

ADB=(adb)
[[ -n "$ADB_SERIAL" ]] && ADB+=(-s "$ADB_SERIAL")

log()  { printf '%s==>%s %s\n' "$C_INFO" "$C_OFF" "$*"; }
warn() { printf '%s!! %s %s\n'  "$C_WARN" "$C_OFF" "$*" >&2; }
die()  { printf '%sxx %s %s\n'  "$C_ERR"  "$C_OFF" "$*" >&2; exit 1; }
ok()   { printf '%sOK%s %s\n'   "$C_OK"   "$C_OFF" "$*"; }

# ---- Read project metadata ------------------------------------------------
prop() {
  local key="$1"
  grep -E "^${key}=" "$PROJECT_ROOT/gradle.properties" | head -1 | cut -d= -f2-
}

MC_VERSION="$(prop minecraft_version)"
LOADER_VERSION="$(prop loader_version)"     # fabric-loader, e.g. "0.18.2"
FABRIC_API_RAW="$(prop fabric_version)"     # fabric-api, e.g. "0.139.4+1.21.11"
MOD_VERSION="$(prop mod_version)"
ARCHIVE_BASE="$(prop archives_base_name)"

[[ -n "$MC_VERSION" && -n "$LOADER_VERSION" && -n "$FABRIC_API_RAW" ]] \
  || die "Could not read versions from gradle.properties"

FABRIC_API_FILENAME="fabric-api-${FABRIC_API_RAW}.jar"
FABRIC_API_CACHE="$CACHE_DIR/$FABRIC_API_FILENAME"
FABRIC_PROFILE_ID="fabric-loader-${LOADER_VERSION}-${MC_VERSION}"
VANILLA_DIR_CACHE="$CACHE_DIR/versions/${MC_VERSION}"
FABRIC_DIR_CACHE="$CACHE_DIR/versions/${FABRIC_PROFILE_ID}"

# ---- Tooling check --------------------------------------------------------
command -v adb >/dev/null     || die "adb not found in PATH (install platform-tools)"
command -v curl >/dev/null    || die "curl not found"
command -v python3 >/dev/null || die "python3 not found (used to query Modrinth API)"

# ---- 1. Build -------------------------------------------------------------
if [[ "$SKIP_BUILD" -eq 0 ]]; then
  log "Building mod (./gradlew build)"
  ( cd "$PROJECT_ROOT" && ./gradlew build )
else
  log "Skipping build (--skip-build)"
fi

MOD_JAR="$PROJECT_ROOT/build/libs/${ARCHIVE_BASE}-${MOD_VERSION}.jar"
[[ -f "$MOD_JAR" ]] || die "Mod jar not found at $MOD_JAR — build failed?"

# ---- 2. Fabric API (cached) ----------------------------------------------
mkdir -p "$CACHE_DIR"
if [[ ! -f "$FABRIC_API_CACHE" ]]; then
  log "Resolving Fabric API ${FABRIC_API_RAW} download URL via Modrinth"
  DOWNLOAD_URL="$(python3 - <<PY
import json, sys, urllib.parse, urllib.request
mc = "$MC_VERSION"
want = "$FABRIC_API_RAW"
url = ("https://api.modrinth.com/v2/project/$MODRINTH_FABRIC_API_PROJECT_ID/version"
       "?game_versions=" + urllib.parse.quote(json.dumps([mc])) +
       "&loaders=" + urllib.parse.quote(json.dumps(["fabric"])))
req = urllib.request.Request(url, headers={"User-Agent": "super-tnt-mod-deploy/1"})
data = json.load(urllib.request.urlopen(req, timeout=30))
for v in data:
    if v.get("version_number") == want:
        print(v["files"][0]["url"])
        sys.exit(0)
sys.exit("no Modrinth release matches version_number=" + want)
PY
)"
  [[ -n "$DOWNLOAD_URL" ]] || die "Could not resolve Fabric API download URL"
  log "Downloading $FABRIC_API_FILENAME"
  curl -fL --retry 3 --retry-delay 2 -o "$FABRIC_API_CACHE.part" "$DOWNLOAD_URL"
  mv "$FABRIC_API_CACHE.part" "$FABRIC_API_CACHE"
else
  log "Fabric API cached: $FABRIC_API_CACHE"
fi

# ---- 2b. Vanilla MC + Fabric profile (cached) -----------------------------
# Cache the vanilla version JSON and client jar from Mojang, plus the Fabric
# loader profile JSON from Fabric meta. PojavLauncher fetches the rest
# (libraries, assets) on first launch when these files are present.
mkdir -p "$VANILLA_DIR_CACHE" "$FABRIC_DIR_CACHE"

VANILLA_JSON="$VANILLA_DIR_CACHE/${MC_VERSION}.json"
VANILLA_JAR="$VANILLA_DIR_CACHE/${MC_VERSION}.jar"
FABRIC_JSON="$FABRIC_DIR_CACHE/${FABRIC_PROFILE_ID}.json"

if [[ ! -f "$VANILLA_JSON" || ! -f "$VANILLA_JAR" ]]; then
  log "Resolving vanilla Minecraft ${MC_VERSION} via Mojang manifest"
  read -r MC_JSON_URL MC_JAR_URL <<< "$(python3 - <<PY
import json, sys, urllib.request
mc = "$MC_VERSION"
manifest = json.load(urllib.request.urlopen(
    "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json", timeout=30))
entry = next((v for v in manifest["versions"] if v["id"] == mc), None)
if entry is None:
    sys.exit("Mojang manifest has no entry for " + mc)
ver = json.load(urllib.request.urlopen(entry["url"], timeout=30))
client = ver.get("downloads", {}).get("client")
if not client:
    sys.exit("Mojang version JSON has no client download for " + mc)
print(entry["url"], client["url"])
PY
)"
  [[ -n "$MC_JSON_URL" && -n "$MC_JAR_URL" ]] \
    || die "Could not resolve Mojang URLs for $MC_VERSION"

  log "Downloading vanilla ${MC_VERSION}.json"
  curl -fL --retry 3 --retry-delay 2 -o "$VANILLA_JSON.part" "$MC_JSON_URL"
  mv "$VANILLA_JSON.part" "$VANILLA_JSON"
  log "Downloading vanilla ${MC_VERSION}.jar (~25MB)"
  curl -fL --retry 3 --retry-delay 2 -o "$VANILLA_JAR.part" "$MC_JAR_URL"
  mv "$VANILLA_JAR.part" "$VANILLA_JAR"
else
  log "Vanilla ${MC_VERSION} cached"
fi

if [[ ! -f "$FABRIC_JSON" ]]; then
  log "Downloading Fabric profile ${FABRIC_PROFILE_ID}"
  FABRIC_PROFILE_URL="https://meta.fabricmc.net/v2/versions/loader/${MC_VERSION}/${LOADER_VERSION}/profile/json"
  curl -fL --retry 3 --retry-delay 2 -o "$FABRIC_JSON.part" "$FABRIC_PROFILE_URL" \
    || die "Fabric meta has no loader ${LOADER_VERSION} for MC ${MC_VERSION}"
  mv "$FABRIC_JSON.part" "$FABRIC_JSON"
else
  log "Fabric profile cached: $FABRIC_PROFILE_ID"
fi

# ---- 2c. JRE 21 — extracted from PojavLauncher APK on first run -----------
# MC 1.21.x requires Java 21. PojavLauncher's bundled "Internal" runtime is
# Java 8, but the APK ships JRE 21 binaries under assets/components/jre-21/.
# We pull the APK once, extract the universal + arm64 tarballs, and re-pack
# them as a single uncompressed .tar so the device-side toybox can extract
# without xz support.
JRE_NAME="JRE-21"
JRE_CACHE_DIR="$CACHE_DIR/runtime"
JRE_TAR_LOCAL="$JRE_CACHE_DIR/$JRE_NAME.tar"
mkdir -p "$JRE_CACHE_DIR"

# ---- 3. Device check ------------------------------------------------------
log "Checking adb device"
DEVICE_LINE="$("${ADB[@]}" devices | awk 'NR>1 && $2=="device"' | head -1 || true)"
[[ -n "$DEVICE_LINE" ]] \
  || die "No authorized adb device. Plug tablet in, enable USB debugging, allow the prompt."

# ---- 4. Resolve mods dir on device ---------------------------------------
adb_dir_exists() {
  "${ADB[@]}" shell "[ -d '$1' ] && echo yes" 2>/dev/null | tr -d '\r' | grep -q yes
}

# Build the list of candidate .minecraft roots: legacy paths + every installed
# PojavLauncher-family package's scoped-storage path. The "real" .minecraft is
# the one that contains versions/ (the launcher writes that on first run).
discover_minecraft_dirs() {
  local pkg path
  for path in "${LEGACY_POJAV_MINECRAFT[@]}"; do
    printf '%s\n' "$path"
  done
  while IFS= read -r pkg; do
    [[ -z "$pkg" ]] && continue
    printf '/sdcard/Android/data/%s/files/.minecraft\n' "$pkg"
  done < <("${ADB[@]}" shell "pm list packages 2>/dev/null" \
           | tr -d '\r' \
           | sed -n 's/^package://p' \
           | grep -iE "$POJAV_PACKAGE_PATTERN" || true)
}

MINECRAFT_DIR=""
if [[ -n "$MODS_DIR_OVERRIDE" ]]; then
  if ! adb_dir_exists "$MODS_DIR_OVERRIDE"; then
    log "Override path $MODS_DIR_OVERRIDE doesn't exist — creating it"
    "${ADB[@]}" shell "mkdir -p '$MODS_DIR_OVERRIDE'" \
      || die "Cannot create $MODS_DIR_OVERRIDE"
  fi
  MODS_DIR="$MODS_DIR_OVERRIDE"
else
  # Prefer a .minecraft that has versions/ (proves it's the live install).
  while IFS= read -r path; do
    [[ -z "$path" ]] && continue
    if adb_dir_exists "$path/versions"; then
      MINECRAFT_DIR="$path"
      log "Live PojavLauncher install: $path (has versions/)"
      break
    fi
  done < <(discover_minecraft_dirs)

  # Fallback: any existing .minecraft, even if empty.
  if [[ -z "$MINECRAFT_DIR" ]]; then
    while IFS= read -r path; do
      [[ -z "$path" ]] && continue
      if adb_dir_exists "$path"; then
        MINECRAFT_DIR="$path"
        warn "No .minecraft has versions/; using $path as fallback. Open PojavLauncher once to materialise the install, then re-run."
        break
      fi
    done < <(discover_minecraft_dirs)
  fi

  [[ -n "$MINECRAFT_DIR" ]] \
    || die "Could not locate PojavLauncher .minecraft on device. Pass --mods-dir <abs path>."

  MODS_DIR="$MINECRAFT_DIR/mods"
  if ! adb_dir_exists "$MODS_DIR"; then
    "${ADB[@]}" shell "mkdir -p '$MODS_DIR'" || die "Cannot create $MODS_DIR"
  fi
fi

log "Target mods dir: $MODS_DIR"

# Parse the PojavLauncher package out of MINECRAFT_DIR so we can target
# scoped storage and run-as for runtimes / shared_prefs. Empty for legacy
# (/sdcard/games/PojavLauncher/...) installs and for --mods-dir overrides.
POJAV_PKG=""
if [[ -n "$MINECRAFT_DIR" ]]; then
  POJAV_PKG="$(printf '%s\n' "$MINECRAFT_DIR" \
    | sed -nE 's|^/sdcard/Android/data/([^/]+)/files/\.minecraft$|\1|p')"
fi

adb_runas_dir_exists() {
  "${ADB[@]}" shell "run-as $POJAV_PKG sh -c '[ -d \"$1\" ] && echo yes'" 2>/dev/null \
    | tr -d '\r' | grep -q yes
}

# ---- 4b. Install Fabric profile on device ---------------------------------
NEW_PROFILE_UUID=""
if [[ -z "$MODS_DIR_OVERRIDE" && -n "$MINECRAFT_DIR" ]]; then
  log "Installing vanilla ${MC_VERSION} into device versions/"
  "${ADB[@]}" shell "mkdir -p '$MINECRAFT_DIR/versions/${MC_VERSION}'"
  "${ADB[@]}" push "$VANILLA_JSON" \
    "$MINECRAFT_DIR/versions/${MC_VERSION}/${MC_VERSION}.json" >/dev/null
  # Don't push the vanilla client jar — Pojav fetches it itself on first
  # launch using the URL in the JSON. A pushed jar in the parent dir
  # combined with our jar-less Fabric child profile makes Pojav's UI
  # silently drop the Fabric entry from the version dropdown (observed
  # against the gladiolus debug build).

  log "Installing Fabric profile ${FABRIC_PROFILE_ID}"
  "${ADB[@]}" shell "mkdir -p '$MINECRAFT_DIR/versions/${FABRIC_PROFILE_ID}'"
  "${ADB[@]}" push "$FABRIC_JSON" \
    "$MINECRAFT_DIR/versions/${FABRIC_PROFILE_ID}/${FABRIC_PROFILE_ID}.json" >/dev/null
  # Same defensive cleanup: remove any stray jar in either version folder
  # left behind by an earlier run that pushed it.
  "${ADB[@]}" shell "rm -f \
    '$MINECRAFT_DIR/versions/${MC_VERSION}/${MC_VERSION}.jar' \
    '$MINECRAFT_DIR/versions/${FABRIC_PROFILE_ID}/${FABRIC_PROFILE_ID}.jar'" || true

  log "Merging launcher_profiles.json (adding Fabric profile)"
  PROFILES_LOCAL="$CACHE_DIR/launcher_profiles.${MC_VERSION}.json"
  "${ADB[@]}" pull "$MINECRAFT_DIR/launcher_profiles.json" "$PROFILES_LOCAL" >/dev/null 2>&1 \
    || printf '{"profiles":{}}\n' > "$PROFILES_LOCAL"

  # PojavLauncher-friendly minimal profile shape: icon / lastVersionId /
  # logConfigIsXML / name. Earlier runs of this script wrote an extended shape
  # ({type, created, lastUsed, ...}) that PojavLauncher's UI did not surface in
  # the version dropdown — we now coerce any matching entry back to the
  # minimal form.
  # Match the exact field set Pojav writes for its built-in Fabric profile
  # (icon / lastVersionId / logConfigIsXML / name) — extra fields trigger
  # silent rejection and the profile never appears in the dropdown.
  # selectedProfile at the root makes the launcher pre-select our profile.
  PYOUT="$(python3 - "$PROFILES_LOCAL" "$FABRIC_PROFILE_ID" <<'PY'
import json, sys, uuid
path, version_id = sys.argv[1], sys.argv[2]
with open(path) as f:
    data = json.load(f)
profiles = data.setdefault("profiles", {})
pid = next((p for p, prof in profiles.items()
            if prof.get("lastVersionId") == version_id), None)
if pid is None:
    pid = str(uuid.uuid4())
profiles[pid] = {
    "icon": "fabric",
    "lastVersionId": version_id,
    "logConfigIsXML": False,
    "name": "Fabric " + version_id.replace("fabric-loader-", ""),
}
data["profiles"] = profiles
data["selectedProfile"] = pid
with open(path, "w") as f:
    json.dump(data, f, indent=2)
print("PROFILE_UUID=" + pid)
PY
)"
  NEW_PROFILE_UUID="$(printf '%s\n' "$PYOUT" | sed -n 's/^PROFILE_UUID=//p')"
  [[ -n "$NEW_PROFILE_UUID" ]] || die "Failed to allocate profile UUID"

  "${ADB[@]}" push "$PROFILES_LOCAL" "$MINECRAFT_DIR/launcher_profiles.json" >/dev/null
  ok "Fabric profile present: $NEW_PROFILE_UUID -> $FABRIC_PROFILE_ID"
fi

# ---- 4c. Install JRE 21 from PojavLauncher APK ----------------------------
# MC 1.21.x needs Java 21. Pojav's "Internal" runtime is Java 8. The APK ships
# a JRE 21 universal+arm64 tarball pair under assets/components/jre-21/. We
# extract once locally, repack as plain .tar (toybox tar has no xz support),
# and push under runtimes/JRE-21/ via run-as.
if [[ -n "$POJAV_PKG" ]]; then
  if [[ ! -f "$JRE_TAR_LOCAL" ]]; then
    log "Pulling PojavLauncher APK to extract JRE-21 (~150MB, first run only)"
    APK_DEVICE_PATH="$("${ADB[@]}" shell "pm path $POJAV_PKG" \
                      | tr -d '\r' | sed -n 's/^package://p' | head -1)"
    [[ -n "$APK_DEVICE_PATH" ]] || die "Cannot resolve APK path for $POJAV_PKG"
    APK_LOCAL="$JRE_CACHE_DIR/pojav.apk"
    "${ADB[@]}" pull "$APK_DEVICE_PATH" "$APK_LOCAL" >/dev/null

    log "Extracting JRE-21 from APK"
    EXTRACT_TMP="$JRE_CACHE_DIR/jre21-extract"
    rm -rf "$EXTRACT_TMP"
    mkdir -p "$EXTRACT_TMP/staged"
    unzip -j -q "$APK_LOCAL" \
      "assets/components/jre-21/bin-arm64.tar.xz" \
      "assets/components/jre-21/universal.tar.xz" \
      -d "$EXTRACT_TMP/" \
      || die "APK has no assets/components/jre-21/ — unsupported PojavLauncher build?"
    tar -xJf "$EXTRACT_TMP/universal.tar.xz" -C "$EXTRACT_TMP/staged/"
    tar -xJf "$EXTRACT_TMP/bin-arm64.tar.xz" -C "$EXTRACT_TMP/staged/"
    # COPYFILE_DISABLE=1 keeps macOS BSD tar from inserting AppleDouble
    # ./._* metadata entries, which the device toybox tar would otherwise
    # extract as unreadable junk.
    COPYFILE_DISABLE=1 tar --no-xattrs -cf "$JRE_TAR_LOCAL.part" \
      -C "$EXTRACT_TMP/staged" .
    mv "$JRE_TAR_LOCAL.part" "$JRE_TAR_LOCAL"
    rm -rf "$EXTRACT_TMP" "$APK_LOCAL"
    ok "JRE-21 cached: $JRE_TAR_LOCAL"
  else
    log "JRE-21 cached: $JRE_TAR_LOCAL"
  fi

  if adb_runas_dir_exists "runtimes/$JRE_NAME" \
     && "${ADB[@]}" shell "run-as $POJAV_PKG sh -c '[ -f runtimes/$JRE_NAME/bin/java ] && echo ok'" 2>/dev/null \
        | tr -d '\r' | grep -q ok; then
    log "$JRE_NAME already installed on device"
  else
    log "Installing $JRE_NAME on device (~26MB)"
    # Stage under /data/local/tmp because /sdcard/Android/data/<pkg>/ files
    # land with SELinux context media_rw_data_file, which the runas_app
    # domain cannot read. /data/local/tmp is shell-writable and run-as can
    # read it.
    JRE_STAGE_REMOTE="/data/local/tmp/super-tnt-jre21.tar"
    "${ADB[@]}" push "$JRE_TAR_LOCAL" "$JRE_STAGE_REMOTE" >/dev/null
    "${ADB[@]}" shell "chmod 0644 $JRE_STAGE_REMOTE" || true
    "${ADB[@]}" shell "run-as $POJAV_PKG sh -c '
      mkdir -p runtimes/$JRE_NAME &&
      cd runtimes/$JRE_NAME &&
      tar -xf $JRE_STAGE_REMOTE &&
      chmod 755 bin/* 2>/dev/null
    '" || die "Failed to extract JRE-21 inside app sandbox"
    "${ADB[@]}" shell "rm -f $JRE_STAGE_REMOTE" || true
    ok "$JRE_NAME installed at /data/data/$POJAV_PKG/runtimes/$JRE_NAME"
  fi
fi

# ---- 4d. Repair shared_prefs.defaultRuntime if a previous run set it ------
# An earlier version of this script wrote defaultRuntime=JRE-21 globally,
# which crashed every Java 8 profile (e.g. 1.7.10) with "Error occurred
# during initialization of VM". We now leave the launcher's own runtime
# choice alone — the user picks the runtime per session via Settings → Java
# inside PojavLauncher. If the prior bad value is still on disk, undo it.
if [[ -n "$POJAV_PKG" ]]; then
  PREF_FILE="${POJAV_PKG}_preferences.xml"
  PREF_LOCAL="$CACHE_DIR/$PREF_FILE"
  if "${ADB[@]}" exec-out "run-as $POJAV_PKG cat shared_prefs/$PREF_FILE" \
       > "$PREF_LOCAL" 2>/dev/null && [[ -s "$PREF_LOCAL" ]]; then
    if grep -q "<string name=\"defaultRuntime\">$JRE_NAME</string>" "$PREF_LOCAL"; then
      log "Reverting shared_prefs.defaultRuntime to Internal (previous run set it to $JRE_NAME)"
      "${ADB[@]}" shell "am force-stop $POJAV_PKG" >/dev/null || true
      python3 - "$PREF_LOCAL" <<'PY'
import sys, xml.etree.ElementTree as ET
path = sys.argv[1]
tree = ET.parse(path)
for child in tree.getroot():
    if child.attrib.get("name") == "defaultRuntime" and child.tag == "string":
        child.text = "Internal"
tree.write(path, xml_declaration=True, encoding="utf-8")
PY
      "${ADB[@]}" shell "run-as $POJAV_PKG sh -c 'cat > shared_prefs/$PREF_FILE'" \
        < "$PREF_LOCAL" || die "Cannot restore shared_prefs/$PREF_FILE"
      ok "Reverted defaultRuntime=Internal — Java 8 profiles work again"
    fi
  fi
fi

# ---- 5. Push --------------------------------------------------------------
# Clean stale jars from any non-target .minecraft we know about (e.g. an empty
# legacy path the user accidentally pushed to before).
if [[ -z "$MODS_DIR_OVERRIDE" ]]; then
  while IFS= read -r path; do
    [[ -z "$path" ]] && continue
    [[ "$path/mods" == "$MODS_DIR" ]] && continue
    if adb_dir_exists "$path/mods"; then
      # `ls` on a non-matching glob returns non-zero, which trips
      # set -o pipefail in command substitution; swallow that.
      stray="$("${ADB[@]}" shell \
        "ls '$path/mods/${ARCHIVE_BASE}-'*.jar '$path/mods/fabric-api-'*.jar 2>/dev/null" \
        | tr -d '\r' || true)"
      if [[ -n "$stray" ]]; then
        warn "Removing stray jars from $path/mods (not the live install)"
        "${ADB[@]}" shell \
          "rm -f '$path/mods/${ARCHIVE_BASE}-'*.jar '$path/mods/fabric-api-'*.jar" || true
      fi
    fi
  done < <(discover_minecraft_dirs)
fi

log "Removing previous super-tnt-mod / fabric-api jars on device"
"${ADB[@]}" shell "rm -f '$MODS_DIR/${ARCHIVE_BASE}-'*.jar '$MODS_DIR/fabric-api-'*.jar" || true

log "Pushing mod jar"
"${ADB[@]}" push "$MOD_JAR" "$MODS_DIR/" >/dev/null

log "Pushing Fabric API"
"${ADB[@]}" push "$FABRIC_API_CACHE" "$MODS_DIR/" >/dev/null

# ---- 6. Verify ------------------------------------------------------------
log "Files now in $MODS_DIR:"
"${ADB[@]}" shell "ls -la '$MODS_DIR'" | sed 's/^/    /'

log "Done. Launch PojavLauncher with a Fabric ${MC_VERSION} profile."
