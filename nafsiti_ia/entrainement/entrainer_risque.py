"""Entraîne le classifieur de détresse (Module 6) à partir de data/risque_dataset.csv.

Le jeu fourni est un jeu de DÉMONSTRATION : il doit être remplacé par un jeu annoté
et validé par des psychologues avant toute utilisation réelle.

Usage : python -m entrainement.entrainer_risque
"""
import csv

import joblib
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline

from app.config import DOSSIER_DONNEES
from app.modeles.risque import CHEMIN, normaliser


def charger_donnees():
    with open(DOSSIER_DONNEES / "risque_dataset.csv", encoding="utf-8") as f:
        lignes = list(csv.DictReader(f))
    return [normaliser(l["texte"]) for l in lignes], [int(l["detresse"]) for l in lignes]


def entrainer(afficher_rapport: bool = False) -> Pipeline:
    textes, labels = charger_donnees()
    pipeline = Pipeline([
        ("tfidf", TfidfVectorizer(ngram_range=(1, 2), sublinear_tf=True)),
        ("logreg", LogisticRegression(class_weight="balanced", max_iter=1000)),
    ])
    if afficher_rapport:
        x_train, x_test, y_train, y_test = train_test_split(
            textes, labels, test_size=0.25, stratify=labels, random_state=42)
        pipeline.fit(x_train, y_train)
        print(classification_report(y_test, pipeline.predict(x_test), target_names=["neutre", "detresse"]))
    pipeline.fit(textes, labels)
    joblib.dump(pipeline, CHEMIN)
    return pipeline


if __name__ == "__main__":
    entrainer(afficher_rapport=True)
    print(f"Modèle enregistré : {CHEMIN}")
