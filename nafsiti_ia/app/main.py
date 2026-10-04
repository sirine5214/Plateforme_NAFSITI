"""Service IA Nafsiti (FastAPI) — API interne consommée uniquement par le backend Spring Boot.

Lancement : uvicorn app.main:app --port 8000
Documentation interactive : http://localhost:8000/docs
"""
import logging
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, HTTPException

from app import registre
from app.modeles import (anomalie_connexion, matching, moderation, recommandation, risque, sentiment,
                         visage)
from app.schemas import *  # noqa: F403 — contrat de l'API
from app.securite import verifier_cle

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s : %(message)s")


@asynccontextmanager
async def lifespan(_app: FastAPI):
    registre.charger_tout()      # tous les modèles sont chargés une seule fois
    yield


app = FastAPI(title="Nafsiti IA", version="1.0", lifespan=lifespan)

# Les endpoints sont synchrones (def) : FastAPI les exécute dans un pool de threads,
# ce qui évite de bloquer la boucle d'événements pendant l'inférence.


@app.get("/sante")
def sante():
    """Sans clé : utilisé par le backend pour savoir quels modules sont disponibles."""
    return {"statut": "ok", "modules": registre.etat}


# ===== Module 1 — Comptes & sécurité =====

@app.post("/v1/connexion/evaluer", response_model=ConnexionOut,
          dependencies=[Depends(verifier_cle), Depends(registre.exiger("connexion"))])
def evaluer_connexion(req: ConnexionIn):
    x = [req.heure_connexion, req.nouvel_appareil, req.pays_different, req.echecs_24h,
         req.distance_km_derniere_ip]
    return anomalie_connexion.evaluer_connexion(x)


@app.post("/v1/connexion/entrainer",
          dependencies=[Depends(verifier_cle), Depends(registre.exiger("connexion"))])
def entrainer_connexion(req: HistoriqueConnexionsIn):
    return {"echantillons": anomalie_connexion.reentrainer(req.historique)}


# ===== Module 2 — Rendez-vous =====

@app.post("/v1/therapeutes/classer", response_model=list[TherapeuteClasse],
          dependencies=[Depends(verifier_cle), Depends(registre.exiger("matching"))])
def classer_therapeutes(req: MatchingIn):
    return matching.classer_therapeutes(req.besoin, [t.model_dump() for t in req.therapeutes], req.langue)


# ===== Module 3 — Journal & humeur =====

@app.post("/v1/journal/valence", response_model=ValenceOut,
          dependencies=[Depends(verifier_cle), Depends(registre.exiger("sentiment"))])
def valence(req: TexteIn):
    return {"valence": sentiment.analyser_note(req.texte)}


@app.post("/v1/journal/tendance", response_model=TendanceOut, dependencies=[Depends(verifier_cle)])
def tendance(req: TendanceIn):
    return sentiment.tendance_humeur(req.humeurs)    # NumPy seul : toujours disponible


# ===== Module 4 — Ressources =====

@app.post("/v1/ressources/recommander", response_model=RecommandationOut,
          dependencies=[Depends(verifier_cle), Depends(registre.exiger("recommandation"))])
def recommander(req: RecommandationIn):
    profil = recommandation.profil_utilisateur(req.entrees_journal, req.contenus_aimes)
    contenus = recommandation.recommander(profil, [c.model_dump() for c in req.catalogue],
                                          set(req.deja_vus), req.k)
    return {"contenus": contenus, "heure_rappel": recommandation.heure_rappel(req.heures_ouverture),
            "personnalise": profil is not None}


# ===== Module 5 — Messagerie =====

@app.post("/v1/messages/moderer", response_model=ModerationOut,
          dependencies=[Depends(verifier_cle), Depends(registre.exiger("moderation"))])
def moderer(req: TexteIn):
    return moderation.moderer_message(req.texte)


# ===== Module 6 — Détection de risque =====

@app.post("/v1/risque", response_model=RisqueOut,
          dependencies=[Depends(verifier_cle), Depends(registre.exiger("risque"))])
def endpoint_risque(req: TexteIn):
    return risque.detecter_risque(req.texte)


# ===== Reconnaissance faciale =====

@app.post("/v1/visage/enroler", response_model=EnrolementOut,
          dependencies=[Depends(verifier_cle), Depends(registre.exiger("visage"))])
def enroler_visage(req: EnrolementIn):
    try:
        return visage.enroler(req.images)
    except visage.VisageErreur as e:
        raise HTTPException(status_code=422, detail=str(e))


@app.post("/v1/visage/verifier", response_model=VerificationOut,
          dependencies=[Depends(verifier_cle), Depends(registre.exiger("visage"))])
def verifier_visage(req: VerificationIn):
    try:
        return visage.verifier(req.image, req.reference)
    except visage.VisageErreur as e:
        raise HTTPException(status_code=422, detail=str(e))
