package dev.bastienluben.algotresor;

import dev.bastienluben.algotresor.strategy.AdaptiveGreedyStrategy;
import dev.bastienluben.algotresor.strategy.GreedyStrategy;
import dev.bastienluben.algotresor.strategy.Strategy;
import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Entrypoint local pour comparer des stratégies entre elles sans le moteur externe.
 *
 * Usage: BenchmarkMain [--n_items N] [--size_capacity S] [--weight_capacity W] [--seed K] [--rounds R]
 */
public class BenchmarkMain {

    record StrategyEntry(String name, Supplier<Strategy> factory) {}

    static final List<StrategyEntry> STRATEGIES = List.of(
        new StrategyEntry("Greedy",         GreedyStrategy::new),
        new StrategyEntry("AdaptiveGreedy", AdaptiveGreedyStrategy::new)
    );

    public static void main(String[] args) {
        int nItems        = 600;
        int sizeCapacity  = 300;
        int weightCapacity = 400;
        long seed         = System.currentTimeMillis();
        int rounds        = 100;

        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--n_items"         -> nItems         = Integer.parseInt(args[i + 1]);
                case "--size_capacity"   -> sizeCapacity   = Integer.parseInt(args[i + 1]);
                case "--weight_capacity" -> weightCapacity  = Integer.parseInt(args[i + 1]);
                case "--seed"            -> seed            = Long.parseLong(args[i + 1]);
                case "--rounds"          -> rounds          = Integer.parseInt(args[i + 1]);
            }
        }

        int n = STRATEGIES.size();
        // wins[i][j] = nombre de victoires de i contre j
        int[][] wins      = new int[n][n];
        int[][] draws     = new int[n][n];
        long[][] totScore = new long[n][n];

        System.err.println("[benchmark] " + rounds + " rounds, n_items=" + nItems
            + ", size=" + sizeCapacity + ", weight=" + weightCapacity + ", seed=" + seed);

        for (int round = 0; round < rounds; round++) {
            List<Item> items = generateItems(nItems, sizeCapacity, weightCapacity, seed + round);

            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    int[] scores = simulate(items, sizeCapacity, weightCapacity,
                        STRATEGIES.get(i).factory().get(), STRATEGIES.get(j).factory().get());
                    totScore[i][j] += scores[0];
                    totScore[j][i] += scores[1];
                    if (scores[0] > scores[1])      { wins[i][j]++; }
                    else if (scores[1] > scores[0]) { wins[j][i]++; }
                    else                            { draws[i][j]++; draws[j][i]++; }
                }
            }
        }

        System.out.println("\n=== Résultats (" + rounds + " rounds) ===");
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                String ni = STRATEGIES.get(i).name();
                String nj = STRATEGIES.get(j).name();
                System.out.printf("%-20s %3d wins  |  %3d draws  |  %3d wins %-20s   (score moy: %.0f vs %.0f)%n",
                    ni,
                    wins[i][j], draws[i][j], wins[j][i],
                    nj,
                    (double) totScore[i][j] / rounds,
                    (double) totScore[j][i] / rounds);
            }
        }
    }

    private static int[] simulate(List<Item> items, int sizeCapacity, int weightCapacity,
                                  Strategy s1, Strategy s2) {
        Game game1 = buildGame(items, sizeCapacity, weightCapacity, s1);
        Game game2 = buildGame(items, sizeCapacity, weightCapacity, s2);

        game1.preprocess();
        game2.preprocess();

        boolean done1 = false, done2 = false;
        boolean p1Turn = true;

        while (!done1 || !done2) {
            if (p1Turn && !done1) {
                int chosen = game1.pickItem();
                if (chosen == -1) done1 = true;
                else game2.opponentTook(chosen);
            } else if (!p1Turn && !done2) {
                int chosen = game2.pickItem();
                if (chosen == -1) done2 = true;
                else game1.opponentTook(chosen);
            }
            p1Turn = !p1Turn;
        }

        return new int[]{ game1.getCurrentScore(), game2.getCurrentScore() };
    }

    private static Game buildGame(List<Item> items, int sizeCapacity, int weightCapacity, Strategy strategy) {
        Game game = new Game(items.size(), sizeCapacity, weightCapacity, strategy);
        for (Item item : items) game.addItem(item);
        return game;
    }

    private static List<Item> generateItems(int n, int sizeCapacity, int weightCapacity, long seed) {
        Random rng = new Random(seed);
        int maxItemSize   = Math.max(1, sizeCapacity / 5);
        int maxItemWeight = Math.max(1, weightCapacity / 5);
        List<Item> items = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int size   = rng.nextInt(maxItemSize) + 1;
            int weight = rng.nextInt(maxItemWeight) + 1;
            int cost   = rng.nextInt(100) + 1;
            items.add(new Item(i, size, weight, cost));
        }
        return items;
    }
}
