package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import java.util.Comparator;
import java.util.Optional;

/**
 * AdaptiveGreedy + bonus de denial proportionnel.
 *
 * score(item) = myDensity(item) + alpha × oppDensity(item)
 *
 * oppDensity = densité adaptive de l'item du point de vue de l'adversaire
 * (0 si l'item ne rentre pas dans son sac). Le bonus est proportionnel à
 * quel point l'adversaire veut vraiment cet item, pas juste binaire.
 */
public class DenialAdaptiveStrategy implements Strategy {

    private final double alpha;

    public DenialAdaptiveStrategy() {
        this(0.5);
    }

    public DenialAdaptiveStrategy(double alpha) {
        this.alpha = alpha;
    }

    @Override
    public void preprocess(Game game) {}

    @Override
    public int pickItem(Game game) {
        int remS = game.getRemainingSize();
        int remW = game.getRemainingWeight();

        int oppUsedS = game.getOpponentItems().stream().mapToInt(Item::getSize).sum();
        int oppUsedW = game.getOpponentItems().stream().mapToInt(Item::getWeight).sum();
        int oppRemS = game.getSizeCapacity() - oppUsedS;
        int oppRemW = game.getWeightCapacity() - oppUsedW;

        Optional<Item> best = game.getAvailableItems().stream()
            .filter(i -> i.getSize() <= remS && i.getWeight() <= remW)
            .max(Comparator.comparingDouble(i -> score(i, remS, remW, oppRemS, oppRemW)));

        return best.map(Item::getId).orElse(-1);
    }

    private double score(Item item, int remS, int remW, int oppRemS, int oppRemW) {
        double myDensity = adaptiveDensity(item, remS, remW);
        double oppDensity = (oppRemS > 0 && oppRemW > 0
                && item.getSize() <= oppRemS && item.getWeight() <= oppRemW)
            ? adaptiveDensity(item, oppRemS, oppRemW) : 0.0;
        return myDensity + alpha * oppDensity;
    }

    private double adaptiveDensity(Item item, int remS, int remW) {
        return item.getCost() / ((double) item.getSize() / remS + (double) item.getWeight() / remW);
    }
}
