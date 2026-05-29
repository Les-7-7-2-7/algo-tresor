package dev.bastienluben.algotresor.structs;

import dev.bastienluben.algotresor.strategy.Strategy;

import java.util.Collection;
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

    public Collection<Item> getAvailableItems() {
        return items.values();
    }

    public int getSizeCapacity() {
        return sizeCapacity;
    }

    public int getWeightCapacity() {
        return weightCapacity;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public int getCurrentWeight() {
        return currentWeight;
    }

    public void preprocess() {
        strategy.preprocess(this);
    }

    public void opponentTook(int id) {
        items.remove(id);
    }

    public int pickItem() {
        int chosen = strategy.pickItem(this);
        if (chosen != -1) {
            Item item = items.remove(chosen);
            currentSize += item.getSize();
            currentWeight += item.getWeight();
        }
        return chosen;
    }
}
