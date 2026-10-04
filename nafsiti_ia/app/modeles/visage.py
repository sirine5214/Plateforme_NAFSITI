"""Reconnaissance faciale (TensorFlow) : FaceNet-512 via DeepFace.

Le service reste sans état : il calcule des empreintes (vecteurs de 512 réels normalisés)
et compare une image à une empreinte de référence. Les empreintes sont stockées, chiffrées,
par le backend Spring Boot ; aucune image n'est conservée.
"""
import base64
import binascii

import numpy as np

from app.config import FACE_ANTI_SPOOFING, FACE_DETECTEUR, FACE_MODELE, FACE_SEUIL


class VisageErreur(Exception):
    """Erreur métier renvoyée au backend (aucun visage, plusieurs visages, photo d'écran…)."""


def charger():
    from deepface import DeepFace

    DeepFace.build_model(FACE_MODELE)   # télécharge et met en cache les poids au premier démarrage


def decoder_image(image_b64: str) -> np.ndarray:
    import cv2

    if "," in image_b64 and image_b64.startswith("data:"):
        image_b64 = image_b64.split(",", 1)[1]                  # data:image/jpeg;base64,....
    try:
        octets = base64.b64decode(image_b64, validate=True)
    except (binascii.Error, ValueError):
        raise VisageErreur("Image invalide")
    image = cv2.imdecode(np.frombuffer(octets, dtype=np.uint8), cv2.IMREAD_COLOR)
    if image is None:
        raise VisageErreur("Image illisible")
    return image                                                 # BGR, comme attendu par DeepFace


def _normaliser(v: np.ndarray) -> np.ndarray:
    return v / (np.linalg.norm(v) + 1e-10)


def extraire_empreinte(image_b64: str) -> np.ndarray:
    from deepface import DeepFace

    image = decoder_image(image_b64)
    try:
        representations = DeepFace.represent(
            img_path=image,
            model_name=FACE_MODELE,
            detector_backend=FACE_DETECTEUR,
            enforce_detection=True,
            align=True,
            anti_spoofing=FACE_ANTI_SPOOFING,
        )
    except ValueError as e:
        if "spoof" in str(e).lower():
            raise VisageErreur("Visage non authentique détecté : présentez votre visage, pas une photo ou un écran")
        raise VisageErreur("Aucun visage détecté : placez-vous face à la caméra, dans un endroit éclairé")
    if len(representations) != 1:
        raise VisageErreur("Un seul visage doit être visible à l'image")
    return _normaliser(np.array(representations[0]["embedding"], dtype=float))


def distance_cosinus(a: np.ndarray, b: np.ndarray) -> float:
    return float(1.0 - np.dot(_normaliser(a), _normaliser(b)))


def enroler(images_b64: list[str]) -> dict:
    """Empreinte de référence = moyenne des captures, qui doivent toutes montrer la même personne."""
    empreintes = [extraire_empreinte(img) for img in images_b64]
    moyenne = _normaliser(np.mean(empreintes, axis=0))
    ecart_max = max(distance_cosinus(e, moyenne) for e in empreintes)
    if ecart_max > FACE_SEUIL:
        raise VisageErreur("Les captures ne semblent pas montrer la même personne, recommencez")
    return {"empreinte": [round(float(x), 6) for x in moyenne], "captures": len(empreintes),
            "modele": FACE_MODELE}


def verifier(image_b64: str, reference: list[float]) -> dict:
    ref = np.array(reference, dtype=float)
    empreinte = extraire_empreinte(image_b64)
    if ref.shape != empreinte.shape:
        raise VisageErreur("Empreinte de référence incompatible : réenregistrez votre visage")
    distance = distance_cosinus(empreinte, ref)
    return {"correspond": distance <= FACE_SEUIL, "distance": round(distance, 4), "seuil": FACE_SEUIL}
