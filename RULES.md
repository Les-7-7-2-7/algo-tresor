# Voleurs de trésors

*Car le peu qu'ils avaient aperçu du trésor jusqu'à maintenant avait suffi à raviver toute la flamme qui brûle dans le cœur des nains ; et quand le cœur d'un nain, aussi noble soit-il, ressent l'appel de l'or et des joyaux, il s'enhardit tout à coup et il peut être redoutable.*

J.R.R. Tolkien, *Bilbo le Hobbit*


Dans ce jeu, les deux joueurs incarnent des nains qui s'aventurent dans la caverne d'un dragon afin de subtiliser une partie des trésors qui s'y trouvent. Le but est de ramener une collection de trésors de la plus grande valeur possible, tout en respectant des contraintes de poids et de taille. Tour à tour, les joueurs choisissent un trésor à ajouter à leur collection, jusqu'à ce que leur sac soit plein. Un trésor pris par un joueur n'est plus disponible pour l'autre.

## Les règles du jeu

Plus formellement, au début du jeu, on vous communiquera:
- une contrainte de taille S
- une contrainte de poids W
- un ensemble d'objets, chaque objet étant caractérisé par un identifiant, une taille, un poids et une valeur

À tour de rôle, chaque joueur choisira un objet et l'ajoutera à sa collection, avec les contraintes suivantes:
- l'objet ne doit pas avoir été choisi auparavant (ni par ce joueur ni par son adversaire)
- la somme des tailles des objets choisis par le joueur doit être inférieure ou égale à S
- la somme des poids des objets choisis par le joueur doit être inférieur ou égale à W

Chaque choix est final, il n'est pas possible d'abandonner des objets précédemment choisis pour faire de la place.

À la fin de la partie, le joueur qui a l'ensemble d'objets avec la plus grande valeur totale gagne la partie.

## Le protocole de jeu

Vous devez développer un programme qui lira des données sur l'entrée standard de la console, et transmettra ses choix en écrivant sur la sortie standard. Le protocole est le suivant:

1. les données du problème sont transmises simultanément aux deux joueurs
2. les joueurs disposent de 5000 millisecondes pour effectuer des calculs préliminaires
3. chacun à tour de rôle, les joueurs doivent choisir un objet disponible pour le mettre dans leur sac, en écrivant l'identifiant de cet objet sur la sortie. L'objet choisi est indiqué à l'autre joueur. Désormais cet objet n'est plus disponible et ne plus être choisi par aucun des deux joueurs. À chaque tour, les joueurs ont au maximum 500 millisecondes pour faire leur choix.
4. le jeu continue jusqu'à ce que chacun des deux joueurs ait fini

Il y a deux moyens pour un joueur de finir:
1. Au lieu de retourner un identifiant d'objet, un joueur peut retourner -1 pour indiquer qu'il ne veut plus (ou ne peut plus) prendre d'objets. Ce joueur ne pourra plus choisir d'objets jusqu'à la fin de la partie.
2. À chaque fois qu'un joueur échoue à choisir un objet (parce qu'il ne répond pas dans le temps imparti, qu'il retourne un identifiant incorrect, qu'il choisit un objet non disponible ou qui viole les contraintes de poids et de taille), ce joueur commet une faute. À la 3ème faute, le joueur est éliminé et ne pourra plus choisir d'autres objets jusqu'à la fin de la partie.

## Format des données

La transmission des données du problème se fait sur l'entrée standard, selon le format suivant:

```
n_items N
size_capacity S
weight_capacity W
0 S0 W0 V0
1 S1 W1 V1
...
```

Les trois premières lignes donnent respectivement le nombre total d'objets N, la contrainte de taille du sac S, ainsi que sa contrainte de poids W. Ensuite viennent N lignes qui décrivent les différents objets. Chaque objet est doté d'un identifiant I (de 0 à N-1, les objets sont transmis dans l'ordre des identifiants), d'une taille SI, d'un poids WI, et d'une valeur VI.

Voici un exemple de problème qui peut vous être transmis:
```
n_items 3
size_capacity 10
weight_capacity 5
0 2 2 20
1 3 4 30
2 5 3 40
```

Après la transmission des données, vous lirez la ligne suivante:
```
preprocessing 5000
```
qui indique le début du temps alloué pour les calculs préliminaire.

ensuite, à chaque tour, vous lirez les lignes suivantes:
```
taken ID
next_item 500
```
La première ligne donne l'identifiant de l'objet choisi par l'adversaire. Si l'adversaire n'a pas pris d'objets (parce que c'est le premier tour du premier joueur, que l'adversaire a fini, ou qu'il n'a pas fourni de réponse valide), l'identifiant sera -1.

Le deuxième ligne indique que vous disposez de 500 millisecondes pour faire votre choix. Une fois ce choix fait, vous devez écrire l'identifiant de l'objet choisi sur la sortie standard, ou bien -1 si vous ne voulez plus prendre d'objets.