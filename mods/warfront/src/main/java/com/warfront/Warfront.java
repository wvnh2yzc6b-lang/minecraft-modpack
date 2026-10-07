package com.warfront;

import com.mojang.logging.LogUtils;
import com.warfront.config.WFConfig;
import com.warfront.registry.WFRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(Warfront.MODID)
public class Warfront {
    public static final String MODID = "warfront";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Warfront(IEventBus modBus, ModContainer container) {
        WFRegistry.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, WFConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, com.warfront.config.WFClientConfig.SPEC);
        LOGGER.info("Warfront: raising the banners");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
