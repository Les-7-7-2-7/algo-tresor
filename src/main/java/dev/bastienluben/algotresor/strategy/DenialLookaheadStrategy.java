package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fusion DenialAdaptive + OneLookahead.
 *
 * score(item) = myDensity(item) + γ × denial(item) + δ × oppDensity(item)
 *
 * denial(item)   = oppGainBaseline - oppGainAfterMyPick
 * oppDensity     = densité adaptive de l'item du point de vue de l'adversaire (0 si ne rentre pas)
 *
 * γ=0.5, δ=0   → équivalent OneLookahead (sanity check)
 * γ=0,   δ=0.5 → équivalent DenialAdaptive (sanity check)
 */
public class DenialLookaheadStrategy implements Strategy {

    private final double gamma;
    private final double delta;
    private final int candidateLimit;

    public DenialLookaheadStrategy() {
        this(0.3, 0.3, 60);
    }

    public DenialLookaheadStrategy(double gamma, double delta, int candidateLimit) {
        this.gamma = gamma;
        this.delta = delta;
        this.candidateLimit = candidateLimit;
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

        Map<Integer, Item> available = new LinkedHashMap<>();
        for (Item item : game.getAvailableItems()) available.put(item.getId(), item);

        List<Item> candidates = available.values().stream()
                .filter(i -> i.getSize() <= remS && i.getWeight() <= remW)
                .sorted(Comparator.comparingDouble((Item i) -> adaptiveDensity(i, remS, remW)).reversed())
                .limit(candidateLimit)
                .toList();

        if (candidates.isEmpty()) return -1;

        Item oppBaseline = bestOppPick(available, oppRemS, oppRemW);
        double oppBaselineGain = oppBaseline != null ? oppBaseline.getCost() : 0;

        Item bestItem = candidates.get(0);
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Item candidate : candidates) {
            double myDensity = adaptiveDensity(candidate, remS, remW);

            available.remove(candidate.getId());
            double oppGainAfter = 0;
            if (oppRemS > 0 && oppRemW > 0) {
                Item oppAfter = bestOppPick(available, oppRemS, oppRemW);
                oppGainAfter = oppAfter != null ? oppAfter.getCost() : 0;
            }
            available.put(candidate.getId(), candidate);

            double denial = oppBaselineGain - oppGainAfter;

            double oppDensity = (oppRemS > 0 && oppRemW > 0
                    && candidate.getSize() <= oppRemS && candidate.getWeight() <= oppRemW)
                    ? adaptiveDensity(candidate, oppRemS, oppRemW) : 0.0;

            double score = myDensity + gamma * denial + delta * oppDensity;

            if (score > bestScore) {
                bestScore = score;
                bestItem = candidate;
            }
        }

        return bestItem.getId();
    }

    private Item bestOppPick(Map<Integer, Item> available, int oppRemS, int oppRemW) {
        return available.values().stream()
                .filter(i -> i.getSize() <= oppRemS && i.getWeight() <= oppRemW)
                .max(Comparator.comparingDouble(i -> adaptiveDensity(i, oppRemS, oppRemW)))
                .orElse(null);
    }

    private double adaptiveDensity(Item i, int remS, int remW) {
        if (remS <= 0 || remW <= 0) return 0;
        return i.getCost() / ((double) i.getSize() / remS + (double) i.getWeight() / remW);
    }
}
