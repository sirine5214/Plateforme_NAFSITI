"""Module 6 — Détection de risque : lexique critique (règles) + TF-IDF / régression logistique."""
import re
import unicodedata

import joblib

from app.config import DOSSIER_MODELES

CHEMIN = DOSSIER_MODELES / "risque_tfidf_logreg.joblib"

LEXIQUE_CRITIQUE = [r"\bme suicider\b", r"\ben finir\b", r"\bplus envie de vivre\b",
                    r"\bme faire du mal\b", r"\bmettre fin a mes jours\b", r"\bme tuer\b",
                    r"\bsuicide\b", r"\bme scarifier\b"]
LEXIQUE_DETRESSE = [r"\bdesespoir\b", r"\bsans issue\b", r"\bje n'en peux plus\b",
                    r"\bpersonne ne m'aime\b", r"\bdisparaitre\b", r"\bje ne sers a rien\b",
                    r"\bvide a l'interieur\b"]

RECOMMANDATIONS = {
    3: ["urgences_3114", "contact_therapeute_immediat"],
    2: ["respiration_guidee", "prise_rdv_prioritaire"],
    1: ["meditation_courte", "article_gestion_stress"],
    0: [],
}

_classifieur = None


def charger():
    global _classifieur
    if not CHEMIN.exists():
        from entrainement.entrainer_risque import entrainer

        entrainer()
    _classifieur = joblib.load(CHEMIN)


def normaliser(texte: str) -> str:
    t = unicodedata.normalize("NFKD", texte.lower().replace("’", "'"))
    return "".join(c for c in t if not unicodedata.combining(c))


def detecter_risque(texte: str) -> dict:
    t = normaliser(texte)
    if any(re.search(motif, t) for motif in LEXIQUE_CRITIQUE):
        return {"niveau": 3, "probabilite": None, "source": "regle",
                "recommandations": RECOMMANDATIONS[3]}
    proba = float(_classifieur.predict_proba([t])[0][1])
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
