"""Module 5 — Messagerie temps réel : modération (Detoxify multilingue) et escalade de crise."""
from app.modeles import risque

SEUIL_BLOCAGE = 0.80
SEUIL_REVUE = 0.50

_moderateur = None


def charger():
    global _moderateur
    from detoxify import Detoxify

    _moderateur = Detoxify("multilingual")


def moderer_message(texte: str) -> dict:
    tox = float(_moderateur.predict(texte)["toxicity"])
    resultat_risque = risque.detecter_risque(texte)      # moteur du Module 6
    if resultat_risque["niveau"] >= 2:
        decision = "ESCALADE_HUMAINE"
    elif tox >= SEUIL_BLOCAGE:
        decision = "BLOQUER"
    elif tox >= SEUIL_REVUE:
        decision = "MASQUER_EN_ATTENTE_REVUE"
    else:
        decision = "PUBLIER"
    return {"toxicite": round(tox, 3), "niveau_risque": resultat_risque["niveau"],
            "decision": decision}
