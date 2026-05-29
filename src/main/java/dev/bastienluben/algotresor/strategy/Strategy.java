package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Game;

public interface Strategy {
    void preprocess(Game game);
    int pickItem(Game game);
}
