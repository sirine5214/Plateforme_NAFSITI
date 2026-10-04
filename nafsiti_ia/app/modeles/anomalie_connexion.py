"""Module 1 — Comptes & sécurité : détection de connexions anormales (IsolationForest)."""
import joblib
import numpy as np
from sklearn.ensemble import IsolationForest

from app.config import DOSSIER_MODELES

FEATURES = ["heure_connexion", "nouvel_appareil", "pays_different",
            "echecs_24h", "distance_km_derniere_ip"]
CHEMIN = DOSSIER_MODELES / "login_anomaly.joblib"

SEUIL_BLOCAGE = 0.65
SEUIL_MFA = 0.55

_modele: IsolationForest | None = None


def historique_synthetique(n: int = 5000, graine: int = 42) -> np.ndarray:
    """Connexions « normales » simulées, utilisées tant qu'aucun historique réel n'est disponible."""
    rng = np.random.default_rng(graine)
    heures = np.clip(rng.normal(15, 4, n), 6, 23.99)               # surtout en journée / soirée
    nouvel_appareil = (rng.random(n) < 0.08).astype(float)         # rarement un nouvel appareil
    pays_different = (rng.random(n) < 0.01).astype(float)
    echecs = rng.choice([0, 0, 0, 0, 0, 0, 1, 1, 2], n).astype(float)
    distance = np.abs(rng.normal(0, 15, n))                        # quelques km au plus
    return np.column_stack([heures, nouvel_appareil, pays_different, echecs, distance])


def entrainer(historique: np.ndarray) -> IsolationForest:
    modele = IsolationForest(n_estimators=200, contamination=0.02, random_state=42)
    modele.fit(historique)
    joblib.dump(modele, CHEMIN)
    return modele


def charger():
    global _modele
    if not CHEMIN.exists():
        entrainer(historique_synthetique())
    _modele = joblib.load(CHEMIN)


def reentrainer(historique: list[list[float]]) -> int:
    """Ré-entraînement hebdomadaire sur les connexions réussies réelles (complétées si trop peu nombreuses)."""
    global _modele
    donnees = np.array(historique, dtype=float).reshape(-1, len(FEATURES))
    if len(donnees) < 500:
        donnees = np.vstack([donnees, historique_synthetique(500 - len(donnees))])
    _modele = entrainer(donnees)
    return len(donnees)


def evaluer_connexion(x: list[float]) -> dict:
    score = float(-_modele.score_samples([x])[0])   # plus élevé = plus anormal
    if score > SEUIL_BLOCAGE:
        action = "BLOQUER_ET_ALERTER"
    elif score > SEUIL_MFA:
        action = "MFA_RENFORCEE"
    else:
        action = "AUTORISER"
    return {"score_risque": round(score, 3), "action": action}
