"""Module 6 — Détection de risque : lexique critique (règles) + classifieur TF-IDF / embeddings / régression logistique.

Les embeddings multilingues (le même encodeur que les modules 2 et 4) complètent le TF-IDF :
le TF-IDF capte les expressions exactes, les embeddings le sens des phrases formulées avec
des mots jamais vus à l'entraînement (ce qui réduit fortement les fausses alertes).
"""
import re
import unicodedata

import joblib
import numpy as np
from scipy.sparse import csr_matrix, hstack
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression

from app.config import DOSSIER_MODELES
from app.modeles.encodeur import obtenir_encodeur

CHEMIN = DOSSIER_MODELES / "risque_tfidf_embeddings_logreg.joblib"

LEXIQUE_CRITIQUE = [r"\bme suicider\b", r"\ben finir\b", r"\bplus envie de vivre\b",
                    r"\bme faire du mal\b", r"\bmettre fin a mes jours\b", r"\bme tuer\b",
                    r"\bsuicide\b", r"\bme scarifier\b", r"\braison de vivre\b", r"\benvie de mourir\b",
                    r"\bne plus me reveiller\b", r"\blettre d'adieu\b", r"\bveux mourir\b"]
LEXIQUE_DETRESSE = [r"\bdesespoir\b", r"\bsans issue\b", r"\bje n'en peux plus\b",
                    r"\bpersonne ne m'aime\b", r"\bdisparaitre\b", r"\bje ne sers a rien\b",
                    r"\bvide a l'interieur\b"]

RECOMMANDATIONS = {
    3: ["urgences_3114", "contact_therapeute_immediat"],
    2: ["respiration_guidee", "prise_rdv_prioritaire"],
    1: ["meditation_courte", "article_gestion_stress"],
    0: [],
}

_modele: dict | None = None


def normaliser(texte: str) -> str:
    t = unicodedata.normalize("NFKD", texte.lower().replace("’", "'"))
    return "".join(c for c in t if not unicodedata.combining(c))


def _caracteristiques(vectoriseur: TfidfVectorizer, textes_normalises: list[str]):
    emb = obtenir_encodeur().encode(textes_normalises, normalize_embeddings=True, show_progress_bar=False)
    return hstack([vectoriseur.transform(textes_normalises), csr_matrix(np.asarray(emb))]).tocsr()


def ajuster(textes_normalises: list[str], labels: list[int]) -> dict:
    """Entraîne le classifieur (utilisé par entrainement/entrainer_risque.py)."""
    vectoriseur = TfidfVectorizer(ngram_range=(1, 2), sublinear_tf=True).fit(textes_normalises)
    classifieur = LogisticRegression(class_weight="balanced", C=3, max_iter=3000)
    classifieur.fit(_caracteristiques(vectoriseur, textes_normalises), labels)
    return {"vectoriseur": vectoriseur, "classifieur": classifieur, "version": 2}


def probabilites(modele: dict, textes_normalises: list[str]) -> np.ndarray:
    return modele["classifieur"].predict_proba(_caracteristiques(modele["vectoriseur"], textes_normalises))[:, 1]


def charger():
    global _modele
    if not CHEMIN.exists():
        from entrainement.entrainer_risque import entrainer

        entrainer()
    _modele = joblib.load(CHEMIN)


def detecter_risque(texte: str) -> dict:
    t = normaliser(texte)
    if any(re.search(motif, t) for motif in LEXIQUE_CRITIQUE):
        return {"niveau": 3, "probabilite": None, "source": "regle",
                "recommandations": RECOMMANDATIONS[3]}
    proba = float(probabilites(_modele, [t])[0])
    detresse = any(re.search(motif, t) for motif in LEXIQUE_DETRESSE)
    # Seuils volontairement bas : on préfère un faux positif à un signal de crise manqué.
    if proba >= 0.80:
        niveau = 3
    elif proba >= 0.40 or detresse:
        niveau = 2
    elif proba >= 0.20:
        niveau = 1
    else:
        niveau = 0
    return {"niveau": niveau, "probabilite": round(proba, 3), "source": "modele",
            "recommandations": RECOMMANDATIONS[niveau]}
