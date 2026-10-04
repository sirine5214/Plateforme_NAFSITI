"""Encodeur d'embeddings multilingue partagé par les modules 2 (matching) et 4 (recommandation)."""
from app.config import MODELE_EMBEDDINGS

_encodeur = None


def obtenir_encodeur():
    """Chargé une seule fois, quel que soit le nombre de modules qui l'utilisent."""
    global _encodeur
    if _encodeur is None:
        from sentence_transformers import SentenceTransformer

        _encodeur = SentenceTransformer(MODELE_EMBEDDINGS)
    return _encodeur
