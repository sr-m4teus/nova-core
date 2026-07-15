package com.novacore.energy.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One connected cluster of cables ({@code K}) plus the providers/consumers ({@code E}) attached
 * to it. Endpoints are keyed separately from nodes because a single cable node can have several
 * external endpoints (one per side). No energy is lost in transport: {@link #tick()} distributes
 * exactly what providers report available, capped by what consumers report they demand.
 */
public final class EnergyNetwork<K, E> {

    public record DistributionResult(long totalSupplied, long totalDemanded, long totalTransferred) {
        static final DistributionResult EMPTY = new DistributionResult(0, 0, 0);
    }

    @FunctionalInterface
    private interface EnergyMover<E> {
        long move(E key, long amount);
    }

    private final Set<K> nodes = new LinkedHashSet<>();
    private final Map<E, EnergyProvider> providers = new LinkedHashMap<>();
    private final Map<E, EnergyConsumer> consumers = new LinkedHashMap<>();

    void addNode(K node) {
        nodes.add(node);
    }

    void removeNode(K node) {
        nodes.remove(node);
    }

    void absorb(EnergyNetwork<K, E> other) {
        nodes.addAll(other.nodes);
        providers.putAll(other.providers);
        consumers.putAll(other.consumers);
    }

    EnergyProvider takeProvider(E endpoint) {
        return providers.remove(endpoint);
    }

    EnergyConsumer takeConsumer(E endpoint) {
        return consumers.remove(endpoint);
    }

    public boolean contains(K node) {
        return nodes.contains(node);
    }

    public Set<K> nodes() {
        return Set.copyOf(nodes);
    }

    void attachProvider(E endpoint, EnergyProvider provider) {
        providers.put(endpoint, provider);
    }

    void attachConsumer(E endpoint, EnergyConsumer consumer) {
        consumers.put(endpoint, consumer);
    }

    void detachProvider(E endpoint) {
        providers.remove(endpoint);
    }

    void detachConsumer(E endpoint) {
        consumers.remove(endpoint);
    }

    public DistributionResult tick() {
        if (consumers.isEmpty()) {
            return DistributionResult.EMPTY;
        }

        Map<E, Long> demand = new LinkedHashMap<>();
        long totalDemand = 0;
        for (var entry : consumers.entrySet()) {
            long d = Math.max(0, entry.getValue().getDemand());
            demand.put(entry.getKey(), d);
            totalDemand += d;
        }
        if (totalDemand == 0) {
            return DistributionResult.EMPTY;
        }

        Map<E, Long> available = new LinkedHashMap<>();
        long totalAvailable = 0;
        for (var entry : providers.entrySet()) {
            long a = Math.max(0, entry.getValue().getAvailable());
            available.put(entry.getKey(), a);
            totalAvailable += a;
        }

        long transferable = Math.min(totalDemand, totalAvailable);
        if (transferable <= 0) {
            return new DistributionResult(0, totalDemand, 0);
        }

        // Every endpoint is registered as both a provider and a consumer (the cable can't know
        // ahead of time which role a handler actually supports), so getAvailable()/getDemand()
        // are only ever *reported* figures -- a node that rejects every real extract()/insert()
        // call (maxExtract=0 or maxInsert=0) still counts toward the totals above. A single
        // proportional split would let such a node's reported figure dominate the shares and
        // either starve the real parties of most of their fair amount (a phantom consumer with
        // huge demand) or leave real supply unclaimed (a phantom provider with huge stored
        // amount). waterFill runs the split in rounds instead: whatever a party doesn't actually
        // move goes back into the pool for the rest, and a party that moved nothing is dropped
        // from consideration, so a dead end can cost at most one round, not the whole transfer.
        long totalExtracted = waterFill(available, transferable, (key, amount) -> providers.get(key).extract(amount));
        long totalInserted = waterFill(demand, totalExtracted, (key, amount) -> consumers.get(key).insert(amount));

        return new DistributionResult(totalExtracted, totalDemand, totalInserted);
    }

    private long waterFill(Map<E, Long> weights, long amount, EnergyMover<E> mover) {
        Map<E, Long> remainingWeights = new LinkedHashMap<>(weights);
        long remainingAmount = amount;
        long totalMoved = 0;

        while (remainingAmount > 0 && !remainingWeights.isEmpty()) {
            Map<E, Long> shares = largestRemainderShares(remainingWeights, remainingAmount);
            long movedThisRound = 0;
            List<E> settled = new ArrayList<>();

            for (E key : remainingWeights.keySet()) {
                long share = shares.getOrDefault(key, 0L);
                if (share <= 0) continue;

                long moved = mover.move(key, share);
                totalMoved += moved;
                movedThisRound += moved;

                long newWeight = remainingWeights.get(key) - moved;
                if (moved <= 0 || newWeight <= 0) {
                    settled.add(key);
                } else {
                    remainingWeights.put(key, newWeight);
                }
            }

            remainingWeights.keySet().removeAll(settled);
            remainingAmount -= movedThisRound;
            if (movedThisRound == 0) {
                break;
            }
        }

        return totalMoved;
    }

    /**
     * Splits {@code total} across {@code weights} proportionally, using the largest-remainder
     * method so the shares sum to exactly {@code total} instead of losing units to rounding.
     */
    private static <E> Map<E, Long> largestRemainderShares(Map<E, Long> weights, long total) {
        long totalWeight = 0;
        for (long w : weights.values()) totalWeight += w;

        Map<E, Long> shares = new LinkedHashMap<>();
        if (totalWeight <= 0 || total <= 0) {
            for (E k : weights.keySet()) shares.put(k, 0L);
            return shares;
        }

        Map<E, Long> remainders = new LinkedHashMap<>();
        long allocated = 0;
        for (var entry : weights.entrySet()) {
            long share = (total * entry.getValue()) / totalWeight;
            long remainder = (total * entry.getValue()) % totalWeight;
            shares.put(entry.getKey(), share);
            remainders.put(entry.getKey(), remainder);
            allocated += share;
        }

        long leftover = total - allocated;
        List<E> byRemainder = new ArrayList<>(weights.keySet());
        byRemainder.sort((a, b) -> Long.compare(remainders.get(b), remainders.get(a)));
        for (int i = 0; i < leftover; i++) {
            E key = byRemainder.get(i % byRemainder.size());
            shares.merge(key, 1L, Long::sum);
        }

        return shares;
    }
}
