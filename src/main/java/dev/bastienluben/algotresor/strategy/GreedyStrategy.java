package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Stratégie simple, on prends le meilleur a chaque tour
 */
public class GreedyStrategy implements Strategy {

    private List<ItemWithValue> items;

    private static class ItemWithValue {
        private final Item item;
        private final double value;

        public ItemWithValue(Item item) {
            this.item = item;
            this.value = item.getCost() / (double) (item.getSize() + item.getWeight());
        }

        public Item getItem() {
            return item;
        }

        public double getValue() {
            return value;
        }
    }

    @Override
    public int pickItem(Game game) {
        var available = game.getAvailableItems().stream()
                .collect(java.util.stream.Collectors.toMap(Item::getId, i -> i));

        Optional<ItemWithValue> bestItem = items.stream()
                .filter(i -> available.containsKey(i.getItem().getId()))
                .filter(i -> i.getItem().getSize() + game.getCurrentSize() <= game.getSizeCapacity())
                .filter(i -> i.getItem().getWeight() + game.getCurrentWeight() <= game.getWeightCapacity())
                .max(Comparator.comparingDouble(ItemWithValue::getValue));

        return bestItem.map(i -> i.getItem().getId()).orElse(-1);
    }

    @Override
    public void preprocess(Game game) {
        items = game.getAvailableItems().stream()
                        .map(ItemWithValue::new)
                        .sorted(Comparator.comparingDouble(ItemWithValue::getValue).reversed())
                        .toList();
    }
}
