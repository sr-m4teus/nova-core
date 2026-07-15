package com.novacore.gametest;

import com.novacore.NovaCore;
import com.novacore.energy.battery.BatteryBlockEntity;
import com.novacore.energy.furnace.ElectricFurnaceBlockEntity;
import com.novacore.energy.generator.ThermalGeneratorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Proves the energy pillar's full cycle end to end, per the spec's testing section: a Thermal
 * Generator burning coal pushes power through a cable to an Electric Furnace, which finishes a
 * smelt using only delivered energy -- generation, the cached network graph, and consumption all
 * exercised. A Basic Battery is then connected to the same cable and confirmed to start charging,
 * proving storage too.
 *
 * <p>The battery joins only after the furnace assertion, not alongside it: an empty battery's
 * demand (up to 100,000 FE) dwarfs the furnace's (up to 200 FE), so under proportional-by-demand
 * distribution a battery competing from tick 0 would take most of the furnace's fair share every
 * round -- correct behavior for the network (see {@link com.novacore.energy.network.EnergyNetwork}),
 * not something to route around by having the test race it.
 */
public final class EnergyFullCycleGameTests {

    public static void energyFullCycle(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 1);
        BlockPos cablePos = new BlockPos(2, 1, 1);
        BlockPos furnacePos = new BlockPos(2, 1, 2);
        BlockPos batteryPos = new BlockPos(2, 2, 1);

        helper.setBlock(generatorPos, NovaCore.THERMAL_GENERATOR.get());
        helper.setBlock(cablePos, NovaCore.CABLE.get());
        helper.setBlock(furnacePos, NovaCore.ELECTRIC_FURNACE.get());

        ThermalGeneratorBlockEntity generator = helper.getBlockEntity(generatorPos, ThermalGeneratorBlockEntity.class);
        generator.setItem(0, new ItemStack(Items.COAL));

        ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos, ElectricFurnaceBlockEntity.class);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));

        helper.startSequence()
                .thenExecuteAfter(220, () -> {
                    ElectricFurnaceBlockEntity resultFurnace = helper.getBlockEntity(furnacePos, ElectricFurnaceBlockEntity.class);
                    helper.assertTrue(resultFurnace.getItem(1).is(Items.IRON_INGOT),
                            "furnace should have finished smelting the raw iron using energy delivered through the cable");
                })
                .thenExecute(() -> helper.setBlock(batteryPos, NovaCore.BATTERY_BASIC.get()))
                .thenExecuteAfter(30, () -> {
                    BatteryBlockEntity battery = helper.getBlockEntity(batteryPos, BatteryBlockEntity.class);
                    helper.assertTrue(battery.energyHandler().getAmountAsLong() > 0,
                            "battery should have received charge from the generator through the cable once connected");
                })
                .thenSucceed();
    }

    private EnergyFullCycleGameTests() {}
}
