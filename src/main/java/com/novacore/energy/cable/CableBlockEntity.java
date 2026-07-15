package com.novacore.energy.cable;

import com.novacore.NovaCore;
import com.novacore.energy.network.EnergyNetworkManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

public class CableBlockEntity extends BlockEntity {

    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(NovaCore.CABLE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide()) return;
        joinNetwork();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        detachFromNetwork();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        detachFromNetwork();
    }

    private void detachFromNetwork() {
        if (level == null) return;
        CableNetworks.get(level).removeNode(worldPosition);
    }

    /** Called by {@link CableBlock#neighborChanged} to re-scan for capabilities on adjacent blocks. */
    public void refreshNeighbors() {
        if (level == null || level.isClientSide()) return;
        joinNetwork();
    }

    /**
     * Registers this cable in the level's network if it isn't already, then rescans its 6 sides.
     * Normally {@link #onLoad()} alone would register the node, but a neighbor placed in the same
     * tick can trigger {@link #refreshNeighbors()} before this cable's own onLoad has run (seen in
     * GameTests, where several blocks are placed back to back with no tick boundary between them),
     * so joining defensively here avoids depending on that ordering.
     */
    private void joinNetwork() {
        EnergyNetworkManager<BlockPos, CableEndpointKey> manager = CableNetworks.get(level);
        if (manager.networkOf(worldPosition) == null) {
            manager.addNode(worldPosition);
        }
        refreshEndpoints(manager);
    }

    private void refreshEndpoints(EnergyNetworkManager<BlockPos, CableEndpointKey> manager) {
        for (Direction direction : Direction.values()) {
            CableEndpointKey key = new CableEndpointKey(worldPosition, direction);
            manager.detach(key);

            BlockPos neighborPos = worldPosition.relative(direction);
            if (level.getBlockEntity(neighborPos) instanceof CableBlockEntity) continue;

            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, neighborPos, direction.getOpposite());
            if (handler == null) continue;

            EnergyHandlerAdapter adapter = new EnergyHandlerAdapter(handler);
            manager.attachProvider(worldPosition, key, adapter);
            manager.attachConsumer(worldPosition, key, adapter);
        }
    }
}
