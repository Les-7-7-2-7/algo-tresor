package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Item;

import java.util.Collection;

public interface Strategy {
    void preprocess(Collection<Item> items, int sizeCapacity, int weightCapacity);
    int pickItem(Collection<Item> availableItems, int currentSize, int currentWeight);
}
