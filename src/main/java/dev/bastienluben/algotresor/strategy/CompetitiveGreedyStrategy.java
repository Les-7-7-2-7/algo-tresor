package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Score = (1 + alpha) * cost / (size + weight)
 * Alpha weighs the competitive value of denying the opponent the same item.
 */
public class CompetitiveGreedyStrategy implements Strategy {

    private final double alpha;
    private List<ItemWithScore> items;

    public CompetitiveGreedyStrategy() {
        this(0.5);
    }

    public CompetitiveGreedyStrategy(double alpha) {
        this.alpha = alpha;
    }

    private class ItemWithScore {
        final Item item;
        final double score;

        ItemWithScore(Item item) {
            this.item = item;
            this.score = (1.0 + alpha) * item.getCost() / (double) (item.getSize() + item.getWeight());
        }
    }

    @Override
    public void preprocess(Game game) {
        items = game.getAvailableItems().stream()
                .map(ItemWithScore::new)
                .sorted(Comparator.comparingDouble((ItemWithScore i) -> i.score).reversed())
                .toList();
    }

    @Override
    public int pickItem(Game game) {
        Optional<ItemWithScore> best = items.stream()
                .filter(i -> game.isAvailable(i.item.getId()))
                .filter(i -> i.item.getSize() + game.getCurrentSize() <= game.getSizeCapacity())
                .filter(i -> i.item.getWeight() + game.getCurrentWeight() <= game.getWeightCapacity())
                .max(Comparator.comparingDouble(i -> i.score));

        return best.map(i -> i.item.getId()).orElse(-1);
    }
}
