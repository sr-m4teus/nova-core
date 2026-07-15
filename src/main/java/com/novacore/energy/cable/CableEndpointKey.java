package com.novacore.energy.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Identifies the external capability a cable found on one of its 6 sides. */
record CableEndpointKey(BlockPos cablePos, Direction direction) {}
