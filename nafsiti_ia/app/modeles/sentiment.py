"""Module 3 — Journal & humeur : valence émotionnelle (DistilCamemBERT) et tendance (régression linéaire)."""
import numpy as np

from app.config import MODELE_SENTIMENT

_sentiment = None


def charger():
    global _sentiment
    from transformers import pipeline

    _sentiment = pipeline("text-classification", model=MODELE_SENTIMENT, top_k=None)


def analyser_note(texte: str) -> float:
    """Valence de 1 (très négatif) à 5 (très positif) : espérance sur les 5 classes « 1 star » … « 5 stars »."""
    scores = _sentiment([texte], truncation=True)[0]
    return round(sum(int(s["label"][0]) * s["score"] for s in scores), 2)


def tendance_humeur(humeurs_30j: list[float]) -> dict:
    y = np.array(humeurs_30j, dtype=float)
    if len(y) < 7:
        return {"statut": "historique_insuffisant"}
    pente = float(np.polyfit(np.arange(len(y)), y, 1)[0])
    moy7, moy30 = float(y[-7:].mean()), float(y.mean())
    baisse = moy7 < moy30 - 1.5 or pente < -0.1
    if pente > 0.05:
        statut = "amelioration"
    elif pente < -0.05:
        statut = "degradation"
    else:
        statut = "stable"
    return {"statut": statut, "pente": round(pente, 3), "moyenne_7j": round(moy7, 2),
            "moyenne_30j": round(moy30, 2), "baisse_significative": bool(baisse)}
