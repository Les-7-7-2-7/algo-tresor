package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import java.util.Comparator;

/**
 * Référence basse : trie par cost décroissant, prend le premier item qui rentre.
 * Ignore taille/poids dans le score — sanity check du bas du classement.
 */
public class PureGreedyMaxCostStrategy implements Strategy {

    @Override
    public void preprocess(Game game) {}

    @Override
    public int pickItem(Game game) {
        int remS = game.getRemainingSize();
        int remW = game.getRemainingWeight();
        return game.getAvailableItems().stream()
                .filter(i -> i.getSize() <= remS && i.getWeight() <= remW)
                .max(Comparator.comparingInt(Item::getCost))
                .map(Item::getId)
                .orElse(-1);
    }
}
