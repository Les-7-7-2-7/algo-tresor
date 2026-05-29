# Tâche — Implémenter `CompetitiveGreedyStrategy`

Lis `Claude.md` pour comprendre comment créer une stratégie, et explore la base de code dans `src/` si besoin.

---

## Ce que tu dois faire

Crée la classe `CompetitiveGreedyStrategy` dans :
```
src/main/java/dev/bastienluben/algotresor/strategy/CompetitiveGreedyStrategy.java
```

Puis branche-la dans `Main.java` à la place de `PassStrategy`.

---

## Logique de `pickItem`

Pour chaque objet disponible qui rentre dans le sac, calculer :

```
score(item) = densité_propre(item) + α × densité_propre(item)
            = (1 + α) × cost / (size + weight)
```

où `α = 0.5` est un paramètre qui représente la valeur de priver l'adversaire de cet objet (on suppose que l'adversaire utilise la même heuristique de densité).

Retourner l'id de l'objet avec le meilleur score, ou `-1` si aucun objet ne rentre.

> Note : `preprocess` peut rester vide pour l'instant.