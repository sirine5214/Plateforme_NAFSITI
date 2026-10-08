# Évaluation des modèles IA Nafsiti

_08/10/2026 21:22_ — jeux de test : `data/evaluation/`. ✅ objectif atteint · ⚠️ sous l'objectif.

## Module 1 — Détection de connexions anormales (IsolationForest)

- **Bonne action (autoriser / MFA / bloquer)** : 16/16 = 100% ✅
- Connexions normales bloquées : 0

## Module 2 — Matching patient / thérapeute (embeddings)

- **Bon thérapeute classé 1er** : 12/12 = 100% ✅
- **Bon thérapeute dans le top 3** : 12/12 = 100% ✅

## Module 3 — Valence du journal (XLM-RoBERTa sentiment) et tendance

- **Polarité correcte (positif ≥ 3,5 / négatif ≤ 2,5)** : 23/25 = 92% ✅

| Cas en erreur | Obtenu |
|---|---|
| J'ai fait les courses et préparé le dîner (attendu neutre) | positif (valence 4.0) |
| Cette dispute m'a laissé un goût amer (attendu negatif) | positif (valence 4.18) |
- **Tendance et alerte de baisse correctes** : 5/5 = 100% ✅

## Module 4 — Recommandation de ressources (similarité sémantique)

- **Contenu pertinent dans les 3 recommandations** : 6/6 = 100% ✅
- **Démarrage à froid : contenu le plus populaire en tête** : 1/1 = 100% ✅
- **Heure de rappel correcte** : 3/3 = 100% ✅

## Module 5 — Modération des messages (Detoxify + escalade)

- **Bonne décision (global)** : 13/14 = 93% ✅
- **  dont PUBLIER** : 6/6 = 100%
- **  dont BLOQUER** : 3/4 = 75%
- **  dont ESCALADE_HUMAINE** : 4/4 = 100% ✅

| Cas en erreur | Obtenu |
|---|---|
| Espèce de crétin, tu ne sers à rien (attendu BLOQUER) | ESCALADE_HUMAINE (toxicité 0.982, risque 2) |

## Module 6 — Détection des signaux de détresse (règles + TF-IDF + embeddings)

- **Rappel sur la détresse (niveau ≥ 2 détecté)** : 20/20 = 100% ✅
- **Spécificité (pas d'alerte sur un texte banal)** : 20/20 = 100% ✅
- **Crises explicites classées niveau 3** : 6/6 = 100% ✅

## Chatbot d'orientation (risque d'abord, puis intention par similarité sémantique)

- **Bonne orientation** : 24/26 = 92% ✅
- **Messages de crise ou détresse avec un numéro d'urgence** : 3/3 = 100% ✅

| Cas en erreur | Obtenu |
|---|---|
| Je me sens isolé en ce moment (attendu tristesse) | detresse (confiance 1.0) |
| Je veux effacer toutes mes données (attendu confidentialite) | detresse (confiance 1.0) |

## Reconnaissance faciale (Facenet512, seuil 0.35)


**Paires étiquetées : `deepface_exemples`** (photos d'archives : anti-usurpation désactivée pour mesurer la reconnaissance seule)

- Photos sans visage détecté (ignorées) : 1 — ['img8.jpg']
- **Même personne acceptée** : 36/37 = 97% ✅
- **Personne différente refusée** : 239/239 = 100% ✅
- Distance moyenne : même personne 0.224 · personnes différentes 0.999 (seuil 0.35)
- Marge : même personne au plus 0.454 · personnes différentes au moins 0.425

| Cas en erreur | Obtenu |
|---|---|
| img3.jpg / img12.jpg : même personne refusée | distance 0.454 |
