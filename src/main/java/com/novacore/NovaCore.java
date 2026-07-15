package com.novacore;

import java.util.Set;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.novacore.energy.battery.BatteryBlock;
import com.novacore.energy.battery.BatteryBlockEntity;
import com.novacore.energy.battery.BatteryTier;
import com.novacore.energy.cable.CableBlock;
import com.novacore.energy.cable.CableBlockEntity;
import com.novacore.energy.cable.CableNetworks;
import com.novacore.energy.furnace.ElectricFurnaceBlock;
import com.novacore.energy.furnace.ElectricFurnaceBlockEntity;
import com.novacore.energy.furnace.ElectricFurnaceMenu;
import com.novacore.energy.generator.ThermalGeneratorBlock;
import com.novacore.energy.generator.ThermalGeneratorBlockEntity;
import com.novacore.energy.generator.ThermalGeneratorMenu;
import com.novacore.energy.solar.SolarPanelBlock;
import com.novacore.energy.solar.SolarPanelBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(NovaCore.MODID)
public class NovaCore {
    public static final String MODID = "novacore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredBlock<CableBlock> CABLE = BLOCKS.registerBlock("cable", CableBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> CABLE_ITEM = ITEMS.registerSimpleBlockItem("cable", CABLE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>> CABLE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("cable", () -> new BlockEntityType<>(CableBlockEntity::new, Set.of(CABLE.get())));

    public static final DeferredBlock<BatteryBlock> BATTERY_BASIC = BLOCKS.registerBlock("battery_basic",
            p -> new BatteryBlock(p, BatteryTier.BASIC), p -> p.mapColor(MapColor.COLOR_YELLOW).strength(2.0F).sound(SoundType.METAL));
    public static final DeferredBlock<BatteryBlock> BATTERY_ADVANCED = BLOCKS.registerBlock("battery_advanced",
            p -> new BatteryBlock(p, BatteryTier.ADVANCED), p -> p.mapColor(MapColor.COLOR_ORANGE).strength(2.0F).sound(SoundType.METAL));
    public static final DeferredBlock<BatteryBlock> BATTERY_SUPREME = BLOCKS.registerBlock("battery_supreme",
            p -> new BatteryBlock(p, BatteryTier.SUPREME), p -> p.mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> BATTERY_BASIC_ITEM = ITEMS.registerSimpleBlockItem("battery_basic", BATTERY_BASIC);
    public static final DeferredItem<BlockItem> BATTERY_ADVANCED_ITEM = ITEMS.registerSimpleBlockItem("battery_advanced", BATTERY_ADVANCED);
    public static final DeferredItem<BlockItem> BATTERY_SUPREME_ITEM = ITEMS.registerSimpleBlockItem("battery_supreme", BATTERY_SUPREME);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBlockEntity>> BATTERY_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("battery", () -> new BlockEntityType<>(BatteryBlockEntity::new,
                    Set.of(BATTERY_BASIC.get(), BATTERY_ADVANCED.get(), BATTERY_SUPREME.get())));

    public static final DeferredBlock<ThermalGeneratorBlock> THERMAL_GENERATOR = BLOCKS.registerBlock("thermal_generator",
            ThermalGeneratorBlock::new, p -> p.mapColor(MapColor.STONE).strength(3.5F).sound(SoundType.STONE));
    public static final DeferredItem<BlockItem> THERMAL_GENERATOR_ITEM = ITEMS.registerSimpleBlockItem("thermal_generator", THERMAL_GENERATOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermalGeneratorBlockEntity>> THERMAL_GENERATOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("thermal_generator", () -> new BlockEntityType<>(ThermalGeneratorBlockEntity::new, Set.of(THERMAL_GENERATOR.get())));
    public static final DeferredHolder<MenuType<?>, MenuType<ThermalGeneratorMenu>> THERMAL_GENERATOR_MENU =
            MENU_TYPES.register("thermal_generator", () -> new MenuType<>(ThermalGeneratorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredBlock<SolarPanelBlock> SOLAR_PANEL = BLOCKS.registerBlock("solar_panel",
            SolarPanelBlock::new, p -> p.mapColor(MapColor.COLOR_BLUE).strength(1.5F).sound(SoundType.GLASS));
    public static final DeferredItem<BlockItem> SOLAR_PANEL_ITEM = ITEMS.registerSimpleBlockItem("solar_panel", SOLAR_PANEL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("solar_panel", () -> new BlockEntityType<>(SolarPanelBlockEntity::new, Set.of(SOLAR_PANEL.get())));

    public static final DeferredBlock<ElectricFurnaceBlock> ELECTRIC_FURNACE = BLOCKS.registerBlock("electric_furnace",
            ElectricFurnaceBlock::new, p -> p.mapColor(MapColor.STONE).strength(3.5F).sound(SoundType.STONE));
    public static final DeferredItem<BlockItem> ELECTRIC_FURNACE_ITEM = ITEMS.registerSimpleBlockItem("electric_furnace", ELECTRIC_FURNACE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("electric_furnace", () -> new BlockEntityType<>(ElectricFurnaceBlockEntity::new, Set.of(ELECTRIC_FURNACE.get())));
    public static final DeferredHolder<MenuType<?>, MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE_MENU =
            MENU_TYPES.register("electric_furnace", () -> new MenuType<>(ElectricFurnaceMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.novacore"))
            .icon(() -> CABLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(CABLE_ITEM.get());
                output.accept(BATTERY_BASIC_ITEM.get());
                output.accept(BATTERY_ADVANCED_ITEM.get());
                output.accept(BATTERY_SUPREME_ITEM.get());
                output.accept(THERMAL_GENERATOR_ITEM.get());
                output.accept(SOLAR_PANEL_ITEM.get());
                output.accept(ELECTRIC_FURNACE_ITEM.get());
            }).build());

    public NovaCore(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENU_TYPES.register(modEventBus);

        modEventBus.addListener(this::registerCapabilities);

        NeoForge.EVENT_BUS.addListener(CableNetworks::onLevelTick);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, BATTERY_BLOCK_ENTITY.get(),
                (battery, direction) -> battery.energyHandler());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, THERMAL_GENERATOR_BLOCK_ENTITY.get(),
                (generator, direction) -> generator.energyHandler());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, SOLAR_PANEL_BLOCK_ENTITY.get(),
                (panel, direction) -> panel.energyHandler());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ELECTRIC_FURNACE_BLOCK_ENTITY.get(),
                (furnace, direction) -> furnace.energyHandler());
    }
}
