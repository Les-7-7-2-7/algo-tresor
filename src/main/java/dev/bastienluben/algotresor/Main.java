package dev.bastienluben.algotresor;

import dev.bastienluben.algotresor.strategy.GreedyStrategy;
import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import java.util.Scanner;

public class Main {
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);

		// Consommation des étiquettes et lecture directe des entiers initiaux
		scanner.next(); // "n_items"
		int n = scanner.nextInt();

		scanner.next(); // "size_capacity"
		int sizeCapacity = scanner.nextInt();

		scanner.next(); // "weight_capacity"
		int weightCapacity = scanner.nextInt();

		Game game = new Game(n, sizeCapacity, weightCapacity, new GreedyStrategy());

		// Remplissage des objets sans allocation de String intermédiaire
		for (int i = 0; i < n; i++) {
			int id = scanner.nextInt();
			int size = scanner.nextInt();
			int weight = scanner.nextInt();
			int cost = scanner.nextInt();

			game.addItem(new Item(id, size, weight, cost));
		}

		scanner.next(); // "preprocessing"
		scanner.next(); // "5000"
		game.preprocess();

		// Boucle principale de traitement par jetons
		while (scanner.hasNext()) {
			String token = scanner.next();

			if ("taken".equals(token)) {
				int takenId = scanner.nextInt();
				game.opponentTook(takenId);
			} else if ("next_item".equals(token)) {
				scanner.next(); // Consomme la contrainte de temps (ex: "500")

				int choice = game.pickItem();
				System.out.println(choice);
				System.out.flush();
			}
		}
	}
}