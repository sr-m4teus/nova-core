package com.novacore;

import com.novacore.energy.furnace.ElectricFurnaceScreen;
import com.novacore.energy.generator.ThermalGeneratorScreen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = NovaCore.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = NovaCore.MODID, value = Dist.CLIENT)
public class NovaCoreClient {

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NovaCore.THERMAL_GENERATOR_MENU.get(), ThermalGeneratorScreen::new);
        event.register(NovaCore.ELECTRIC_FURNACE_MENU.get(), ElectricFurnaceScreen::new);
    }
}
