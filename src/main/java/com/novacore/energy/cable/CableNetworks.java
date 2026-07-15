package com.novacore.energy.cable;

import com.novacore.energy.network.EnergyNetwork;
import com.novacore.energy.network.EnergyNetworkManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** One {@link EnergyNetworkManager} per level, and the driver that ticks every distinct network once per game tick. */
public final class CableNetworks {

    private static final Map<Level, EnergyNetworkManager<BlockPos, CableEndpointKey>> MANAGERS = new WeakHashMap<>();

    public static EnergyNetworkManager<BlockPos, CableEndpointKey> get(Level level) {
        return MANAGERS.computeIfAbsent(level, l -> new EnergyNetworkManager<>(CableNetworks::neighborsOf));
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        EnergyNetworkManager<BlockPos, CableEndpointKey> manager = MANAGERS.get(level);
        if (manager == null) return;

        for (EnergyNetwork<BlockPos, CableEndpointKey> network : manager.allNetworks()) {
            network.tick();
        }
    }

    private static List<BlockPos> neighborsOf(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            neighbors.add(pos.relative(direction));
        }
        return neighbors;
    }

    private CableNetworks() {}
}
