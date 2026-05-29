package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Stratégie simple, on prends le meilleur a chaque tour
 */
public class GreedyStrategy implements Strategy {

	private List<ItemWithValue> items;

	private static class ItemWithValue {
		private final Item item;
		private final double value;

		public ItemWithValue(Item item) {
			this.item = item;
			this.value = item.getCost() / (double) (item.getSize() + item.getWeight());
		}

		public Item getItem() {
			return item;
		}

		public double getValue() {
			return value;
		}
	}

	@Override
	public int pickItem(Game game) {
		int output = -1;

		for (ItemWithValue itemWithValue : items) {
			Item item = itemWithValue.getItem();
			boolean available = game.containsItem(item.getId());
			boolean fits_in = game.getRemainingSize() >= item.getSize() &&
					game.getRemainingWeight() >= item.getWeight();

			if (available && fits_in) {
				output = item.getId();
				break;
			}
		}

		return output;
	}

	@Override
	public void preprocess(Game game) {
		items = game.getAvailableItems().stream()
				.map(ItemWithValue::new)
				.sorted(Comparator.comparingDouble(ItemWithValue::getValue).reversed())
				.toList();
	}
}
