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

        long totalInserted = 0;
        for (var share : largestRemainderShares(demand, transferable).entrySet()) {
            if (share.getValue() <= 0) continue;
            totalInserted += consumers.get(share.getKey()).insert(share.getValue());
        }

        long totalExtracted = 0;
        for (var share : largestRemainderShares(available, totalInserted).entrySet()) {
            if (share.getValue() <= 0) continue;
            totalExtracted += providers.get(share.getKey()).extract(share.getValue());
        }

        return new DistributionResult(totalExtracted, totalDemand, totalInserted);
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
