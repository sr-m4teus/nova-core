package com.novacore.energy.network;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Tracks which {@link EnergyNetwork} each node belongs to, merging networks when a node is
 * added next to existing ones and splitting a network into its remaining connected components
 * when a node is removed. Adjacency is supplied by the caller ({@code neighborLookup}), so this
 * class has no notion of world space and can be tested with plain node keys.
 */
public final class EnergyNetworkManager<K> {

    private final Function<K, ? extends Collection<K>> neighborLookup;
    private final Map<K, EnergyNetwork<K>> networkByNode = new HashMap<>();

    public EnergyNetworkManager(Function<K, ? extends Collection<K>> neighborLookup) {
        this.neighborLookup = neighborLookup;
    }

    public EnergyNetwork<K> networkOf(K node) {
        return networkByNode.get(node);
    }

    public EnergyNetwork<K> addNode(K node) {
        Set<EnergyNetwork<K>> toMerge = new LinkedHashSet<>();
        for (K neighbor : neighborLookup.apply(node)) {
            EnergyNetwork<K> network = networkByNode.get(neighbor);
            if (network != null) {
                toMerge.add(network);
            }
        }

        EnergyNetwork<K> result = toMerge.isEmpty() ? new EnergyNetwork<>() : mergeAll(toMerge);
        result.addNode(node);
        networkByNode.put(node, result);
        return result;
    }

    private EnergyNetwork<K> mergeAll(Set<EnergyNetwork<K>> networks) {
        var it = networks.iterator();
        EnergyNetwork<K> target = it.next();
        while (it.hasNext()) {
            EnergyNetwork<K> other = it.next();
            for (K node : other.nodes()) {
                networkByNode.put(node, target);
            }
            target.absorb(other);
        }
        return target;
    }

    public void removeNode(K node) {
        EnergyNetwork<K> old = networkByNode.remove(node);
        if (old == null) {
            return;
        }

        Set<K> remaining = new HashSet<>(old.nodes());
        remaining.remove(node);
        old.removeNode(node);

        Set<K> unvisited = new HashSet<>(remaining);
        while (!unvisited.isEmpty()) {
            K seed = unvisited.iterator().next();
            unvisited.remove(seed);

            EnergyNetwork<K> component = new EnergyNetwork<>();
            Deque<K> queue = new ArrayDeque<>();
            queue.add(seed);

            while (!queue.isEmpty()) {
                K current = queue.poll();
                component.addNode(current);
                networkByNode.put(current, component);

                EnergyProvider provider = old.takeProvider(current);
                if (provider != null) component.attachProvider(current, provider);
                EnergyConsumer consumer = old.takeConsumer(current);
                if (consumer != null) component.attachConsumer(current, consumer);

                for (K neighbor : neighborLookup.apply(current)) {
                    if (remaining.contains(neighbor) && unvisited.remove(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

    public void attachProvider(K node, EnergyProvider provider) {
        requireNetwork(node).attachProvider(node, provider);
    }

    public void attachConsumer(K node, EnergyConsumer consumer) {
        requireNetwork(node).attachConsumer(node, consumer);
    }

    private EnergyNetwork<K> requireNetwork(K node) {
        EnergyNetwork<K> network = networkByNode.get(node);
        if (network == null) {
            throw new IllegalStateException("Node " + node + " is not part of any network");
        }
        return network;
    }
}
