package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import java.util.*;

/**
 * Lookahead 1 coup avec objectif corrigé.
 *
 * Ancienne erreur : maximiser (myGain - oppGain) sacrifiait le score absolu.
 * Nouvelle formule : score = myDensity(item) + gamma × denial(item)
 *
 * denial(item) = gain adversaire sans ce pick - gain adversaire après ce pick
 *              = combien on réduit le potentiel de l'adversaire en prenant cet item.
 * Bonus élevé seulement si on prend quelque chose que l'adversaire aurait pris.
 */
public class OneLookaheadStrategy implements Strategy {

    private final double gamma;
    private final int candidateLimit;

    public OneLookaheadStrategy() {
        this(0.5, 60);
    }

    public OneLookaheadStrategy(double gamma, int candidateLimit) {
        this.gamma = gamma;
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

        // Baseline : meilleur pick adversaire sans aucune interférence de ma part
        Item oppBaseline = bestOppPick(available, oppRemS, oppRemW);
        double oppBaselineGain = oppBaseline != null ? oppBaseline.getCost() : 0;

        Item bestItem = candidates.get(0);
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Item candidate : candidates) {
            double myDensity = adaptiveDensity(candidate, remS, remW);

            // Meilleur pick adversaire après que je prenne candidate
            available.remove(candidate.getId());
            double oppGainAfter = 0;
            if (oppRemS > 0 && oppRemW > 0) {
                Item oppAfter = bestOppPick(available, oppRemS, oppRemW);
                oppGainAfter = oppAfter != null ? oppAfter.getCost() : 0;
            }
            available.put(candidate.getId(), candidate);

            // denial = combien je réduis le gain potentiel adversaire
            double denial = oppBaselineGain - oppGainAfter;
            double score = myDensity + gamma * denial;

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
