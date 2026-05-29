package dev.bastienluben.algotresor.structs;

import dev.bastienluben.algotresor.strategy.Strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game {
    private static final String RESET  = "[0m";
    private static final String CYAN   = "[36m";
    private static final String GREEN  = "[32m";
    private static final String RED    = "[31m";
    private static final String YELLOW = "[33m";

    private final Map<Integer, Item> items;
    private final int sizeCapacity;
    private final int weightCapacity;
    private final Strategy strategy;
    private final List<Item> opponentItems;
    private int currentSize;
    private int currentWeight;

    public Game(int numberItems, int sizeCapacity, int weightCapacity, Strategy strategy) {
        this.items = new HashMap<>(numberItems);
        this.sizeCapacity = sizeCapacity;
        this.weightCapacity = weightCapacity;
        this.strategy = strategy;
        this.opponentItems = new ArrayList<>();
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
        System.err.println(CYAN + "[preprocess] " + items.size() + " items, sizeCapacity=" + sizeCapacity + ", weightCapacity=" + weightCapacity + RESET);
        strategy.preprocess(this);
        System.err.println(CYAN + "[preprocess] done" + RESET);
    }

    public List<Item> getOpponentItems() {
        return opponentItems;
    }

    public void opponentTook(int id) {
        if (id == -1) {
            System.err.println(YELLOW + "[opponent] passed" + RESET);
            return;
        }
        Item item = items.remove(id);
        if (item != null) {
            opponentItems.add(item);
            System.err.println(RED + "[opponent] took item " + id + " (size=" + item.getSize() + ", weight=" + item.getWeight() + ", value=" + item.getCost() + ")" + RESET);
        }
    }

    public int pickItem() {
        int chosen = strategy.pickItem(this);
        if (chosen == -1) {
            System.err.println(YELLOW + "[pick] passing" + RESET);
            return -1;
        }
        Item item = items.get(chosen);
        if (item == null) {
            System.err.println(YELLOW + "[pick] invalid id=" + chosen + ", passing" + RESET);
            return -1;
        }
        if (currentSize + item.getSize() > sizeCapacity || currentWeight + item.getWeight() > weightCapacity) {
            System.err.println(YELLOW + "[pick] item " + chosen + " exceeds capacity (size=" + (currentSize + item.getSize()) + "/" + sizeCapacity + ", weight=" + (currentWeight + item.getWeight()) + "/" + weightCapacity + "), passing" + RESET);
            return -1;
        }
        items.remove(chosen);
        currentSize += item.getSize();
        currentWeight += item.getWeight();
        System.err.println(GREEN + "[pick] took item " + chosen + " (size=" + item.getSize() + ", weight=" + item.getWeight() + ", value=" + item.getCost() + ") | bag: size=" + currentSize + "/" + sizeCapacity + ", weight=" + currentWeight + "/" + weightCapacity + RESET);
        return chosen;
    }
}
