"""Chargement unique des modèles au démarrage et état de chaque module."""
import logging
import time

from fastapi import HTTPException

from app.config import MODULES_ACTIFS
from app.modeles import (anomalie_connexion, chatbot, matching, moderation, recommandation, risque, sentiment,
                         visage)

log = logging.getLogger("nafsiti.ia")

# L'ordre compte : la modération (5) et le chatbot s'appuient sur le moteur de risque (6).
CHARGEURS = {
    "connexion": anomalie_connexion.charger,
    "risque": risque.charger,
    "matching": matching.charger,
    "recommandation": recommandation.charger,
    "sentiment": sentiment.charger,
    "moderation": moderation.charger,
    "visage": visage.charger,
    "chatbot": chatbot.charger,
}

etat: dict[str, str] = {}


def charger_tout():
    for nom, chargeur in CHARGEURS.items():
        if nom not in MODULES_ACTIFS:
            etat[nom] = "desactive"
            continue
        if nom in ("moderation", "chatbot") and etat.get("risque") != "pret":
            etat[nom] = "erreur : le module risque est requis"
            continue
        debut = time.perf_counter()
        try:
            chargeur()
            etat[nom] = "pret"
            log.info("Module %s chargé en %.1f s", nom, time.perf_counter() - debut)
        except Exception as e:  # un module en échec ne doit pas empêcher les autres de servir
            etat[nom] = f"erreur : {e}"
            log.exception("Échec du chargement du module %s", nom)


def exiger(nom: str):
    """Dépendance FastAPI : 503 si le modèle n'est pas disponible (le backend bascule en mode dégradé)."""
    def _verifier():
        if etat.get(nom) != "pret":
            raise HTTPException(status_code=503, detail=f"Module IA « {nom} » indisponible")
    return _verifier
