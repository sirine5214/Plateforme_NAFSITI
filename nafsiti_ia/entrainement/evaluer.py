"""Évalue les modèles IA sur les jeux de test de data/evaluation (exemples jamais vus à l'entraînement).

Usage :
    python -m entrainement.evaluer                         # tous les modules
    python -m entrainement.evaluer --modules risque moderation

Les modèles sont chargés directement (le service FastAPI n'a pas besoin de tourner).
Un rapport Markdown est écrit dans data/evaluation/rapport.md.
"""
import argparse
import base64
import csv
import json
import time
from datetime import datetime
from pathlib import Path

from app.config import DOSSIER_DONNEES, FACE_ANTI_SPOOFING, FACE_MODELE, FACE_SEUIL

EVAL = DOSSIER_DONNEES / "evaluation"
MODULES = ["connexion", "matching", "sentiment", "recommandation", "moderation", "risque", "chatbot", "visage"]

rapport: list[str] = []


def section(titre: str):
    print(f"\n=== {titre} ===")
    rapport.append(f"\n## {titre}\n")


def ligne(texte: str):
    print(texte)
    rapport.append(texte)


def score(nom: str, bons: int, total: int, objectif: float | None = None) -> float:
    taux = bons / total if total else 0.0
    verdict = "" if objectif is None else (" ✅" if taux >= objectif else f" ⚠️ (objectif {objectif:.0%})")
    ligne(f"- **{nom}** : {bons}/{total} = {taux:.0%}{verdict}")
    return taux


def erreurs(liste: list[str]):
    if liste:
        ligne("\n| Cas en erreur | Obtenu |\n|---|---|")
        for e in liste:
            ligne(e)


def lire_csv(nom: str) -> list[dict]:
    with open(EVAL / nom, encoding="utf-8") as f:
        return list(csv.DictReader(f))


def lire_json(nom: str):
    return json.loads((EVAL / nom).read_text(encoding="utf-8"))


# ===== Module 1 =====
def evaluer_connexion():
    from app.modeles import anomalie_connexion as m
    m.charger()
    section("Module 1 — Détection de connexions anormales (IsolationForest)")
    cas, ko = lire_csv("m1_connexions.csv"), []
    for c in cas:
        x = [float(c[k]) for k in m.FEATURES]
        r = m.evaluer_connexion(x)
        if r["action"] != c["attendu"]:
            ko.append(f"| {c['description']} (attendu {c['attendu']}) | {r['action']} (score {r['score_risque']}) |")
    score("Bonne action (autoriser / MFA / bloquer)", len(cas) - len(ko), len(cas), 0.85)
    faux_blocages = sum("attendu AUTORISER" in e and "BLOQUER" in e for e in ko)
    ligne(f"- Connexions normales bloquées : {faux_blocages}")
    erreurs(ko)


# ===== Module 2 =====
def evaluer_matching():
    from app.modeles import matching
    matching.charger()
    section("Module 2 — Matching patient / thérapeute (embeddings)")
    donnees = lire_json("m2_matching.json")
    noms = {t["id"]: t["nom"] for t in donnees["therapeutes"]}
    top1 = top3 = 0
    ko = []
    for d in donnees["demandes"]:
        classement = [r["therapeute_id"] for r in matching.classer_therapeutes(d["besoin"], donnees["therapeutes"], d["langue"])]
        top1 += classement[0] == d["attendu"]
        top3 += d["attendu"] in classement[:3]
        if classement[0] != d["attendu"]:
            ko.append(f"| {d['besoin']} (attendu {noms[d['attendu']]}) | {noms[classement[0]]} |")
    n = len(donnees["demandes"])
    score("Bon thérapeute classé 1er", top1, n, 0.8)
    score("Bon thérapeute dans le top 3", top3, n, 0.95)
    erreurs(ko)


# ===== Module 3 =====
def evaluer_sentiment():
    from app.modeles import sentiment
    sentiment.charger()
    section("Module 3 — Valence du journal (XLM-RoBERTa sentiment) et tendance")
    cas, ko = lire_csv("m3_sentiment.csv"), []
    for c in cas:
        v = sentiment.analyser_note(c["texte"])
        obtenu = "positif" if v >= 3.5 else "negatif" if v <= 2.5 else "neutre"
        if obtenu != c["polarite"]:
            ko.append(f"| {c['texte']} (attendu {c['polarite']}) | {obtenu} (valence {v}) |")
    score("Polarité correcte (positif ≥ 3,5 / négatif ≤ 2,5)", len(cas) - len(ko), len(cas), 0.8)
    erreurs(ko)

    series, ko = lire_json("m3_tendances.json"), []
    for s in series:
        r = sentiment.tendance_humeur(s["humeurs"])
        if r["statut"] != s["statut"] or r.get("baisse_significative", False) != s["baisse_significative"]:
            ko.append(f"| {s['description']} | {r} |")
    score("Tendance et alerte de baisse correctes", len(series) - len(ko), len(series), 1.0)
    erreurs(ko)


# ===== Module 4 =====
def evaluer_recommandation():
    from app.modeles import recommandation
    recommandation.charger()
    section("Module 4 — Recommandation de ressources (similarité sémantique)")
    donnees = lire_json("m4_recommandation.json")
    titres = {c["id"]: c["titre"] for c in donnees["catalogue"]}
    ok, ko = 0, []
    for p in donnees["profils"]:
        profil = recommandation.profil_utilisateur(p["entrees_journal"], [])
        top3 = [r["contenu_id"] for r in recommandation.recommander(profil, donnees["catalogue"], set(), k=3)]
        if any(a in top3 for a in p["attendus_top3"]):
            ok += 1
        else:
            ko.append(f"| {p['description']} (attendu {', '.join(titres[a] for a in p['attendus_top3'])}) | "
                      f"{', '.join(titres[i] for i in top3)} |")
    score("Contenu pertinent dans les 3 recommandations", ok, len(donnees["profils"]), 0.8)
    erreurs(ko)
    froid = recommandation.recommander(None, donnees["catalogue"], set(), k=3)
    plus_populaire = max(donnees["catalogue"], key=lambda c: c["popularite"])["id"]
    score("Démarrage à froid : contenu le plus populaire en tête", int(froid[0]["contenu_id"] == plus_populaire), 1, 1.0)
    heures = donnees["heures"]
    bonnes = sum(recommandation.heure_rappel(h["heures_ouverture"]) == h["attendu"] for h in heures)
    score("Heure de rappel correcte", bonnes, len(heures), 1.0)


# ===== Module 5 =====
def evaluer_moderation():
    from app.modeles import moderation, risque
    risque.charger()
    moderation.charger()
    section("Module 5 — Modération des messages (Detoxify + escalade)")
    cas, ko = lire_csv("m5_moderation.csv"), []
    par_classe: dict[str, list[int]] = {}
    for c in cas:
        r = moderation.moderer_message(c["texte"])
        bon = r["decision"] == c["attendu"]
        par_classe.setdefault(c["attendu"], [0, 0])
        par_classe[c["attendu"]][0] += bon
        par_classe[c["attendu"]][1] += 1
        if not bon:
            ko.append(f"| {c['texte']} (attendu {c['attendu']}) | {r['decision']} (toxicité {r['toxicite']}, "
                      f"risque {r['niveau_risque']}) |")
    score("Bonne décision (global)", len(cas) - len(ko), len(cas), 0.85)
    for classe, (b, t) in par_classe.items():
        score(f"  dont {classe}", b, t, 1.0 if classe == "ESCALADE_HUMAINE" else None)
    erreurs(ko)


# ===== Module 6 =====
def evaluer_risque():
    from app.modeles import risque
    risque.charger()
    section("Module 6 — Détection des signaux de détresse (règles + TF-IDF + embeddings)")
    cas = lire_csv("m6_risque.csv")
    with open(DOSSIER_DONNEES / "risque_dataset.csv", encoding="utf-8") as f:
        entrainement = {risque.normaliser(l["texte"]) for l in csv.DictReader(f)}
    communs = [c["texte"] for c in cas if risque.normaliser(c["texte"]) in entrainement]
    if communs:
        ligne(f"- ⚠️ {len(communs)} phrase(s) de test présentes dans le jeu d'entraînement : {communs}")
    vp = fn = vn = fp = critiques_ok = critiques = 0
    ko = []
    for c in cas:
        r = risque.detecter_risque(c["texte"])
        n, attendu = r["niveau"], c["niveau_attendu"]
        if attendu == "0-1":
            vn += n < 2
            fp += n >= 2
            bon = n < 2
        else:
            vp += n >= 2
            fn += n < 2
            bon = n >= 2
            if attendu == "3":
                critiques += 1
                critiques_ok += n == 3
                bon = n == 3
        if not bon:
            ko.append(f"| {c['texte']} (attendu {attendu}) | niveau {n} (p={r['probabilite']}, {r['source']}) |")
    score("Rappel sur la détresse (niveau ≥ 2 détecté)", vp, vp + fn, 0.95)
    score("Spécificité (pas d'alerte sur un texte banal)", vn, vn + fp, 0.85)
    score("Crises explicites classées niveau 3", critiques_ok, critiques, 1.0)
    erreurs(ko)


# ===== Chatbot d'orientation =====
def evaluer_chatbot():
    from app.modeles import chatbot, risque
    risque.charger()
    chatbot.charger()
    section("Chatbot d'orientation (risque d'abord, puis intention par similarité sémantique)")
    cas, ko = lire_csv("chatbot.csv"), []
    crises = [c for c in cas if c["intention_attendue"] in ("crise", "detresse")]
    for c in cas:
        r = chatbot.repondre(c["texte"])
        # Répondre « crise » à un message de détresse est l'erreur prudente voulue par le cahier des charges
        prudent = c["intention_attendue"] == "detresse" and r["intention"] == "crise"
        if r["intention"] != c["intention_attendue"] and not prudent:
            ko.append(f"| {c['texte']} (attendu {c['intention_attendue']}) | {r['intention']} (confiance {r['confiance']}) |")
    score("Bonne orientation", len(cas) - len(ko), len(cas), 0.85)
    urgences = sum(any(a["lien"].startswith("tel:") for a in chatbot.repondre(c["texte"])["actions"]) for c in crises)
    score("Messages de crise ou détresse avec un numéro d'urgence", urgences, len(crises), 1.0)
    erreurs(ko)


# ===== Reconnaissance faciale =====
def b64(f: Path) -> str:
    return base64.b64encode(f.read_bytes()).decode()


def evaluer_paires(visage, dossier: Path):
    """Paires étiquetées (master.csv : file_x, file_y, Decision Yes/No), ex. jeu de test de DeepFace."""
    ligne(f"\n**Paires étiquetées : `{dossier.name}`** (photos d'archives : anti-usurpation désactivée pour "
          "mesurer la reconnaissance seule)\n")
    visage.FACE_ANTI_SPOOFING = False
    with open(dossier / "master.csv", encoding="utf-8") as f:
        paires = list(csv.DictReader(f))
    empreintes, illisibles = {}, set()
    for nom in {p[k] for p in paires for k in ("file_x", "file_y")}:
        try:
            empreintes[nom] = visage.extraire_empreinte(b64(dossier / nom))
        except (visage.VisageErreur, FileNotFoundError):
            illisibles.add(nom)
    vrais = [p for p in paires if p["Decision"] == "Yes" and not {p["file_x"], p["file_y"]} & illisibles]
    faux = [p for p in paires if p["Decision"] == "No" and not {p["file_x"], p["file_y"]} & illisibles]

    def distance(p):
        return visage.distance_cosinus(empreintes[p["file_x"]], empreintes[p["file_y"]])

    d_vrais = [distance(p) for p in vrais]
    d_faux = [distance(p) for p in faux]
    if illisibles:
        ligne(f"- Photos sans visage détecté (ignorées) : {len(illisibles)} — {sorted(illisibles)}")
    score("Même personne acceptée", sum(d <= FACE_SEUIL for d in d_vrais), len(d_vrais), 0.9)
    score("Personne différente refusée", sum(d > FACE_SEUIL for d in d_faux), len(d_faux), 0.99)
    ligne(f"- Distance moyenne : même personne {sum(d_vrais) / len(d_vrais):.3f} · "
          f"personnes différentes {sum(d_faux) / len(d_faux):.3f} (seuil {FACE_SEUIL})")
    ligne(f"- Marge : même personne au plus {max(d_vrais):.3f} · personnes différentes au moins {min(d_faux):.3f}")
    ko = [f"| {p['file_x']} / {p['file_y']} : même personne refusée | distance {d:.3f} |"
          for p, d in zip(vrais, d_vrais) if d > FACE_SEUIL]
    ko += [f"| {p['file_x']} / {p['file_y']} : autre personne acceptée | distance {d:.3f} |"
           for p, d in zip(faux, d_faux) if d <= FACE_SEUIL]
    erreurs(ko)


def evaluer_visage():
    section(f"Reconnaissance faciale ({FACE_MODELE}, seuil {FACE_SEUIL})")
    dossiers = [p for p in (EVAL / "visages").iterdir() if p.is_dir()]
    jeux_paires = [p for p in dossiers if (p / "master.csv").exists()]
    personnes = {p.name: sorted(f for f in p.iterdir() if f.suffix.lower() in {".jpg", ".jpeg", ".png"})
                 for p in dossiers if p not in jeux_paires}
    personnes = {k: v for k, v in personnes.items() if len(v) >= 4}
    if not personnes and not jeux_paires:
        ligne("- Ignoré : aucune photo dans data/evaluation/visages (voir le README de ce dossier).")
        return
    from app.modeles import visage
    visage.charger()
    for dossier in jeux_paires:
        evaluer_paires(visage, dossier)
    if not personnes:
        return
    visage.FACE_ANTI_SPOOFING = FACE_ANTI_SPOOFING   # captures « vivantes » : réglage normal
    ligne("\n**Personnes (un dossier par personne)**\n")

    references, acceptes, essais_vrais, refuses, essais_faux, ko = {}, 0, 0, 0, 0, []
    for nom, photos in personnes.items():
        try:
            references[nom] = visage.enroler([b64(f) for f in photos[:3]])["empreinte"]
        except visage.VisageErreur as e:
            ko.append(f"| Enregistrement de {nom} | {e} |")
    for nom, photos in personnes.items():
        for f in photos[3:]:
            for ref_nom, ref in references.items():
                try:
                    r = visage.verifier(b64(f), ref)
                except visage.VisageErreur as e:
                    ko.append(f"| {nom}/{f.name} | {e} |")
                    break
                if ref_nom == nom:
                    essais_vrais += 1
                    acceptes += r["correspond"]
                    if not r["correspond"]:
                        ko.append(f"| {nom}/{f.name} refusé alors que c'est la bonne personne | distance {r['distance']} |")
                else:
                    essais_faux += 1
                    refuses += not r["correspond"]
                    if r["correspond"]:
                        ko.append(f"| {nom}/{f.name} accepté comme {ref_nom} | distance {r['distance']} |")
    score("Bonne personne acceptée", acceptes, essais_vrais, 0.9)
    if essais_faux:
        score("Autre personne refusée", refuses, essais_faux, 0.99)
    erreurs(ko)


EVALUATEURS = {
    "connexion": evaluer_connexion, "matching": evaluer_matching, "sentiment": evaluer_sentiment,
    "recommandation": evaluer_recommandation, "moderation": evaluer_moderation, "risque": evaluer_risque,
    "chatbot": evaluer_chatbot, "visage": evaluer_visage,
}

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--modules", nargs="+", choices=MODULES, default=MODULES)
    args = parser.parse_args()

    rapport.append(f"# Évaluation des modèles IA Nafsiti\n\n_{datetime.now():%d/%m/%Y %H:%M}_ — jeux de test : "
                   "`data/evaluation/`. ✅ objectif atteint · ⚠️ sous l'objectif.")
    debut = time.perf_counter()
    for module in args.modules:
        EVALUATEURS[module]()
    sortie = EVAL / "rapport.md"
    sortie.write_text("\n".join(rapport) + "\n", encoding="utf-8")
    print(f"\nRapport écrit dans {sortie} ({time.perf_counter() - debut:.0f} s)")
