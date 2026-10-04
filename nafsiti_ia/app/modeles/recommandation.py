"""Module 4 — Bibliothèque de ressources : recommandation hybride par similarité sémantique."""
import numpy as np

from app.modeles.encodeur import obtenir_encodeur


def charger():
    obtenir_encodeur()


def profil_utilisateur(entrees_journal: list[str], contenus_aimes: list[str]):
    textes = entrees_journal[-10:] + contenus_aimes
    if not textes:
        return None                                  # démarrage à froid
    return obtenir_encodeur().encode(textes, normalize_embeddings=True).mean(axis=0)


def recommander(profil, catalogue: list[dict], deja_vus: set, k: int = 5) -> list[dict]:
    if not catalogue:
        return []
    if profil is None:
        tries = sorted(catalogue, key=lambda c: c["popularite"], reverse=True)
        return [{"contenu_id": c["id"], "score": None} for c in tries[:k]]
    emb = obtenir_encodeur().encode([c["titre"] + " " + c["description"] for c in catalogue],
                                    normalize_embeddings=True)
    scores = emb @ profil
    for i, c in enumerate(catalogue):
        if c["id"] in deja_vus:
            scores[i] *= 0.3                         # diversité
        if c.get("valide_par_pro"):
            scores[i] += 0.05                        # bonus contenu validé
    top = np.argsort(scores)[::-1][:k]
    return [{"contenu_id": catalogue[i]["id"], "score": round(float(scores[i]), 3)} for i in top]


def heure_rappel(heures_ouverture: list[int]) -> int:
    heures = [h for h in heures_ouverture if 0 <= h <= 23]
    if not heures:
        return 20
    return int(np.bincount(heures, minlength=24).argmax())
