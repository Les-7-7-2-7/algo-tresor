package dev.bastienluben.algotresor;

import dev.bastienluben.algotresor.strategy.PassStrategy;
import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import java.util.Scanner;

public class Main {
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);

		int n = Integer.parseInt(scanner.nextLine().split(" ")[1]);
		int sizeCapacity = Integer.parseInt(scanner.nextLine().split(" ")[1]);
		int weightCapacity = Integer.parseInt(scanner.nextLine().split(" ")[1]);

		Game game = new Game(n, sizeCapacity, weightCapacity, new PassStrategy());

		for (int i = 0; i < n; i++) {
			game.addItem(Item.fromString(scanner.nextLine()));
		}

		scanner.nextLine(); // "preprocessing 5000"
		game.preprocess();

		while (scanner.hasNextLine()) {
			int takenId = Integer.parseInt(scanner.nextLine().split(" ")[1]); // "taken ID"
			game.opponentTook(takenId);

			scanner.nextLine(); // "next_item 500"
			int choice = game.pickItem();
			System.out.println(choice);
			System.out.flush();
		}
	}
}