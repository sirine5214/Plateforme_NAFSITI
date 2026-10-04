# Nafsiti IA — service Python (FastAPI)

Service interne qui héberge les modèles d'intelligence artificielle de Nafsiti. Il est appelé **uniquement par le backend Spring Boot** (clé d'API `X-API-Key`), jamais directement par le navigateur.

| Module | Modèle | Bibliothèque | Endpoint |
|---|---|---|---|
| 1 — Comptes & sécurité | IsolationForest : connexions anormales | scikit-learn | `POST /v1/connexion/evaluer`, `POST /v1/connexion/entrainer` |
| 1 — Reconnaissance faciale | FaceNet-512 (+ anti-usurpation Fasnet) | TensorFlow, DeepFace | `POST /v1/visage/enroler`, `POST /v1/visage/verifier` |
| 2 — Rendez-vous | Embeddings multilingues + score pondéré | sentence-transformers | `POST /v1/therapeutes/classer` |
| 3 — Journal & humeur | DistilCamemBERT sentiment + régression de tendance | transformers, NumPy | `POST /v1/journal/valence`, `POST /v1/journal/tendance` |
| 4 — Ressources | Recommandation hybride par similarité sémantique | sentence-transformers, NumPy | `POST /v1/ressources/recommander` |
| 5 — Messagerie | Detoxify multilingue + escalade de crise | detoxify (PyTorch) | `POST /v1/messages/moderer` |
| 6 — Détection de risque | Règles lexicales + TF-IDF / régression logistique | scikit-learn, re | `POST /v1/risque` |

`GET /sante` (sans clé) indique l'état de chaque module. La documentation interactive est sur `http://localhost:8000/docs`.

## Installation

Python **3.10 à 3.12** recommandé (TensorFlow ne supporte pas toujours la dernière version de Python).

```bash
cd nafsiti_ia
python -m venv .venv
.venv\Scripts\activate          # Windows  (Linux/macOS : source .venv/bin/activate)
pip install -r requirements.txt
copy .env.example .env          # puis changer NAFSITI_IA_API_KEY (même valeur que app.ia.api-key côté Spring)
uvicorn app.main:app --port 8000
```

Au premier démarrage, les poids des modèles pré-entraînés sont téléchargés (Hugging Face, DeepFace : environ 2 Go au total), puis mis en cache. Les deux modèles scikit-learn sont entraînés automatiquement s'ils n'existent pas dans `models/`.

Sur une machine peu puissante, on peut ne charger qu'une partie des modules avec `NAFSITI_IA_MODULES` (voir `.env.example`). Un module absent répond `503` et le backend bascule en mode dégradé : par exemple, le journal est enregistré sans analyse et la connexion par mot de passe reste possible.

## Reconnaissance faciale

- **Enregistrement** : le profil Angular prend 3 captures webcam. Le service calcule une empreinte FaceNet-512 pour chacune, vérifie qu'elles montrent la même personne et renvoie leur moyenne normalisée. Le backend la stocke **chiffrée (AES-256-GCM)**. Aucune image n'est conservée.
- **Connexion** : l'email désigne le compte, puis une capture est comparée à l'empreinte enregistrée (vérification 1:1, distance cosinus ≤ `FACE_SEUIL`).
- **Anti-usurpation** : `FACE_ANTI_SPOOFING=true` refuse une photo ou un écran présenté à la caméra.
- **MFA** : si le module 1 juge une connexion par mot de passe inhabituelle (`MFA_RENFORCEE`), le backend exige une vérification faciale avant de délivrer le JWT.

## Ré-entraînement

```bash
python -m entrainement.entrainer_risque      # module 6 : affiche le rapport précision / rappel
python -m entrainement.entrainer_anomalie    # module 1 : sur l'historique synthétique
```

Le détecteur d'anomalies est aussi ré-entraîné chaque lundi à 3 h par le backend, sur les connexions réussies réelles.

> ⚠️ `data/risque_dataset.csv` est un **jeu de démonstration**. Avant toute utilisation réelle, il doit être remplacé par un jeu annoté et validé par des psychologues, avec un suivi du rappel (objectif ≥ 95 % sur les signaux de crise).

## Garde-fous

- Pas de diagnostic : les sorties orientent, priorisent ou alertent. La décision sensible reste humaine (alertes traitées par un thérapeute ou un administrateur).
- Données pseudonymisées : le backend n'envoie qu'un identifiant opaque (HMAC), jamais de nom ni d'email. Aucun texte n'est écrit dans les journaux du service.
- Modèles versionnés : identifiants Hugging Face figés dans `app/config.py`, fichiers `.joblib` dans `models/`.
