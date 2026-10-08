"""Entraîne le classifieur de détresse (Module 6) à partir de data/risque_dataset.csv.

Le jeu fourni est un jeu de DÉMONSTRATION : il doit être remplacé par un jeu annoté
et validé par des psychologues avant toute utilisation réelle.

Usage : python -m entrainement.entrainer_risque
"""
import csv

import joblib
import numpy as np
from sklearn.metrics import classification_report
from sklearn.model_selection import StratifiedKFold

from app.config import DOSSIER_DONNEES
from app.modeles.risque import CHEMIN, ajuster, normaliser, probabilites

SEUIL_ALERTE = 0.40


def charger_donnees():
    with open(DOSSIER_DONNEES / "risque_dataset.csv", encoding="utf-8") as f:
        lignes = list(csv.DictReader(f))
    return [normaliser(l["texte"]) for l in lignes], [int(l["detresse"]) for l in lignes]


def validation_croisee(textes: list[str], labels: list[int], plis: int = 5) -> np.ndarray:
    """Probabilités hors échantillon : chaque phrase est prédite par un modèle qui ne l'a pas vue."""
    y = np.array(labels)
    p = np.zeros(len(y))
    for tr, te in StratifiedKFold(plis, shuffle=True, random_state=42).split(textes, y):
        modele = ajuster([textes[i] for i in tr], y[tr].tolist())
        p[te] = probabilites(modele, [textes[i] for i in te])
    return p


def entrainer(afficher_rapport: bool = False) -> dict:
    textes, labels = charger_donnees()
    if afficher_rapport:
        p = validation_croisee(textes, labels)
        y = np.array(labels)
        print(f"Validation croisée (5 plis), seuil d'alerte {SEUIL_ALERTE} :")
        print(classification_report(y, (p >= SEUIL_ALERTE).astype(int), target_names=["neutre", "detresse"]))
    modele = ajuster(textes, labels)
    joblib.dump(modele, CHEMIN)
    return modele


if __name__ == "__main__":
    entrainer(afficher_rapport=True)
    print(f"Modèle enregistré : {CHEMIN}")
