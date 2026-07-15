package com.novacore.energy.network;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnergyNetworkManagerTest {

    /** Adjacency for a 1-D line of cables: node N is adjacent to N-1 and N+1. */
    private static EnergyNetworkManager<Integer> lineManager() {
        return new EnergyNetworkManager<>(node -> List.of(node - 1, node + 1));
    }

    @Test
    void addingAdjacentNodesMergesIntoOneNetwork() {
        var manager = lineManager();
        manager.addNode(1);
        manager.addNode(2);
        manager.addNode(3);

        assertSame(manager.networkOf(1), manager.networkOf(2));
        assertSame(manager.networkOf(2), manager.networkOf(3));
        assertEquals(Set.of(1, 2, 3), manager.networkOf(1).nodes());
    }

    @Test
    void addingIsolatedNodeCreatesSeparateNetwork() {
        var manager = lineManager();
        manager.addNode(1);
        manager.addNode(10);

        assertNotSame(manager.networkOf(1), manager.networkOf(10));
    }

    @Test
    void addingBridgeNodeMergesTwoExistingNetworks() {
        var manager = lineManager();
        manager.addNode(1);
        manager.addNode(3);
        assertNotSame(manager.networkOf(1), manager.networkOf(3));

        manager.addNode(2);

        assertSame(manager.networkOf(1), manager.networkOf(2));
        assertSame(manager.networkOf(2), manager.networkOf(3));
    }

    @Test
    void removingMiddleNodeSplitsNetworkInTwo() {
        var manager = lineManager();
        manager.addNode(1);
        manager.addNode(2);
        manager.addNode(3);

        manager.removeNode(2);

        assertNull(manager.networkOf(2));
        assertNotSame(manager.networkOf(1), manager.networkOf(3));
        assertEquals(Set.of(1), manager.networkOf(1).nodes());
        assertEquals(Set.of(3), manager.networkOf(3).nodes());
    }

    @Test
    void removingEndNodeDoesNotSplitNetwork() {
        var manager = lineManager();
        manager.addNode(1);
        manager.addNode(2);
        manager.addNode(3);

        manager.removeNode(1);

        assertNull(manager.networkOf(1));
        assertSame(manager.networkOf(2), manager.networkOf(3));
        assertEquals(Set.of(2, 3), manager.networkOf(2).nodes());
    }

    @Test
    void removingLastNodeOfNetworkLeavesNoNetworkBehind() {
        var manager = lineManager();
        manager.addNode(1);
        manager.removeNode(1);

        assertNull(manager.networkOf(1));
    }

    @Test
    void splitReassignsProvidersAndConsumersToTheirOwnComponent() {
        var manager = lineManager();
        manager.addNode(1);
        manager.addNode(2);
        manager.addNode(3);

        FakeProvider provider = new FakeProvider(100);
        FakeConsumer consumer = new FakeConsumer(50);
        manager.attachProvider(1, provider);
        manager.attachConsumer(3, consumer);

        manager.removeNode(2);

        var networkWithProvider = manager.networkOf(1);
        var networkWithConsumer = manager.networkOf(3);
        assertNotSame(networkWithProvider, networkWithConsumer);

        // Provider's network has no consumer to deliver to -> tick is a no-op.
        assertEquals(0, networkWithProvider.tick().totalTransferred());
        // Consumer's network has no provider attached -> tick reports demand but no transfer.
        var result = networkWithConsumer.tick();
        assertEquals(50, result.totalDemanded());
        assertEquals(0, result.totalTransferred());
    }

    @Test
    void attachingToUnknownNodeThrows() {
        var manager = lineManager();
        assertThrows(IllegalStateException.class, () -> manager.attachProvider(1, new FakeProvider(10)));
    }

    private static final class FakeProvider implements EnergyProvider {
        private long available;

        FakeProvider(long available) {
            this.available = available;
        }

        @Override
        public long getAvailable() {
            return available;
        }

        @Override
        public long extract(long amount) {
            long extracted = Math.min(amount, available);
            available -= extracted;
            return extracted;
        }
    }

    private static final class FakeConsumer implements EnergyConsumer {
        private long demand;
        private long received;

        FakeConsumer(long demand) {
            this.demand = demand;
        }

        @Override
        public long getDemand() {
            return demand;
        }

        @Override
        public long insert(long amount) {
            long accepted = Math.min(amount, demand);
            demand -= accepted;
            received += accepted;
            return accepted;
        }
    }
}
