package com.novacore;

import java.util.Set;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.novacore.energy.cable.CableBlock;
import com.novacore.energy.cable.CableBlockEntity;
import com.novacore.energy.cable.CableNetworks;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
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

    public static final DeferredBlock<CableBlock> CABLE = BLOCKS.registerBlock("cable", CableBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> CABLE_ITEM = ITEMS.registerSimpleBlockItem("cable", CABLE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>> CABLE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("cable", () -> new BlockEntityType<>(CableBlockEntity::new, Set.of(CABLE.get())));

    public NovaCore(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(CableNetworks::onLevelTick);
    }
}
