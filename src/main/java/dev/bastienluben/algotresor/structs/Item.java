package dev.bastienluben.algotresor.structs;

import java.util.Objects;

public class Item {
    private int id;
    private int size;
    private int weight;
    private int cost;

    public Item(int id, int size, int weight, int cost) {
        this.id = id;
        this.size = size;
        this.weight = weight;
        this.cost = cost;
    }

    public int getId() {
        return id;
    }

    public int getSize() {
        return size;
    }

    public int getWeight() {
        return weight;
    }

    public int getCost() {
        return cost;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return id == item.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public static Item fromString(String line) {
        String[] numbers = line.split(" ");
        int id = Integer.parseInt(numbers[0]);
        int size = Integer.parseInt(numbers[1]);
        int weight = Integer.parseInt(numbers[2]);
        int cost = Integer.parseInt(numbers[3]);

        return new Item(id, size, weight, cost);
    }
}
