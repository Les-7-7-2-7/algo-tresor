# Notes stratégies — observations & pistes

## Stratégies implémentées

### PassStrategy
Passe toujours (`-1`). Référence minimale, score 0.

### GreedyStrategy
Trie les items en preprocess par `cost / (size + weight)`.
Prend le meilleur item disponible qui rentre, score fixe (indépendant de la capacité restante).

### AdaptiveGreedyStrategy
Score recalculé à chaque tour : `cost / (size/remSize + weight/remWeight)`.
Un item qui consomme une grande fraction de la capacité restante est pénalisé.
Pas de preprocess. **Meilleure stratégie actuelle (70.5% win rate).**

### CompetitiveGreedyStrategy *(abandonné)*
Formule du sujet : `score = (1 + α) × cost / (size + weight)`.
Mathématiquement équivalent à GreedyStrategy — `(1 + α)` est constant sur tous les items,
le tri est identique quel que soit α. Sans effet compétitif réel.

### LocalSearchStrategy *(non compétitif)*
Preprocess : solution greedy initiale + hill-climbing (swaps 1-pour-1) pendant 4.5s.
PickItem : suit la liste pré-calculée, fallback adaptive greedy.
Problème fondamental : planifie seul, se fait dépecer en jeu. 0% win rate.

### DenialAdaptiveStrategy
Score : `myDensity(item) + α × oppDensity(item)`
- `myDensity` = densité adaptive de mon point de vue
- `oppDensity` = densité adaptive du point de vue adversaire (0 si l'item ne rentre pas dans son sac)
Bonus proportionnel à quel point l'adversaire veut l'item. Paramètre α (défaut 0.5).
**65% win rate (α=0.5).**

### OneLookaheadStrategy
Score : `myDensity(item) + γ × denial(item)`
- `denial(item)` = gain adversaire sans interférence − gain adversaire après mon pick
  → bonus élevé uniquement si on prend un item que l'adversaire aurait pris
- Simule la meilleure réponse adversaire pour chaque candidat (top-60 par densité)
Paramètres : γ (défaut 0.5), candidateLimit (défaut 60).
**64% win rate (γ=0.5).**

---

## Résultats benchmark (50 rounds, seed aléatoire)

| Rang | Stratégie | Win rate | Score moy |
|------|-----------|----------|-----------|
| 1 | AdaptiveGreedy | 70.5% | 1973 |
| 2 | DenialAdaptive(α=0.5) | 65.0% | 1960 |
| 3 | OneLookahead(γ=0.5) | 64.0% | 1960 |
| 4 | DenialAdaptive(α=1.0) | 50.5% | 1951 |
| 5 | LocalSearch | 0% | 1863 |

---

## Observations

### 1. CompetitiveGreedy est mathématiquement inutile

`score = (1 + α) × cost / (size + weight)` — le facteur `(1 + α)` est constant sur tous les items,
donc le tri est identique à GreedyStrategy quel que soit α. La formule du sujet ne capture pas
le côté compétitif.

### 2. LocalSearch ignore l'adversaire

Planifier une solution optimale en isolation puis la suivre ne fonctionne pas : l'adversaire prend
exactement les items ciblés. 0% de win rate contre toutes les autres stratégies. À abandonner
ou refondre complètement.

### 3. OneLookahead V1 (myGain - oppGain) était contre-productif

Maximiser `myGain - oppGain` poussait la stratégie à prendre des items médiocres pour "gagner"
le delta local. Résultat : score absolu de 1597, 0% de win rate.

**Fix** : remplacer par `myDensity + γ × denial` où `denial = oppGainBaseline - oppGainAfterMyPick`.
Le bonus de denial ne s'applique que si on prend un item que l'adversaire aurait pris.
Résultat après fix : 64% win rate.

### 4. DenialAdaptive — soft vs binaire

La version binaire (`opponentCanTake ? 1 : 0`) donnait déjà 74% en 30 rounds.
La version soft (`oppDensity` proportionnelle) est plus précise :
items très désirés par l'adversaire → bonus plus fort.

### 5. α trop élevé nuit

DenialAdaptive(α=1.0) plafonne à 50.5% — trop de poids sur le denial réduit la valeur absolue collectée.
Plage optimale estimée : α ∈ [0.3, 0.6].

---

## Idée suivante : DenialLookahead

Fusionner les deux meilleures approches.

**Problème de DenialAdaptive** : estime la capacité adversaire depuis les items observés,
mais ignore ce que l'adversaire *choisirait vraiment* parmi les items restants.

**Problème de OneLookahead** : simule la réponse adversaire mais ne prend en compte que
le meilleur pick immédiat — pas la densité adaptative du point de vue de l'adversaire.

**Proposition** :

```
score(item) = myDensity(item)
            + γ × denial(item)
            + δ × oppDensity(item)   ← nouveau terme
```

Où :
- `myDensity` = densité adaptive de mon point de vue
- `denial` = reduction du meilleur pick adversaire si je prends cet item (lookahead simulé)
- `oppDensity` = densité adaptive de l'item du point de vue de l'adversaire (estime à quel point
  il le veut, indépendamment du denial immédiat)

Le terme `oppDensity` capture les items que l'adversaire voudra dans les prochains tours,
pas seulement au tour suivant.

**Valeurs initiales à tester** : γ = 0.3, δ = 0.3

**Implémentation** : classe `DenialLookaheadStrategy(gamma, delta, candidateLimit)`.
