import hmac

from fastapi import Header, HTTPException

from app.config import API_KEY


def verifier_cle(x_api_key: str | None = Header(default=None)):
    """Seul le backend Spring Boot connaît la clé : le service n'est pas exposé au navigateur."""
    if x_api_key is None or not hmac.compare_digest(x_api_key, API_KEY):
        raise HTTPException(status_code=401, detail="Clé invalide")
