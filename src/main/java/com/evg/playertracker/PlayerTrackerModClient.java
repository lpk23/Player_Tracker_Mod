package com.evg.playertracker;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = PlayerTrackerMod.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = PlayerTrackerMod.MODID, value = Dist.CLIENT)
public class PlayerTrackerModClient {
    public PlayerTrackerModClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Client setup code
        PlayerTrackerMod.LOGGER.info("Player Tracker Mod - Client Setup Complete");
        PlayerTrackerMod.LOGGER.info("Player name: {}", Minecraft.getInstance().getUser().getName());
        PlayerTrackerMod.LOGGER.info("HUD mode: {}", Config.HUD_MODE.get());
        PlayerTrackerMod.LOGGER.info("Sort mode: {}", Config.SORT_MODE.get());
        
        // Initialize client-side components
        initializeClientComponents();
    }
    
    private static void initializeClientComponents() {
        PlayerTrackerMod.LOGGER.info("Client components initialized");
        PlayerTrackerMod.LOGGER.info("Мод работает полностью на клиенте");
        PlayerTrackerMod.LOGGER.info("Обнаружение игроков происходит автоматически");
        PlayerTrackerMod.LOGGER.info("Используйте HUD для просмотра информации");
        PlayerTrackerMod.LOGGER.info("Для тестирования HUD вызовите PlayerTrackerHUD.renderHUD()");
    }
}
