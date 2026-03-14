package com.supertntmod;

import com.supertntmod.block.ModBlocks;
import com.supertntmod.entity.ModEntities;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuperTntMod implements ModInitializer {
    public static final String MOD_ID = "supertntmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModEntities.register();
        ModBlocks.register();
        LOGGER.info("💥 Super TNT Modu yüklendi! 7 yeni TNT türü hazır.");
    }
}
