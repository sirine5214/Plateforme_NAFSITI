"""Module 2 — Rendez-vous : matching patient ↔ thérapeute (embeddings + score pondéré)."""
from app.modeles.encodeur import obtenir_encodeur


def charger():
    obtenir_encodeur()


def classer_therapeutes(besoin: str, therapeutes: list[dict], langue: str, k: int = 5) -> list[dict]:
    if not therapeutes:
        return []
    from sentence_transformers import util

    encodeur = obtenir_encodeur()
    emb_besoin = encodeur.encode(besoin, convert_to_tensor=True)
    profils = [f"{t['specialites']} {t['approche']}".strip() or "psychologue" for t in therapeutes]
    emb_profils = encodeur.encode(profils, convert_to_tensor=True)
    similarites = util.cos_sim(emb_besoin, emb_profils)[0]

    resultats = []
    for t, sim in zip(therapeutes, similarites):
        langues = [l.strip().lower() for l in t["langues"].split(",")] if t["langues"] else []
        score = (0.6 * max(float(sim), 0.0)
                 + 0.2 * min(t["creneaux_7j"], 10) / 10
                 + 0.1 * t["note_moyenne"] / 5
                 + 0.1 * (1.0 if langue.lower() in langues else 0.0))
        resultats.append({"therapeute_id": t["id"], "score": round(score, 3),
                          "similarite": round(float(sim), 3)})
    return sorted(resultats, key=lambda x: x["score"], reverse=True)[:k]
