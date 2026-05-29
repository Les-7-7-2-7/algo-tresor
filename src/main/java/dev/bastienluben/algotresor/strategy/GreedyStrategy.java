package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import java.util.Comparator;
import java.util.List;

/**
 * Stratégie gloutonne dynamique avec déni de ressources (blocage).
 */
public class GreedyStrategy implements Strategy {

	private List<ItemWithValue> items;

	private static class ItemWithValue {
		private final Item item;
		private final double baseValue;

		public ItemWithValue(Item item, double c_size, double c_weight) {
			this.item = item;
			// Correction de la priorité des opérateurs avec double parenthèse
			this.baseValue = item.getCost() / ((c_size * item.getSize()) + (c_weight * item.getWeight()));
		}

		public Item getItem() {
			return item;
		}

		public double getBaseValue() {
			return baseValue;
		}
	}

	@Override
	public int pickItem(Game game) {
		int output = -1;

		int opponentRemainingSize = game.getSizeCapacity();
		int opponentRemainingWeight = game.getWeightCapacity();

		for (Item oppItem : game.getOpponentItems()) {
			opponentRemainingSize -= oppItem.getSize();
			opponentRemainingWeight -= oppItem.getWeight();
		}

		double bestScore = -1.0;

		for (ItemWithValue itemWithValue : items) {
			Item item = itemWithValue.getItem();

			if (game.containsItem(item.getId())) {
				boolean fitsInMe = game.getRemainingSize() >= item.getSize() &&
						game.getRemainingWeight() >= item.getWeight();

				if (fitsInMe) {
					double currentScore = itemWithValue.getBaseValue();

					boolean fitsInOpponent = opponentRemainingSize >= item.getSize() &&
							opponentRemainingWeight >= item.getWeight();

					if (fitsInOpponent) {
						currentScore += (itemWithValue.getBaseValue() * 0.5);
					}

					if (currentScore > bestScore) {
						bestScore = currentScore;
						output = item.getId();
					}
				}
			}
		}

		return output;
	}

	@Override
	public void preprocess(Game game) {
		long totalSize = 0,
			totalWeight = 0;

		for (Item item : game.getAvailableItems()) {
			totalSize += item.getSize();
			totalWeight += item.getWeight();
		}

		double c_size = totalSize / (double)game.getSizeCapacity(),
			c_weight = totalWeight / (double)game.getWeightCapacity();

		items = game.getAvailableItems().stream()
				.map(item -> new ItemWithValue(item, c_size, c_weight))
				.sorted(Comparator.comparingDouble(ItemWithValue::getBaseValue).reversed())
				.toList();
	}
}