package dev.bastienluben.algotresor.strategy;

import dev.bastienluben.algotresor.structs.Game;
import dev.bastienluben.algotresor.structs.Item;

import java.util.*;

/**
 * Calcule une bonne solution hors-ligne en preprocess via hill-climbing (swaps 1-pour-1),
 * puis suit cet ordre en jeu. Fallback : densité adaptive si un item visé a été pris.
 */
public class LocalSearchStrategy implements Strategy {

    private List<Item> targetItems;

    @Override
    public void preprocess(Game game) {
        long deadline = System.currentTimeMillis() + 4500;
        int S = game.getSizeCapacity();
        int W = game.getWeightCapacity();

        List<Item> all = new ArrayList<>(game.getAvailableItems());
        all.sort(Comparator.comparingDouble((Item i) -> (double) i.getCost() / (i.getSize() + i.getWeight())).reversed());

        Map<Integer, Item> byId = new HashMap<>();
        for (Item item : all) byId.put(item.getId(), item);

        // Solution greedy initiale
        Set<Integer> inBag = new LinkedHashSet<>();
        int curSize = 0, curWeight = 0, curValue = 0;
        for (Item item : all) {
            if (curSize + item.getSize() <= S && curWeight + item.getWeight() <= W) {
                inBag.add(item.getId());
                curSize += item.getSize();
                curWeight += item.getWeight();
                curValue += item.getCost();
            }
        }

        // Hill-climbing : swaps 1-pour-1
        boolean improved = true;
        while (improved && System.currentTimeMillis() < deadline) {
            improved = false;
            for (Item j : all) {
                if (inBag.contains(j.getId())) continue;
                // Ajout direct si ça rentre
                if (curSize + j.getSize() <= S && curWeight + j.getWeight() <= W) {
                    inBag.add(j.getId());
                    curSize += j.getSize();
                    curWeight += j.getWeight();
                    curValue += j.getCost();
                    improved = true;
                    continue;
                }
                // Swap j contre i si gain positif
                for (int iId : new ArrayList<>(inBag)) {
                    if (System.currentTimeMillis() >= deadline) break;
                    Item i = byId.get(iId);
                    int ns = curSize - i.getSize() + j.getSize();
                    int nw = curWeight - i.getWeight() + j.getWeight();
                    if (ns <= S && nw <= W && j.getCost() > i.getCost()) {
                        inBag.remove(iId);
                        inBag.add(j.getId());
                        curSize = ns;
                        curWeight = nw;
                        curValue = curValue - i.getCost() + j.getCost();
                        improved = true;
                        break;
                    }
                }
                if (System.currentTimeMillis() >= deadline) break;
            }
        }

        targetItems = new ArrayList<>();
        for (int id : inBag) targetItems.add(byId.get(id));
        targetItems.sort(Comparator.comparingInt(Item::getCost).reversed());

        System.err.println("[LocalSearch] value=" + curValue + ", items=" + targetItems.size());
    }

    @Override
    public int pickItem(Game game) {
        int remS = game.getRemainingSize();
        int remW = game.getRemainingWeight();

        for (Item item : targetItems) {
            if (game.isAvailable(item.getId()) && item.getSize() <= remS && item.getWeight() <= remW) {
                return item.getId();
            }
        }

        // Fallback adaptive greedy
        return game.getAvailableItems().stream()
            .filter(i -> i.getSize() <= remS && i.getWeight() <= remW)
            .max(Comparator.comparingDouble(i -> adaptiveDensity(i, remS, remW)))
            .map(Item::getId)
            .orElse(-1);
    }

    private double adaptiveDensity(Item i, int remS, int remW) {
        return i.getCost() / ((double) i.getSize() / remS + (double) i.getWeight() / remW);
    }
}
