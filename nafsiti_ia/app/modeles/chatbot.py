"""Chatbot d'orientation : oriente vers la bonne fonctionnalité ou le bon interlocuteur, sans jamais diagnostiquer.

1. Sécurité d'abord : chaque message passe par le moteur de risque (module 6). Une crise donne
   toujours la même réponse, avec les numéros d'urgence, quelle que soit la question posée.
2. Sinon, l'intention est reconnue par similarité sémantique (encodeur multilingue partagé)
   avec des phrases d'exemple ; en dessous d'un seuil de confiance, le chatbot le dit et
   propose de parler à un professionnel plutôt que de deviner.
"""
import numpy as np

from app.modeles import risque
from app.modeles.encodeur import obtenir_encodeur

SEUIL_CONFIANCE = 0.45

RDV = {"libelle": "Prendre rendez-vous", "lien": "/rendez-vous/prendre"}
MESSAGERIE = {"libelle": "Écrire à mon thérapeute", "lien": "/messagerie"}
RESSOURCES = {"libelle": "Ressources bien-être", "lien": "/ressources"}
JOURNAL = {"libelle": "Mon journal", "lien": "/journal"}
PROFIL = {"libelle": "Mon profil", "lien": "/profil"}
APPEL_3114 = {"libelle": "Appeler le 3114 (24 h/24)", "lien": "tel:3114"}
APPEL_15 = {"libelle": "Appeler le 15 (SAMU)", "lien": "tel:15"}

REPONSE_INCOMPRISE_TEXTE = ("Je ne suis pas sûr d'avoir bien compris. Je peux vous orienter vers un rendez-vous, "
                            "des exercices de respiration ou votre journal. Pour une question personnelle, le mieux "
                            "est d'en parler à un thérapeute.")

INTENTIONS: dict[str, dict] = {
    "salutation": {
        "exemples": ["bonjour", "salut", "bonsoir", "coucou", "hello", "salam", "hi"],
        "reponse": "Bonjour ! Je suis l'assistant de Nafsiti. Je peux vous orienter : prendre rendez-vous, "
                   "trouver un exercice de respiration, écrire dans votre journal… Que puis-je faire pour vous ?",
        "actions": [RDV, RESSOURCES, JOURNAL],
    },
    "remerciement": {
        "exemples": ["merci", "merci beaucoup", "c'est gentil", "super merci", "parfait merci"],
        "reponse": "Avec plaisir. Prenez soin de vous, et n'hésitez pas à revenir si besoin.",
        "actions": [],
    },
    "prendre_rdv": {
        "exemples": ["je veux prendre rendez-vous", "comment réserver une séance", "je voudrais voir un psychologue",
                     "parler à un thérapeute", "consulter un professionnel", "trouver un psy",
                     "réserver une consultation", "je cherche un thérapeute qui parle arabe"],
        "reponse": "Vous pouvez réserver une séance avec un thérapeute. Décrivez votre besoin en quelques mots sur "
                   "la page de rendez-vous : l'IA vous suggère les professionnels les plus adaptés, et vous restez "
                   "libre de choisir.",
        "actions": [RDV],
    },
    "annuler_rdv": {
        "exemples": ["annuler mon rendez-vous", "déplacer ma séance", "changer l'heure de mon rendez-vous",
                     "je ne pourrai pas venir à ma séance"],
        "reponse": "Vous pouvez annuler un rendez-vous à venir depuis « Mes rendez-vous » ; le créneau est alors "
                   "libéré. Pour le déplacer, prévenez votre thérapeute par la messagerie puis réservez un autre créneau.",
        "actions": [{"libelle": "Mes rendez-vous", "lien": "/rendez-vous"}, MESSAGERIE],
    },
    "stress_anxiete": {
        "exemples": ["je suis stressé", "j'ai de l'anxiété", "je fais des crises d'angoisse", "je panique",
                     "j'ai le cœur qui s'emballe", "le travail me stresse", "je suis angoissé avant mes examens"],
        "reponse": "Le stress et l'anxiété sont fréquents et peuvent s'apaiser. Un exercice de respiration de quelques "
                   "minutes (respiration carrée, cohérence cardiaque) aide souvent sur le moment. Si cela dure ou "
                   "vous gêne au quotidien, un thérapeute peut vous accompagner.",
        "actions": [RESSOURCES, RDV],
    },
    "sommeil": {
        "exemples": ["je n'arrive pas à dormir", "insomnie", "je me réveille la nuit", "je dors mal",
                     "je fais des cauchemars", "je suis fatigué le matin"],
        "reponse": "Les troubles du sommeil ont souvent un lien avec le stress. La bibliothèque propose une respiration "
                   "4-7-8 pour l'endormissement et des conseils d'hygiène du sommeil. Noter vos nuits dans le journal "
                   "aide aussi à repérer ce qui les influence.",
        "actions": [RESSOURCES, JOURNAL],
    },
    "tristesse": {
        "exemples": ["je me sens triste", "j'ai le moral à zéro", "je me sens seul", "je n'ai plus de motivation",
                     "je déprime un peu", "je me sens nul"],
        "reponse": "Merci de le partager. Écrire ce que vous ressentez dans votre journal peut aider à y voir plus "
                   "clair, et la méditation de bienveillance envers soi est conçue pour ces moments. Si ce sentiment "
                   "s'installe, parlez-en à un professionnel.",
        "actions": [JOURNAL, RESSOURCES, RDV],
    },
    "respiration_meditation": {
        "exemples": ["un exercice de respiration", "je veux méditer", "comment me calmer", "un exercice de relaxation",
                     "méditation guidée", "comment me détendre"],
        "reponse": "La bibliothèque contient des exercices de respiration et des méditations guidées de 3 à 10 minutes, "
                   "validés par des professionnels. Vos recommandations personnalisées apparaissent en haut de la page.",
        "actions": [RESSOURCES],
    },
    "journal": {
        "exemples": ["comment fonctionne le journal", "noter mon humeur", "suivre mon humeur",
                     "voir l'évolution de mon humeur", "écrire dans mon journal"],
        "reponse": "Le journal vous permet de noter votre humeur de 1 à 10, vos émotions et quelques mots. Vos notes "
                   "sont chiffrées ; une courbe montre l'évolution sur 30 jours.",
        "actions": [JOURNAL],
    },
    "messagerie": {
        "exemples": ["comment contacter mon thérapeute", "envoyer un message", "écrire à mon psy",
                     "parler à mon thérapeute entre deux séances"],
        "reponse": "La messagerie sécurisée est ouverte avec les thérapeutes avec qui vous avez un rendez-vous. "
                   "Les messages sont chiffrés et arrivent en temps réel.",
        "actions": [MESSAGERIE],
    },
    "confidentialite": {
        "exemples": ["mes données sont-elles protégées", "qui peut lire mon journal", "confidentialité",
                     "supprimer mes données", "télécharger mes données", "rgpd", "mon thérapeute voit-il mon journal"],
        "reponse": "Votre journal et vos messages sont chiffrés. Personne ne lit votre journal : seuls des signaux de "
                   "détresse peuvent être transmis à vos thérapeutes, et uniquement si vous l'avez accepté. Vous "
                   "pouvez télécharger ou supprimer toutes vos données depuis votre profil.",
        "actions": [PROFIL],
    },
    "reconnaissance_faciale": {
        "exemples": ["connexion avec mon visage", "reconnaissance faciale", "activer la caméra pour me connecter",
                     "se connecter sans mot de passe"],
        "reponse": "Vous pouvez activer la connexion par reconnaissance faciale dans « Mon profil » (3 captures). "
                   "Seule une empreinte numérique chiffrée est conservée, jamais votre photo.",
        "actions": [PROFIL],
    },
    # Questions sans rapport : reconnues explicitement plutôt que rattachées au hasard à une intention
    "hors_sujet": {
        "exemples": ["quel temps fait-il", "la météo de demain", "une recette de cuisine", "qui a gagné le match",
                     "raconte-moi une blague", "quelle est la capitale de la France", "les résultats du football",
                     "que penses-tu de la politique", "traduis cette phrase en anglais"],
        "reponse": REPONSE_INCOMPRISE_TEXTE,
        "actions": [RDV, RESSOURCES, JOURNAL],
    },
    "presentation": {
        "exemples": ["qu'est-ce que nafsiti", "à quoi sert cette application", "que peux-tu faire", "aide",
                     "comment ça marche", "qui es-tu"],
        "reponse": "Nafsiti vous aide à prendre soin de votre santé mentale : journal d'humeur, exercices de respiration "
                   "et de méditation, rendez-vous et messagerie avec des thérapeutes. Je suis un assistant "
                   "d'orientation : je ne pose pas de diagnostic et ne remplace pas un professionnel.",
        "actions": [JOURNAL, RESSOURCES, RDV],
    },
}

REPONSE_CRISE = {
    "reponse": "Ce que vous traversez semble très difficile, et vous n'avez pas à rester seul·e avec ça. "
               "Appelez maintenant le 3114 (prévention du suicide, gratuit, 24 h/24) ou le 15. "
               "Si vous êtes en danger immédiat, appelez le 112.",
    "actions": [APPEL_3114, APPEL_15, MESSAGERIE],
}
REPONSE_DETRESSE = {
    "reponse": "Je suis désolé que vous traversiez ce moment. Vous méritez d'être écouté·e : un professionnel peut "
               "vous aider, et un exercice de respiration peut soulager sur l'instant. Si cela devient trop lourd, "
               "le 3114 répond 24 h/24.",
    "actions": [RDV, MESSAGERIE, RESSOURCES, APPEL_3114],
}
REPONSE_INCOMPRISE = {"reponse": REPONSE_INCOMPRISE_TEXTE, "actions": [RDV, RESSOURCES, JOURNAL]}

_noms: list[str] = []
_matrice: np.ndarray | None = None


def charger():
    global _noms, _matrice
    _noms = [nom for nom, i in INTENTIONS.items() for _ in i["exemples"]]
    exemples = [e for i in INTENTIONS.values() for e in i["exemples"]]
    _matrice = obtenir_encodeur().encode(exemples, normalize_embeddings=True, show_progress_bar=False)


def repondre(texte: str) -> dict:
    r = risque.detecter_risque(texte)
    if r["niveau"] >= 3:
        return {**REPONSE_CRISE, "intention": "crise", "niveau_risque": r["niveau"], "confiance": 1.0}
    if r["niveau"] == 2:
        return {**REPONSE_DETRESSE, "intention": "detresse", "niveau_risque": 2, "confiance": 1.0}

    emb = obtenir_encodeur().encode([texte], normalize_embeddings=True, show_progress_bar=False)[0]
    similarites = _matrice @ emb
    meilleur = int(np.argmax(similarites))
    confiance = float(similarites[meilleur])
    if confiance < SEUIL_CONFIANCE:
        return {**REPONSE_INCOMPRISE, "intention": "inconnue", "niveau_risque": r["niveau"],
                "confiance": round(confiance, 3)}
    nom = _noms[meilleur]
    intention = INTENTIONS[nom]
    return {"reponse": intention["reponse"], "actions": intention["actions"],
            "intention": "inconnue" if nom == "hors_sujet" else nom,
            "niveau_risque": r["niveau"], "confiance": round(confiance, 3)}
