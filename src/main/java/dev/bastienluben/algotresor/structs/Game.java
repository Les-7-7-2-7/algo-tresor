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
		int chosenId = -1;
		int proposedId = this.strategy.pickItem(this);

		if (proposedId != -1) {
			Item item = this.items.get(proposedId);

			if (item != null
					&& (this.currentWeight + item.getWeight()) <= this.weightCapacity
					&& (this.currentSize + item.getSize()) <= this.sizeCapacity) {

				chosenId = proposedId;
				this.items.remove(chosenId);
				this.currentSize += item.getSize();
				this.currentWeight += item.getWeight();
			}
		}

		return chosenId;
	}
}
