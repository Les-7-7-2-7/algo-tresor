package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import java.util.Comparator;
import java.util.Optional;

/**
 * Greedy adaptatif : score recalculé à chaque tour selon capacité restante.
 * score = value / (size/remainingSize + weight/remainingWeight)
 * Un item qui consomme une grande fraction de la capacité restante est pénalisé.
 */
public class AdaptiveGreedyStrategy implements Strategy {

    @Override
    public void preprocess(Game game) {
        // Pas de précalcul nécessaire, les scores dépendent de la capacité restante
    }

    @Override
    public int pickItem(Game game) {
        int remainingSize = game.getSizeCapacity() - game.getCurrentSize();
        int remainingWeight = game.getWeightCapacity() - game.getCurrentWeight();

        Optional<Item> best = game.getAvailableItems().stream()
                .filter(i -> i.getSize() <= remainingSize)
                .filter(i -> i.getWeight() <= remainingWeight)
                .max(Comparator.comparingDouble(i -> score(i, remainingSize, remainingWeight)));

        return best.map(Item::getId).orElse(-1);
    }

    private double score(Item item, int remainingSize, int remainingWeight) {
        double sizeFraction = (double) item.getSize() / remainingSize;
        double weightFraction = (double) item.getWeight() / remainingWeight;
        return item.getCost() / (sizeFraction + weightFraction);
    }
}
