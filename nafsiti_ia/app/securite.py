import hmac

from fastapi import Header, HTTPException

from app.config import API_KEY


def verifier_cle(x_api_key: str = Header(...)):
    """Seul le backend Spring Boot connaît la clé : le service n'est pas exposé au navigateur."""
    if not hmac.compare_digest(x_api_key, API_KEY):
        raise HTTPException(status_code=401, detail="Clé invalide")
