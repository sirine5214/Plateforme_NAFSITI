"""Configuration du service IA, lue depuis les variables d'environnement (ou le fichier .env)."""
import os
from pathlib import Path

from dotenv import load_dotenv

RACINE = Path(__file__).resolve().parent.parent
load_dotenv(RACINE / ".env")

DOSSIER_MODELES = RACINE / "models"
DOSSIER_DONNEES = RACINE / "data"
DOSSIER_MODELES.mkdir(exist_ok=True)

API_KEY = os.getenv("NAFSITI_IA_API_KEY", "dev-nafsiti-ia-cle-a-changer")

TOUS_LES_MODULES = ["connexion", "matching", "sentiment", "recommandation", "moderation", "risque", "visage",
                    "chatbot"]
MODULES_ACTIFS = [
    m.strip() for m in os.getenv("NAFSITI_IA_MODULES", ",".join(TOUS_LES_MODULES)).split(",") if m.strip()
]

# Identifiants Hugging Face figés (versionnement des modèles)
MODELE_EMBEDDINGS = "paraphrase-multilingual-MiniLM-L12-v2"
# XLM-RoBERTa entraîné sur des messages courts (3 classes, dont neutre) : plus fiable sur les textes de
# journal que DistilCamemBERT (entraîné sur des avis clients) — voir data/evaluation/rapport.md.
MODELE_SENTIMENT = "cardiffnlp/twitter-xlm-roberta-base-sentiment"

FACE_MODELE = os.getenv("FACE_MODELE", "Facenet512")
FACE_DETECTEUR = os.getenv("FACE_DETECTEUR", "opencv")
FACE_SEUIL = float(os.getenv("FACE_SEUIL", "0.35"))
FACE_ANTI_SPOOFING = os.getenv("FACE_ANTI_SPOOFING", "true").lower() == "true"
