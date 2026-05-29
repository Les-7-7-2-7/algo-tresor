package dev.bastienluben.algotresor.structs;

import java.util.HashMap;
import java.util.Map;

public class Game {
    private final Map<Integer, Item> items;
    private final int sizeCapacity;
    private final int weightCapacity;
    private int currentSize;
    private int currentWeight;

    public Game(int numberItems, int sizeCapacity, int weightCapacity) {
        this.items = new HashMap<>(numberItems);
        this.sizeCapacity = sizeCapacity;
        this.weightCapacity = weightCapacity;
        this.currentSize = 0;
        this.currentWeight = 0;
    }

    public void addItem(Item item) {
        items.put(item.getId(), item);
    }

    public void preprocess() {
        // TODO: calculs préliminaires
    }

    public void opponentTook(int id) {
        items.remove(id);
    }

    public int pickItem() {
        // TODO: stratégie
        return -1;
    }
}
