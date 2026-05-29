# Consignes — prochaines étapes

Plan d'implémentation pour solidifier le benchmark puis tester deux nouvelles stratégies.
À exécuter dans l'ordre. Chaque phase doit produire des résultats avant de passer à la suivante.

---

## Contexte

Paramètres de la compétition (extrait de `RULES.md`) :
- `n_items` ∈ [500, 1000]
- `size_capacity` ∈ [500, 1000]
- `weight_capacity` ∈ [500, 1000]
- 5000 ms preprocess, 500 ms par tour, 3 fautes = élimination

Le benchmark actuel tourne à 600/300/400 (capacités plus serrées que la compétition).
Plus de capacité en compétition → plus de tours par partie → l'endgame et le denial pèsent plus.

Les classements actuels (50 rounds) ne sont pas significatifs : écart de score absolu
entre les 3 meilleures stratégies < 1 %, et AdaptiveGreedy perd 23-27 en head-to-head
contre OneLookahead.

---

## Phase 1 — Solidifier `BenchmarkMain`

Objectif : rendre les résultats statistiquement fiables et représentatifs de la compétition.

### 1.1 Augmenter le nombre de rounds
- Passer le défaut de 100 à **500 rounds**.
- Garder l'option `--rounds R` pour pouvoir descendre en dev.

### 1.2 Échantillonner les paramètres dans les plages de compétition
Aujourd'hui chaque round utilise les mêmes `nItems / sizeCapacity / weightCapacity`.
À la place, tirer ces 3 valeurs aléatoirement **par round** dans :
- `nItems` ∈ [500, 1000]
- `sizeCapacity` ∈ [500, 1000]
- `weightCapacity` ∈ [500, 1000]

Conserver les flags `--n_items`, `--size_capacity`, `--weight_capacity` mais comme **valeur fixe optionnelle** (si fournis, on désactive l'échantillonnage pour ce paramètre).

Le seed doit rester reproductible : utiliser `seed + round` pour le RNG du round (déjà le cas pour les items), et un sous-RNG dérivé du même seed pour les params.

### 1.3 Ajouter 2 adversaires de référence
Créer dans `strategy/` :

**`PureGreedyMaxCostStrategy`** — trie par `cost` décroissant, prend le premier item qui rentre. Ignore taille/poids dans le score. Sanity check du bas du classement.

**`MinFootprintStrategy`** — trie par `size + weight` croissant, prend le premier item qui rentre. Vise à maximiser le nombre d'items pris. Révèle les cas où "beaucoup de petits" bat "quelques gros".

Les ajouter à `STRATEGIES` dans `BenchmarkMain`.

### 1.4 Rapport plus informatif
Dans `printAnalysis`, ajouter une colonne **win rate hors LocalSearch** (ou plus généralement : hors stratégies qui perdent toutes leurs parties). Évite que les stratégies "punching bag" gonflent artificiellement le classement.

Ajouter aussi le **score moyen quand la stratégie gagne** et **quand elle perd** (deux colonnes), pour repérer les stratégies qui gagnent serré vs celles qui gagnent large.

---

## Phase 2 — Implémenter `DenialLookaheadStrategy`

Fusion de `DenialAdaptive` et `OneLookahead`.

### Formule
```
score(item) = myDensity(item) + γ × denial(item) + δ × oppDensity(item)
```

Où :
- `myDensity(item)` = densité adaptive de mon point de vue (cf. `AdaptiveGreedyStrategy`)
- `denial(item)` = `oppGainBaseline - oppGainAfterMyPick` (cf. `OneLookaheadStrategy`)
- `oppDensity(item)` = densité adaptive de l'item du point de vue de l'adversaire (0 si l'item ne rentre pas dans son sac)

### Signature
```java
public class DenialLookaheadStrategy implements Strategy {
    public DenialLookaheadStrategy() { this(0.3, 0.3, 60); }
    public DenialLookaheadStrategy(double gamma, double delta, int candidateLimit) { ... }
}
```

### Risque connu
Le bonus `denial` et le bonus `oppDensity` sont corrélés (un item que l'adversaire veut donne du denial *et* de l'oppDensity). C'est précisément le test : est-ce que la version "soft" (oppDensity) ajoute de l'info utile par rapport au denial seul, ou est-ce du double comptage qui dégrade le score ?

### Inscription au benchmark
Ajouter dans `STRATEGIES` plusieurs configurations à tester :
- `DenialLookahead(γ=0.3, δ=0.3)`
- `DenialLookahead(γ=0.5, δ=0)` — équivalent à OneLookahead, sanity check
- `DenialLookahead(γ=0, δ=0.5)` — équivalent à DenialAdaptive, sanity check

---

## Phase 3 — Implémenter `EndgameAwareStrategy`

Hybride : AdaptiveGreedy en milieu de partie, puis bascule en DP knapsack quand la capacité restante devient faible.

### Logique
À chaque appel de `pickItem(game)` :

1. Calculer `remS = game.getRemainingSize()`, `remW = game.getRemainingWeight()`
2. Calculer `loadFraction = min(remS / sizeCapacity, remW / weightCapacity)`
3. Si `loadFraction >= ENDGAME_THRESHOLD` (défaut 0.2) → utiliser AdaptiveGreedy
4. Sinon → résoudre un knapsack DP 2D sur les items qui rentrent encore, prendre le **premier item** de la solution optimale

### DP knapsack 2D
```
dp[s][w] = meilleure valeur réalisable avec taille ≤ s et poids ≤ w
```
- Domaine : `s ∈ [0, remS]`, `w ∈ [0, remW]`
- Coût : `O(K × remS × remW)` où K = items qui rentrent
- En endgame (remS, remW < 200), `K` aussi typiquement < 100 → ~4M ops, largement sous 500 ms

### Garde-fous
- Timer hard à 400 ms dans le DP ; si dépassé, fallback sur AdaptiveGreedy
- Recompute du DP à chaque tour (l'adversaire peut avoir pris un item du plan)
- Reconstruire la solution (pas juste la valeur) pour savoir quel item prendre **maintenant**
- Critère de sélection si plusieurs items dans la solution : prendre celui de **plus grande valeur** d'abord (sécurise contre interruption par l'adversaire)

### Signature
```java
public class EndgameAwareStrategy implements Strategy {
    public EndgameAwareStrategy() { this(0.2); }
    public EndgameAwareStrategy(double endgameThreshold) { ... }
}
```

### Inscription au benchmark
- `EndgameAware(0.2)` — défaut
- `EndgameAware(0.3)` — bascule plus tôt
- `EndgameAware(0.15)` — bascule plus tard

---

## Phase 4 — Benchmark final + tuning

### 4.1 Run de référence
Lancer le benchmark complet (toutes stratégies, 500 rounds, params aléatoires dans les plages compétition). Archiver `benchmark_results.json` sous un nom daté.

### 4.2 Sweep des hyperparamètres
Pour la meilleure variante de chaque famille :
- DenialLookahead : sweep `γ ∈ {0.2, 0.3, 0.4, 0.5}` × `δ ∈ {0.2, 0.3, 0.4}`
- EndgameAware : sweep `threshold ∈ {0.15, 0.2, 0.25, 0.3}`

### 4.3 Combo
Tester `EndgameAwareDenialLookahead` : DenialLookahead en milieu de partie, DP knapsack en endgame.
Si gain significatif, c'est probablement la stratégie à soumettre.

### 4.4 Validation finale
Mettre la stratégie gagnante dans `Main.java`, lancer un match contre le moteur externe (`voleurs_de_tresors.jar`) avec les params extrêmes de la compétition (1000 items, 1000/1000 capacités) et vérifier :
- Aucun timeout (preprocess + tous les tours)
- Aucune faute
- Score absolu raisonnable

---

## Mettre à jour `STRATEGY_NOTES.md` au fur et à mesure

À chaque nouvelle stratégie testée, ajouter une section dans `STRATEGY_NOTES.md` avec :
- Formule
- Hyperparamètres testés
- Win rate vs pool
- Score moyen
- Observation qualitative (gagne contre quoi, perd contre quoi)

Garder la trace des configs abandonnées et **pourquoi** (comme `CompetitiveGreedy` et la V1 de `OneLookahead`).