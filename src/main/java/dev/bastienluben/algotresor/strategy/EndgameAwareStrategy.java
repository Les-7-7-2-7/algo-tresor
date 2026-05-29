package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Hybride : AdaptiveGreedy tant que la capacité restante dépasse le seuil,
 * puis DP knapsack 2D optimal en endgame.
 *
 * loadFraction = min(remS/sizeCapacity, remW/weightCapacity)
 * loadFraction >= threshold → AdaptiveGreedy
 * loadFraction <  threshold → DP 2D (fallback AdaptiveGreedy si timeout 400ms)
 *
 * En endgame, l'item avec la plus grande valeur dans la solution DP est choisi
 * en premier pour sécuriser contre les interruptions adverses.
 */
public class EndgameAwareStrategy implements Strategy {

    private final double endgameThreshold;

    public EndgameAwareStrategy() {
        this(0.2);
    }

    public EndgameAwareStrategy(double endgameThreshold) {
        this.endgameThreshold = endgameThreshold;
    }

    @Override
    public void preprocess(Game game) {}

    @Override
    public int pickItem(Game game) {
        int remS = game.getRemainingSize();
        int remW = game.getRemainingWeight();

        double loadFraction = Math.min(
                (double) remS / game.getSizeCapacity(),
                (double) remW / game.getWeightCapacity()
        );

        if (loadFraction >= endgameThreshold) {
            return adaptiveGreedyPick(game.getAvailableItems(), remS, remW);
        }

        List<Item> fitting = new ArrayList<>();
        for (Item item : game.getAvailableItems()) {
            if (item.getSize() <= remS && item.getWeight() <= remW) fitting.add(item);
        }
        if (fitting.isEmpty()) return -1;

        long deadline = System.currentTimeMillis() + 400;
        try {
            List<Item> solution = solveKnapsack2D(fitting, remS, remW, deadline);
            if (solution == null) {
                // timeout → fallback
                return adaptiveGreedyPick(game.getAvailableItems(), remS, remW);
            }
            if (solution.isEmpty()) return -1;
            return solution.stream()
                    .max(Comparator.comparingInt(Item::getCost))
                    .map(Item::getId)
                    .orElse(-1);
        } catch (OutOfMemoryError e) {
            return adaptiveGreedyPick(game.getAvailableItems(), remS, remW);
        }
    }

    /**
     * DP knapsack 2D avec reconstruction de solution.
     * Retourne null si le deadline est dépassé.
     */
    private List<Item> solveKnapsack2D(List<Item> items, int remS, int remW, long deadline) {
        int n = items.size();
        // dp[k][s][w] = meilleure valeur avec items 0..k-1 et capacité (s, w)
        int[][][] dp = new int[n + 1][remS + 1][remW + 1];

        for (int k = 0; k < n; k++) {
            if (System.currentTimeMillis() > deadline) return null;
            Item item = items.get(k);
            int s = item.getSize(), w = item.getWeight(), c = item.getCost();
            for (int si = 0; si <= remS; si++) {
                for (int wi = 0; wi <= remW; wi++) {
                    dp[k + 1][si][wi] = dp[k][si][wi];
                    if (si >= s && wi >= w) {
                        int withItem = dp[k][si - s][wi - w] + c;
                        if (withItem > dp[k + 1][si][wi]) {
                            dp[k + 1][si][wi] = withItem;
                        }
                    }
                }
            }
        }

        // Reconstruction arrière
        List<Item> result = new ArrayList<>();
        int si = remS, wi = remW;
        for (int k = n - 1; k >= 0; k--) {
            Item item = items.get(k);
            int s = item.getSize(), w = item.getWeight(), c = item.getCost();
            if (si >= s && wi >= w && dp[k + 1][si][wi] == dp[k][si - s][wi - w] + c) {
                result.add(item);
                si -= s;
                wi -= w;
            }
        }
        return result;
    }

    private int adaptiveGreedyPick(Iterable<Item> items, int remS, int remW) {
        Item best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Item item : items) {
            if (item.getSize() > remS || item.getWeight() > remW) continue;
            double score = item.getCost() / ((double) item.getSize() / remS + (double) item.getWeight() / remW);
            if (score > bestScore) {
                bestScore = score;
                best = item;
            }
        }
        return best != null ? best.getId() : -1;
    }
}
