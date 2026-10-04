"""(Ré)entraîne le détecteur de connexions anormales (Module 1) sur l'historique synthétique.

En production, le backend envoie chaque semaine les connexions réussies réelles
sur POST /v1/connexion/entrainer.

Usage : python -m entrainement.entrainer_anomalie
"""
from app.modeles.anomalie_connexion import CHEMIN, entrainer, evaluer_connexion, historique_synthetique, charger

if __name__ == "__main__":
    entrainer(historique_synthetique())
    charger()
    print(f"Modèle enregistré : {CHEMIN}")
    print("Connexion habituelle (15 h, appareil connu)  :", evaluer_connexion([15, 0, 0, 0, 2]))
    print("Connexion inhabituelle (3 h, nouvel appareil) :", evaluer_connexion([3, 1, 0, 4, 0]))
    print("Connexion très suspecte                        :", evaluer_connexion([3, 1, 1, 9, 2500]))
