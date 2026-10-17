# Passation — direction « Cadran » (feuille de route 6.1, 6.2, 1.7)

Point d'entrée du chantier. Écrit le 15 octobre 2026 avant un `/compact`, à la fin de la séance de maquettes ; complété le même jour après la séance lecteur (Écoute arrêté). **Reprendre par la section 8.**

## 1. Où on en est

| Sujet | Décisions | État |
|---|---|---|
| Maquettes de la direction « Cadran » | Trois planches publiées ; la troisième est la référence, retouchée dix fois | Validée par Dimitri |
| Écran Écoute (séance lecteur) | Planche 4 (trois pistes depuis deux pins) ; **B, l'arche**, retenue et retouchée ; reportée dans la référence (version 4) | Validée par Dimitri, fermée |
| Docs | `docs/handoff.md` (« Décisions UX figées », « Direction artistique », résumé, gestes) et `docs/roadmap-retours-usage.md` (6.1, 6.2, 1.7 → `Précisé`, cinq vagues, 2.4 première transition) réécrits | Commits `e4cd638`, `a912828`, `4c152e3`, `e6bdec0` sur `main` |
| Push | Premier push refusé (403 : compte git actif `dimitri-allovoisins`) ; les suivants avec un jeton scopé `BahuaudDimitri` | Poussé jusqu'à `4c152e3` ; `e6bdec0` et ce document : voir section 7 |
| Code | Rien. Ce poste n'a pas l'outillage Android : **aucun développement ici** (décision de Dimitri, 15 octobre) | — |

Arbre : cinq fichiers modifiés **avant cette session**, non commités, à ne pas toucher ni inclure : `Search.kt`, `SearchTest.kt`, `docs/CATALOG.md`, `SearchList.kt`, `SearchListUiTest.kt` (chantier recherche d'une autre session, point 1.4 probablement).

Documents du chantier :
- Référence visuelle (fait foi pour le rendu) : https://claude.ai/artifact/5NHHJQSjetVk85mu8XfSjD — version 4, « Bercail Cadran, référence », Écoute en arche. Fichier source dans le scratchpad de cette session, perdu au prochain nettoyage : pour la modifier, **relire l'artefact** (action read) et republier à la même adresse.
- Planche 4, l'écran Écoute : https://claude.ai/artifact/Cfr4zVSNAVvVdetu3qmgJD — version 3, « Bercail Écoute, trois pistes » : B retenue en tête (musique sans barre native, vidéo), la transition en trois images (chaque cadre est le même gabarit à un instant `--t`), le tableau de ce que B engage, A et C écartées en bas. Références : deux pins Pinterest (950752171321045164, 169096160996634711 ; images lues par leur balise `og:image` sur `i.pinimg.com/736x/…`, le fetch de la page Pinterest ne rend rien).
- Planches intermédiaires : https://claude.ai/artifact/4M2KFj6bJf7ypqCJrXgvC9 (trois directions A/B/C), https://claude.ai/artifact/52Bp3K5kFqkUYEETGp4BaN (C sur sept écrans, avant retouches). Historiques, ne font plus foi.
- Planche du 14 octobre « Bercail — pistes accueil » : https://claude.ai/artifact/RycUTpQNBg1DbYCVrV9QRK (autre organisation, partagée publiquement ; type Design, se lit par ses fichiers `project/*.dc.html`).
- Règles : `docs/handoff.md` sections « Décisions UX figées » et « Direction artistique » (révisées le 15 octobre) ; feuille de route 6.1 pour les vagues.

## 2. Ce qui reste, dans l'ordre

1. ~~Se mettre d'accord sur le lecteur~~ : fait le 15 octobre (arche, planche 4, référence v4, handoff et feuille de route à jour, commit `e6bdec0`).
2. ~~**Vague 1, le fil**~~ : livrée le 16 octobre 2026 (voir la fiche 6.1 de la feuille de route, « Vague 1 livrée »).
3. ~~**Vague 2, le bandeau et la feuille**~~ : livrée le 16 octobre 2026 (fiche 6.1, « Vague 2 livrée ») ; le verre givré (6.2) attend sa mesure Perfetto sur le Pixel avant d'être gardé.
4. ~~**Vague 3, le dock**~~ : livrée le 16 octobre 2026 (fiche 6.1, « Vague 3 livrée ») et 1.7 avec elle ; les vraies icônes et les raccourcis du toucher long restent à voir sur le Pixel, avec la version publiée choisie comme écran d'accueil.
5. ~~**Vague 4, les feuilles**~~ : livrée le 17 octobre 2026 (fiche 6.1, « Vague 4 livrée ») : boutons en pilule, lignes de liste, feuilles sans voile, recherche givrée, réglages à plat, fond `#101119` ; le 17 octobre, les apps de la recherche et de la feuille « Écouter » ont repris leur vraie icône en couleurs, sans anneau, et le dock a perdu ses anneaux. Ce qui reste à voir sur le Pixel est listé dans la fiche.
6. **Suivante : la vague 5, Écoute (l'arche)**, dans l'ordre de la feuille de route 6.1 ; elle porte aussi la première transition de 2.4 et le masquage de la barre native.

## 3. Décisions — verrouillées et ouvertes

**Verrouillées** (ne pas rouvrir, options écartées en séance) :
- Direction C « Cadran » retenue ; A (reskin) et B (relecture) écartées.
- Le rendez-vous reste le héros ; **pas d'horloge empilée** (écartée deux fois).
- Fil tendu depuis le bord, 3 dp, **sans crans** (la règle graduée est écartée), « maintenant » en **anneau simple** (la poignée-pilule est écartée), titres des rendez-vous sur le fil, réveil en bout, règle d'écartement à 30 min, lâcher ouvre l'agenda du téléphone.
- Bandeau replié plat : résumé, **dock de cinq icônes au trait** (icônes réelles en monochrome, pas de glyphes dessinés), **barre Chercher en pilule bordée** qui ne bouge jamais. La **pastille musique reste**, alignée sur le réveil (la ligne musique du bandeau et la progression sur le bord bas sont écartées).
- Feuille ouverte en **verre givré** (valeurs dans le handoff), contenant **seulement gens et maison** en anneaux ; ni dock ni Chercher dedans. La feuille plate à liseré est écartée.
- Conversation : **les bulles restent** (le transcript à filets est écarté) ; champ Répondre en pilule bordée.
- **Vrais boutons en pilule partout, jamais de lien souligné.** Sélecteurs des réglages en pilule (les onglets soulignés sont écartés).
- Trois accents du moment, ceux de Brume.
- Aucun développement sur ce poste.
- **Écoute = l'arche** (15 octobre) : pochette en arche 300 × 440 dp depuis le bord haut, progression en demi-cercle sous l'arche seulement (les flancs sont écartés), temps écoulé et durée au centre, textes sous la pochette jamais dedans, lecteur seul, **le héros s'efface aussi**, barre native cachée sur Écoute seulement, entrée par glissement vers la gauche ou la pastille, **sortie par glissement vers le haut** (plus de latéral sur le lecteur), pastille avec chevron, plus de points de pagination. Écartés : A le disque, C pleine largeur et feuille, le titre dans l'image, la forme d'onde, le disque avec un trou, aimer / aléatoire / file d'attente (rien dans le domaine), le thème clair, le glissement vers le bas pour entrer (geste du volet de notifications).

**Ouvertes** (ne tranche pas à sa place) :
- Point 3.3 (tirer parti de la barre verticale) et 3.1 (météo sur le fil) : compatibles avec le fil sans crans, non rediscutés.

## 4. Ce qui n'a PAS été vérifié — `UNVERIFIED`

- **Le rendu des trois planches** : jamais regardé par moi. Le navigateur intégré n'est pas connecté à claude.ai (artefacts privés invisibles), un fichier HTML hors projet en `file://` ne charge pas, et l'outil de capture a été bloqué par le hook anti-boucle après trois échecs. Dimitri a validé à l'écran ; les figcaptions et le tableau des décisions sont la source sûre, pas le pixel.
- **Lâcher sur un rendez-vous ouvre l'agenda** : `[Likely]` faisable par `ACTION_VIEW` sur `CalendarContract.Events` + id ; non testé sur le Pixel.
- **La CI après les commits docs** : non regardée (`docs:` ne publie rien ; `CatalogTest` ne lit pas le handoff).
- **Le masquage de la barre native** : `[Likely]` `WindowInsetsControllerCompat.hide(statusBars())` avec `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`, animé par le système ; non essayé sur le Pixel. À vérifier en vague 5 : l'aller-retour de la barre à chaque entrée/sortie d'Écoute ne doit pas agacer, sinon la cacher partout ou nulle part (l'accueil n'a pas d'horloge à lui : la barre est la seule).
- **Le tiré de l'arc** : l'anneau suit l'angle du doigt autour du centre de l'arche ; précision sous le pouce non testée.

## 5. Les pièges de CE chantier

*Environnement et outillage*
- **Artefact d'une autre organisation** (lien `?org=…`) : lecture refusée tant que Dimitri ne l'a pas partagé publiquement ; ensuite, un artefact de type Design rend « Loading… » sur sa page : lire ses fichiers (`action: list, scope: files`, puis `read` avec `paths` sur `project/*.dc.html`). Sans ça, une session tourne en rond sur la page de connexion.
- **Push sur `main` refusé en 403** : le compte git actif est `dimitri-allovoisins`. Pousser avec `git push "https://x-access-token:$(gh auth token -u BahuaudDimitri)@github.com/BahuaudDimitri/Bercail-launcher.git" main`, jamais `gh auth switch`. Le hook avant push lance `./gradlew check` : plusieurs minutes, lancer en arrière-plan.
- **Python absent** sur ce poste : les scripts jetables passent par `node`.
- **L'outil de capture du navigateur** est coupé par le hook anti-boucle cc-forge après trois échecs consécutifs ; ne pas insister, demander à Dimitri de regarder.
- **Pinterest** : le fetch d'une page de pin rend « Content truncated » sans rien ; `curl` de la page avec un User-Agent de navigateur donne les balises `og:image`, et l'image se lit sur `i.pinimg.com/736x/…` (le chemin `originals/` rend une erreur XML).
- **Gabarit `<template>` cloné en JS** : un attribut booléen (`<div data-home>`) ressort de `innerHTML` comme `data-home=""` ; un `replace` sur la chaîne littérale rate sans bruit (cadres vides). Utiliser une expression régulière tolérante.

*Pièges de harnais*
- **Le `/compact` ne garde pas le scratchpad** : les sources HTML des planches n'existent que là. Pour retoucher la référence, relire l'artefact publié, pas chercher le fichier.

*Faux verts*
- Rien à signaler : aucun test lancé dans cette session.

## 6. Ce qui a bien marché, à refaire

- **Planches graduées puis convergentes** : trois directions, puis la retenue déclinée, puis une référence unique republiée **à la même adresse** à chaque retouche. Dimitri répond par listes de retouches précises : les appliquer toutes d'un coup et republier, sans re-demander.
- **Le sélecteur Matin / Journée / Soir** dans la planche : l'accent du moment se juge sur trois états sans tripler les écrans.
- **Le tableau des décisions dans la forme du handoff**, écrit dans la planche avant le commit docs : la validation du visuel vaut validation du texte, le commit n'est plus qu'une copie.
- **Les questions groupées par quatre** avec une recommandation en premier, à chaque gate.

## 7. Table `claim | preuve | commande`

| Claim | Preuve | Commande |
|---|---|---|
| Commit docs principal existe | `e4cd638 docs: settle the Cadran direction for the home screen` | `git log -3 --oneline` |
| Commit lecteur provisoire existe | `a912828 docs: allow the provisional player while music references are sought` | `git log -3 --oneline` |
| Premier push refusé | `Permission to BahuaudDimitri/Bercail-launcher.git denied to dimitri-allovoisins` (403) | sortie du `git push origin main` |
| Second push abouti | `remote a912828d33b1ba0f60aae583a97617f7d1c6caea` et `## main...origin/main` sans retard | `git fetch origin && git rev-parse HEAD origin/main && git status -sb` |
| Référence publiée, Écoute en arche | `Published … at https://claude.ai/artifact/5NHHJQSjetVk85mu8XfSjD (Version 4, version id 1792076643-8491)` | tool result Artifact |
| Planche 4 publiée | `Published … at https://claude.ai/artifact/Cfr4zVSNAVvVdetu3qmgJD (Version 3, version id 1792076389-4277)` | tool result Artifact |
| Commit Écoute existe | `e6bdec0 docs: settle the Listen screen on the arch player` (2 fichiers, 7 insertions, 6 suppressions) | `git log -1 --oneline && git diff --stat HEAD~1 HEAD` |
| Commit Écoute et ce document poussés | à relire dans la sortie du push ; si ce document dit encore ceci, vérifier `git fetch origin && git rev-parse HEAD origin/main` | — |
| Rendu des planches conforme | `UNVERIFIED` (voir section 4) | — |

## 8. Par où reprendre

```bash
git fetch origin && git status -sb && git rev-parse HEAD origin/main
git log -3 --oneline
```

1. `origin/main` doit être au moins `e6bdec0` (plus le commit de ce document) ; sinon, repousser avec le jeton scopé (section 5), en arrière-plan.
2. Relire `docs/handoff.md` « Décisions UX figées » (lignes « Milieu » et « Écran Écoute », et les gestes d'accès à Écoute) et la feuille de route 6.1 (« Écoute, arrêté », vague 5) et 2.4.
3. Plus rien à maquetter : vague 1 (le fil) sur un poste outillé, puis 2 à 5. Pour la vague 5, la planche 4 porte la transition image par image.
4. Ce document se supprime quand la vague 5 est livrée.
