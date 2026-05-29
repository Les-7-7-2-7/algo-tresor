package dev.bastienluben.algotresor.structs;

import dev.bastienluben.algotresor.strategy.Strategy;

import java.util.HashMap;
import java.util.Map;

public class Game {
    private final Map<Integer, Item> items;
    private final int sizeCapacity;
    private final int weightCapacity;
    private final Strategy strategy;
    private int currentSize;
    private int currentWeight;

    public Game(int numberItems, int sizeCapacity, int weightCapacity, Strategy strategy) {
        this.items = new HashMap<>(numberItems);
        this.sizeCapacity = sizeCapacity;
        this.weightCapacity = weightCapacity;
        this.strategy = strategy;
        this.currentSize = 0;
        this.currentWeight = 0;
    }

    public void addItem(Item item) {
        items.put(item.getId(), item);
    }

    public void preprocess() {
        strategy.preprocess(items.values(), sizeCapacity, weightCapacity);
    }

    public void opponentTook(int id) {
        items.remove(id);
    }

    public int pickItem() {
        int chosen = strategy.pickItem(items.values(), currentSize, currentWeight);
        if (chosen != -1) {
            Item item = items.remove(chosen);
            currentSize += item.getSize();
            currentWeight += item.getWeight();
        }
        return chosen;
    }
}
