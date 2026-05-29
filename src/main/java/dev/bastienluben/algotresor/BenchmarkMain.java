package dev.bastienluben.algotresor;

import dev.bastienluben.algotresor.strategy.AdaptiveGreedyStrategy;
import dev.bastienluben.algotresor.strategy.CompetitiveGreedyStrategy;
import dev.bastienluben.algotresor.strategy.DenialAdaptiveStrategy;
import dev.bastienluben.algotresor.strategy.DenialLookaheadStrategy;
import dev.bastienluben.algotresor.strategy.EndgameAwareStrategy;
import dev.bastienluben.algotresor.strategy.GreedyStrategy;
import dev.bastienluben.algotresor.strategy.LocalSearchStrategy;
import dev.bastienluben.algotresor.strategy.MinFootprintStrategy;
import dev.bastienluben.algotresor.strategy.OneLookaheadStrategy;
import dev.bastienluben.algotresor.strategy.PureGreedyMaxCostStrategy;
import dev.bastienluben.algotresor.strategy.Strategy;
import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Entrypoint local pour comparer des stratégies entre elles sans le moteur externe.
 *
 * Usage: BenchmarkMain [--n_items N] [--size_capacity S] [--weight_capacity W] [--seed K] [--rounds R]
 *
 * Sans --n_items / --size_capacity / --weight_capacity, chaque round tire les paramètres
 * aléatoirement dans [500, 1000] (plages de compétition).
 */
public class BenchmarkMain {

    record StrategyEntry(String name, Supplier<Strategy> factory) {}

    static final List<StrategyEntry> STRATEGIES = List.of(
            // Références basses
            new StrategyEntry("PureGreedyMaxCost",          PureGreedyMaxCostStrategy::new),
            new StrategyEntry("MinFootprint",               MinFootprintStrategy::new),
            // Stratégies de base
            new StrategyEntry("Greedy",                     GreedyStrategy::new),
            new StrategyEntry("AdaptiveGreedy",             AdaptiveGreedyStrategy::new),
            new StrategyEntry("LocalSearch",                LocalSearchStrategy::new),
            // Denial
            new StrategyEntry("DenialAdaptive(α=0.5)",      () -> new DenialAdaptiveStrategy(0.5)),
            new StrategyEntry("DenialAdaptive(α=1.0)",      () -> new DenialAdaptiveStrategy(1.0)),
            // Lookahead
            new StrategyEntry("OneLookahead",               OneLookaheadStrategy::new),
            // DenialLookahead
            new StrategyEntry("DenialLA(γ=0.3,δ=0.3)",     () -> new DenialLookaheadStrategy(0.3, 0.3, 60)),
            new StrategyEntry("DenialLA(γ=0.5,δ=0)",       () -> new DenialLookaheadStrategy(0.5, 0.0, 60)),
            new StrategyEntry("DenialLA(γ=0,δ=0.5)",       () -> new DenialLookaheadStrategy(0.0, 0.5, 60)),
            // EndgameAware
            new StrategyEntry("EndgameAware(t=0.2)",        () -> new EndgameAwareStrategy(0.2)),
            new StrategyEntry("EndgameAware(t=0.3)",        () -> new EndgameAwareStrategy(0.3)),
            new StrategyEntry("EndgameAware(t=0.15)",       () -> new EndgameAwareStrategy(0.15))
    );

    public static void main(String[] args) {
        Integer nItemsFixed = null;
        Integer sizeCapacityFixed = null;
        Integer weightCapacityFixed = null;
        long seed = System.currentTimeMillis();
        int rounds = 500;

        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--n_items"        -> nItemsFixed        = Integer.parseInt(args[i + 1]);
                case "--size_capacity"  -> sizeCapacityFixed  = Integer.parseInt(args[i + 1]);
                case "--weight_capacity"-> weightCapacityFixed = Integer.parseInt(args[i + 1]);
                case "--seed"           -> seed               = Long.parseLong(args[i + 1]);
                case "--rounds"         -> rounds             = Integer.parseInt(args[i + 1]);
            }
        }

        int n = STRATEGIES.size();
        int[][] wins        = new int[n][n];
        int[][] draws       = new int[n][n];
        long[][] totScore   = new long[n][n];
        long[] scoreWhenWin  = new long[n];
        long[] scoreWhenLoss = new long[n];

        // Sous-RNG pour les paramètres par round (dérivé du même seed, séparé du RNG des items)
        Random paramRng = new Random(seed ^ 0xDEADBEEFL);

        System.out.println("[benchmark] " + rounds + " rounds, seed=" + seed
                + ", n_items=" + (nItemsFixed != null ? nItemsFixed : "[500,1000]")
                + ", size=" + (sizeCapacityFixed != null ? sizeCapacityFixed : "[500,1000]")
                + ", weight=" + (weightCapacityFixed != null ? weightCapacityFixed : "[500,1000]"));

        long startTime = System.currentTimeMillis();
        int progressStep = Math.max(1, rounds / 20); // report tous les 5%

        for (int round = 0; round < rounds; round++) {
            int nItems        = nItemsFixed        != null ? nItemsFixed        : 500 + paramRng.nextInt(501);
            int sizeCapacity  = sizeCapacityFixed  != null ? sizeCapacityFixed  : 500 + paramRng.nextInt(501);
            int weightCapacity= weightCapacityFixed != null ? weightCapacityFixed : 500 + paramRng.nextInt(501);

            List<Item> items = generateItems(nItems, sizeCapacity, weightCapacity, seed + round);

            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    int[] scores = simulate(items, sizeCapacity, weightCapacity,
                            STRATEGIES.get(i).factory().get(), STRATEGIES.get(j).factory().get());
                    totScore[i][j] += scores[0];
                    totScore[j][i] += scores[1];
                    if (scores[0] > scores[1]) {
                        wins[i][j]++;
                        scoreWhenWin[i]  += scores[0];
                        scoreWhenLoss[j] += scores[1];
                    } else if (scores[1] > scores[0]) {
                        wins[j][i]++;
                        scoreWhenWin[j]  += scores[1];
                        scoreWhenLoss[i] += scores[0];
                    } else {
                        draws[i][j]++;
                        draws[j][i]++;
                    }
                }
            }

            if ((round + 1) % progressStep == 0 || round == rounds - 1) {
                long elapsed = System.currentTimeMillis() - startTime;
                long eta = round > 0 ? elapsed * (rounds - round - 1) / (round + 1) : 0;
                System.out.printf("[%3.0f%%] %d/%d rounds — %ds écoulés, ~%ds restants%n",
                        100.0 * (round + 1) / rounds, round + 1, rounds, elapsed / 1000, eta / 1000);
            }
        }

        System.out.println("\n=== Résultats bruts (" + rounds + " rounds) ===");
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                System.out.printf("%-28s %3d wins  |  %3d draws  |  %3d wins  %-28s   (score moy: %.0f vs %.0f)%n",
                        STRATEGIES.get(i).name(),
                        wins[i][j], draws[i][j], wins[j][i],
                        STRATEGIES.get(j).name(),
                        (double) totScore[i][j] / rounds,
                        (double) totScore[j][i] / rounds);
            }
        }

        printAnalysis(n, rounds, wins, draws, totScore, scoreWhenWin, scoreWhenLoss);
        exportJson(n, rounds, nItemsFixed, sizeCapacityFixed, weightCapacityFixed, seed, wins, draws, totScore);
    }

    private static void printAnalysis(int n, int rounds, int[][] wins, int[][] draws, long[][] totScore,
                                      long[] scoreWhenWin, long[] scoreWhenLoss) {
        int[] totalWins   = new int[n];
        int[] totalDraws  = new int[n];
        int[] totalLosses = new int[n];
        long[] totalScore = new long[n];
        int[] gamesPlayed = new int[n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                totalWins[i]   += wins[i][j];
                totalDraws[i]  += draws[i][j];
                totalLosses[i] += wins[j][i];
                totalScore[i]  += totScore[i][j];
                gamesPlayed[i] += rounds;
            }
        }

        // Identifier les punching bags (win rate == 0)
        boolean[] isPunchingBag = new boolean[n];
        for (int i = 0; i < n; i++) {
            isPunchingBag[i] = (totalWins[i] == 0 && totalLosses[i] > 0);
        }

        // Win rate excl punching bags
        int[] winsExcl   = new int[n];
        int[] lossesExcl = new int[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j || isPunchingBag[j]) continue;
                winsExcl[i]   += wins[i][j];
                lossesExcl[i] += wins[j][i];
            }
        }

        // Classement par win-rate (victoires / parties décisives)
        Integer[] ranked = new Integer[n];
        for (int i = 0; i < n; i++) ranked[i] = i;
        java.util.Arrays.sort(ranked, (a, b) -> {
            double rateA = winRate(totalWins[a], totalLosses[a]);
            double rateB = winRate(totalWins[b], totalLosses[b]);
            int cmp = Double.compare(rateB, rateA);
            if (cmp != 0) return cmp;
            double avgA = gamesPlayed[a] > 0 ? (double) totalScore[a] / gamesPlayed[a] : 0;
            double avgB = gamesPlayed[b] > 0 ? (double) totalScore[b] / gamesPlayed[b] : 0;
            return Double.compare(avgB, avgA);
        });

        System.out.println("\n=== Analyse ===");
        System.out.printf("%-4s  %-28s  %6s  %5s  %6s  %7s  %7s  %9s  %8s  %8s%n",
                "Rang", "Stratégie", "Wins", "Draws", "Losses", "Win%", "WR excl PB", "Score moy", "Score W", "Score L");
        System.out.println("─".repeat(104));

        for (int rank = 0; rank < n; rank++) {
            int i = ranked[rank];
            double rate     = winRate(totalWins[i], totalLosses[i]);
            double rateExcl = winRate(winsExcl[i], lossesExcl[i]);
            double avgSc    = gamesPlayed[i] > 0 ? (double) totalScore[i] / gamesPlayed[i] : 0;
            double avgWin   = totalWins[i]   > 0 ? (double) scoreWhenWin[i]  / totalWins[i]   : 0;
            double avgLoss  = totalLosses[i] > 0 ? (double) scoreWhenLoss[i] / totalLosses[i] : 0;
            System.out.printf("%-4d  %-28s  %6d  %5d  %6d  %6.1f%%  %6.1f%%  %9.0f  %8.0f  %8.0f%n",
                    rank + 1,
                    STRATEGIES.get(i).name(),
                    totalWins[i], totalDraws[i], totalLosses[i],
                    rate * 100,
                    rateExcl * 100,
                    avgSc, avgWin, avgLoss);
        }

        int best = ranked[0];
        System.out.printf("%n→ Meilleure stratégie : %s (win rate %.1f%%, score moy %.0f)%n",
                STRATEGIES.get(best).name(),
                winRate(totalWins[best], totalLosses[best]) * 100,
                gamesPlayed[best] > 0 ? (double) totalScore[best] / gamesPlayed[best] : 0);

        // Signaler les punching bags
        boolean anyPB = false;
        for (int i = 0; i < n; i++) {
            if (isPunchingBag[i]) {
                if (!anyPB) { System.out.println("\n[Punching bags — exclus de WR excl PB] :"); anyPB = true; }
                System.out.println("  - " + STRATEGIES.get(i).name());
            }
        }
    }

    private static double winRate(int wins, int losses) {
        int decisive = wins + losses;
        return decisive == 0 ? 0.5 : (double) wins / decisive;
    }

    private static void exportJson(int n, int rounds,
                                   Integer nItemsFixed, Integer sizeCapFixed, Integer weightCapFixed, long seed,
                                   int[][] wins, int[][] draws, long[][] totScore) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("rounds", rounds);
        config.put("n_items",          nItemsFixed  != null ? nItemsFixed  : "variable [500,1000]");
        config.put("size_capacity",    sizeCapFixed != null ? sizeCapFixed : "variable [500,1000]");
        config.put("weight_capacity",  weightCapFixed != null ? weightCapFixed : "variable [500,1000]");
        config.put("seed", seed);

        List<Map<String, Object>> matchups = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("strategy_a",   STRATEGIES.get(i).name());
                m.put("strategy_b",   STRATEGIES.get(j).name());
                m.put("wins_a",       wins[i][j]);
                m.put("draws",        draws[i][j]);
                m.put("wins_b",       wins[j][i]);
                m.put("avg_score_a",  Math.round((double) totScore[i][j] / rounds));
                m.put("avg_score_b",  Math.round((double) totScore[j][i] / rounds));
                matchups.add(m);
            }
        }

        int[] totalWins   = new int[n];
        int[] totalDraws  = new int[n];
        int[] totalLosses = new int[n];
        long[] totalScore = new long[n];
        int[] gamesPlayed = new int[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                totalWins[i]   += wins[i][j];
                totalDraws[i]  += draws[i][j];
                totalLosses[i] += wins[j][i];
                totalScore[i]  += totScore[i][j];
                gamesPlayed[i] += rounds;
            }
        }

        Integer[] ranked = new Integer[n];
        for (int i = 0; i < n; i++) ranked[i] = i;
        java.util.Arrays.sort(ranked, (a, b) -> {
            int cmp = Double.compare(winRate(totalWins[b], totalLosses[b]), winRate(totalWins[a], totalLosses[a]));
            if (cmp != 0) return cmp;
            return Double.compare(
                    gamesPlayed[b] > 0 ? (double) totalScore[b] / gamesPlayed[b] : 0,
                    gamesPlayed[a] > 0 ? (double) totalScore[a] / gamesPlayed[a] : 0);
        });

        List<Map<String, Object>> rankings = new ArrayList<>();
        for (int rank = 0; rank < n; rank++) {
            int i = ranked[rank];
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("rank",      rank + 1);
            r.put("strategy",  STRATEGIES.get(i).name());
            r.put("wins",      totalWins[i]);
            r.put("draws",     totalDraws[i]);
            r.put("losses",    totalLosses[i]);
            r.put("win_rate",  Math.round(winRate(totalWins[i], totalLosses[i]) * 1000.0) / 10.0);
            r.put("avg_score", Math.round(gamesPlayed[i] > 0 ? (double) totalScore[i] / gamesPlayed[i] : 0));
            rankings.add(r);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("config",        config);
        root.put("matchups",      matchups);
        root.put("rankings",      rankings);
        root.put("best_strategy", STRATEGIES.get(ranked[0]).name());

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        String path = "benchmark_results.json";
        try (FileWriter fw = new FileWriter(path)) {
            gson.toJson(root, fw);
            System.out.println("\nRésultats exportés → " + path);
        } catch (IOException e) {
            System.err.println("[export] Erreur écriture JSON : " + e.getMessage());
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

        return new int[]{game1.getCurrentScore(), game2.getCurrentScore()};
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
            int size   = rng.nextInt(maxItemSize)   + 1;
            int weight = rng.nextInt(maxItemWeight) + 1;
            int cost   = rng.nextInt(100) + 1;
            items.add(new Item(i, size, weight, cost));
        }
        return items;
    }
}
