"""Contrat de l'API interne Spring Boot ↔ service IA. Les utilisateurs sont pseudonymisés (user_id opaque)."""
from pydantic import BaseModel, Field


class TexteIn(BaseModel):
    user_id: str                                   # identifiant pseudonymisé
    texte: str = Field(min_length=1, max_length=5000)


# ===== Module 1 =====
class ConnexionIn(BaseModel):
    heure_connexion: float = Field(ge=0, lt=24)
    nouvel_appareil: int = Field(ge=0, le=1)
    pays_different: int = Field(ge=0, le=1)
    echecs_24h: int = Field(ge=0)
    distance_km_derniere_ip: float = Field(ge=0)


class ConnexionOut(BaseModel):
    score_risque: float
    action: str                                    # AUTORISER | MFA_RENFORCEE | BLOQUER_ET_ALERTER


class HistoriqueConnexionsIn(BaseModel):
    historique: list[list[float]]


# ===== Module 2 =====
class TherapeuteIn(BaseModel):
    id: int
    specialites: str = ""
    approche: str = ""
    langues: str = ""                              # ex. "fr,ar,en"
    note_moyenne: float = Field(default=4.0, ge=0, le=5)
    creneaux_7j: int = Field(default=0, ge=0)


class MatchingIn(BaseModel):
    besoin: str = Field(min_length=1, max_length=1000)
    langue: str = "fr"
    therapeutes: list[TherapeuteIn]


class TherapeuteClasse(BaseModel):
    therapeute_id: int
    score: float
    similarite: float


# ===== Module 3 =====
class ValenceOut(BaseModel):
    valence: float                                 # 1 (très négatif) → 5 (très positif)


class TendanceIn(BaseModel):
    humeurs: list[float] = Field(max_length=60)


class TendanceOut(BaseModel):
    statut: str
    pente: float | None = None
    moyenne_7j: float | None = None
    moyenne_30j: float | None = None
    baisse_significative: bool = False


# ===== Module 4 =====
class ContenuIn(BaseModel):
    id: int
    titre: str
    description: str = ""
    popularite: int = 0
    valide_par_pro: bool = False


class RecommandationIn(BaseModel):
    user_id: str
    entrees_journal: list[str] = []
    contenus_aimes: list[str] = []
    catalogue: list[ContenuIn]
    deja_vus: list[int] = []
    heures_ouverture: list[int] = []
    k: int = Field(default=5, ge=1, le=20)


class ContenuRecommande(BaseModel):
    contenu_id: int
    score: float | None


class RecommandationOut(BaseModel):
    contenus: list[ContenuRecommande]
    heure_rappel: int
    personnalise: bool


# ===== Module 5 =====
class ModerationOut(BaseModel):
    toxicite: float
    niveau_risque: int
    decision: str                                  # PUBLIER | MASQUER_EN_ATTENTE_REVUE | BLOQUER | ESCALADE_HUMAINE


# ===== Module 6 =====
class RisqueOut(BaseModel):
    niveau: int                                    # 0 → 3
    probabilite: float | None
    source: str                                    # regle | modele
    recommandations: list[str]


# ===== Reconnaissance faciale =====
class EnrolementIn(BaseModel):
    user_id: str
    images: list[str] = Field(min_length=1, max_length=5)   # images base64 (JPEG/PNG)


class EnrolementOut(BaseModel):
    empreinte: list[float]
    captures: int
    modele: str


class VerificationIn(BaseModel):
    user_id: str
    image: str
    reference: list[float]


class VerificationOut(BaseModel):
    correspond: bool
    distance: float
    seuil: float
