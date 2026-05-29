package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Game;

public class PassStrategy implements Strategy {

	@Override
	public void preprocess(Game game) {
	}

	@Override
	public int pickItem(Game game) {
		return -1;
	}
}
