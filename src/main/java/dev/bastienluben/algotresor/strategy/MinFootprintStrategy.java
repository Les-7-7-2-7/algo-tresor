package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import java.util.Comparator;

/**
 * Référence : trie par (size + weight) croissant, prend le premier item qui rentre.
 * Vise à maximiser le nombre d'items pris — révèle si "beaucoup de petits" bat "quelques gros".
 */
public class MinFootprintStrategy implements Strategy {

    @Override
    public void preprocess(Game game) {}

    @Override
    public int pickItem(Game game) {
        int remS = game.getRemainingSize();
        int remW = game.getRemainingWeight();
        return game.getAvailableItems().stream()
                .filter(i -> i.getSize() <= remS && i.getWeight() <= remW)
                .min(Comparator.comparingInt(i -> i.getSize() + i.getWeight()))
                .map(Item::getId)
                .orElse(-1);
    }
}
