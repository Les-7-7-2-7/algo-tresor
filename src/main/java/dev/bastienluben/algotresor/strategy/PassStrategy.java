package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Item;

import java.util.Collection;

public class PassStrategy implements Strategy {

    @Override
    public void preprocess(Collection<Item> items, int sizeCapacity, int weightCapacity) {
    }

    @Override
    public int pickItem(Collection<Item> availableItems, int currentSize, int currentWeight) {
        return -1;
    }
}
