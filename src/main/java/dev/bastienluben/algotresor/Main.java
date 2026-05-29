package dev.bastienluben.algotresor;

import dev.bastienluben.algotresor.strategy.GreedyStrategy;
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
            String line = scanner.nextLine();
            if (line.startsWith("taken")) {
                int takenId = Integer.parseInt(line.split(" ")[1]);
                game.opponentTook(takenId);
            } else if (line.startsWith("next_item")) {
                int choice = game.pickItem();
                System.out.println(choice);
                System.out.flush();
            }
        }
    }
}