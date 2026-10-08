# Jeu de test de la reconnaissance faciale

Ce dossier est vide par défaut : les photos de visages sont des données biométriques, elles ne sont **jamais versionnées** (voir `.gitignore`).

Pour évaluer la reconnaissance faciale, crée un sous-dossier par personne (avec son accord) et places-y au moins 4 photos de face :

```
visages/
├── personne_a/
│   ├── 1.jpg
│   ├── 2.jpg
│   ├── 3.jpg
│   └── 4.jpg
└── personne_b/
    ├── 1.jpg
    └── ...
```

Le script `python -m entrainement.evaluer --modules visage` :

1. enregistre chaque personne avec ses 3 premières photos (comme dans l'application) ;
2. vérifie les photos suivantes de la même personne : elles doivent être **acceptées** (taux de vrais positifs) ;
3. vérifie les photos des autres personnes : elles doivent être **refusées** (taux de vrais négatifs).

Deux personnes au minimum sont nécessaires pour mesurer les refus.

## Paires étiquetées

Un dossier contenant un fichier `master.csv` (`file_x,file_y,Decision` avec `Yes` = même personne, `No` = personnes
différentes) est évalué paire par paire. C'est le format du jeu de test de DeepFace
(github.com/serengil/deepface, `tests/unit/dataset`, licence MIT), qui a servi à calibrer le seuil `FACE_SEUIL=0.35`.
