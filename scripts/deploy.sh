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
FABRIC_API_RAW="$(prop fabric_version)"     # e.g. "0.139.4+1.21.11"
MOD_VERSION="$(prop mod_version)"
ARCHIVE_BASE="$(prop archives_base_name)"

[[ -n "$MC_VERSION" && -n "$FABRIC_API_RAW" ]] \
  || die "Could not read versions from gradle.properties"

FABRIC_API_FILENAME="fabric-api-${FABRIC_API_RAW}.jar"
FABRIC_API_CACHE="$CACHE_DIR/$FABRIC_API_FILENAME"

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

# ---- 5. Push --------------------------------------------------------------
# Clean stale jars from any non-target .minecraft we know about (e.g. an empty
# legacy path the user accidentally pushed to before).
if [[ -z "$MODS_DIR_OVERRIDE" ]]; then
  while IFS= read -r path; do
    [[ -z "$path" ]] && continue
    [[ "$path/mods" == "$MODS_DIR" ]] && continue
    if adb_dir_exists "$path/mods"; then
      stray="$("${ADB[@]}" shell \
        "ls '$path/mods/${ARCHIVE_BASE}-'*.jar '$path/mods/fabric-api-'*.jar 2>/dev/null" \
        | tr -d '\r')"
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
