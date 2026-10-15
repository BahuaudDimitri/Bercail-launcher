# Passation — direction « Cadran » (feuille de route 6.1, 6.2, 1.7)

Point d'entrée du chantier. Écrit le 15 octobre 2026 avant un `/compact`, à la fin de la séance de maquettes. **Reprendre par la section 8.**

## 1. Où on en est

| Sujet | Décisions | État |
|---|---|---|
| Maquettes de la direction « Cadran » | Trois planches publiées ; la troisième est la référence, retouchée dix fois puis Écoute simplifiée | Validée par Dimitri, **sauf l'écran Écoute** |
| Docs | `docs/handoff.md` (« Décisions UX figées », « Direction artistique », résumé) et `docs/roadmap-retours-usage.md` (6.1, 6.2, 1.7 → `Précisé`, cinq vagues) réécrits | Commits `e4cd638` et `a912828` sur `main` |
| Push | Premier push refusé (403 : compte git actif `dimitri-allovoisins`) ; second push avec un jeton scopé `BahuaudDimitri` | Poussé : `origin/main` = `a912828`, vérifié par `git fetch` |
| Code | Rien. Ce poste n'a pas l'outillage Android : **aucun développement ici** (décision de Dimitri, 15 octobre) | — |

Arbre : cinq fichiers modifiés **avant cette session**, non commités, à ne pas toucher ni inclure : `Search.kt`, `SearchTest.kt`, `docs/CATALOG.md`, `SearchList.kt`, `SearchListUiTest.kt` (chantier recherche d'une autre session, point 1.4 probablement).

Documents du chantier :
- Référence visuelle (fait foi pour le rendu) : https://claude.ai/artifact/5NHHJQSjetVk85mu8XfSjD — version 3, « Bercail Cadran, référence ». Fichier source dans le scratchpad de cette session (`bercail-cadran-reference.html`), perdu au prochain nettoyage : pour la modifier, **relire l'artefact** (action read) et republier à la même adresse.
- Planches intermédiaires : https://claude.ai/artifact/4M2KFj6bJf7ypqCJrXgvC9 (trois directions A/B/C), https://claude.ai/artifact/52Bp3K5kFqkUYEETGp4BaN (C sur sept écrans, avant retouches). Historiques, ne font plus foi.
- Planche du 14 octobre « Bercail — pistes accueil » : https://claude.ai/artifact/RycUTpQNBg1DbYCVrV9QRK (autre organisation, partagée publiquement ; type Design, se lit par ses fichiers `project/*.dc.html`).
- Règles : `docs/handoff.md` sections « Décisions UX figées » et « Direction artistique » (révisées le 15 octobre) ; feuille de route 6.1 pour les vagues.

## 2. Ce qui reste, dans l'ordre

1. **Se mettre d'accord sur le lecteur (écran Écoute)** : Dimitri cherche des références musique ; séance de maquettes dédiée, à rapprocher du point 4.1 (Spotify via Sonos). Livrable : la référence republiée (même adresse) avec Écoute arrêté, puis le handoff (ligne « Écran Écoute ») et la feuille de route (6.1 « Reste à préciser », vague 5).
2. **Vague 1, le fil** (`BcColors`, `BcTimeline`) : sur un poste avec l'outillage Android, pas ici. Dépend de rien.
3. Vagues 2 à 5 dans l'ordre de la feuille de route 6.1 ; la vague 2 (bandeau et feuille) porte aussi 6.2 ; la vague 3 (dock) ferme 1.7. La vague 5 (Écoute) attend le point 1 ci-dessus.

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

**Ouvertes** (ne tranche pas à sa place) :
- **L'écran Écoute** : figé = lecteur seul, ni dock ni Chercher, la pochette dans le cercle. Tout le reste (disposition, boutons, progression) attend les références de Dimitri. Le cadran à crans et l'arc autour de la pochette ont déplu ; le lecteur simple façon Spotify est provisoire et codable, mais sera revu.
- Point 3.3 (tirer parti de la barre verticale) et 3.1 (météo sur le fil) : compatibles avec le fil sans crans, non rediscutés.

## 4. Ce qui n'a PAS été vérifié — `UNVERIFIED`

- **Le rendu des trois planches** : jamais regardé par moi. Le navigateur intégré n'est pas connecté à claude.ai (artefacts privés invisibles), un fichier HTML hors projet en `file://` ne charge pas, et l'outil de capture a été bloqué par le hook anti-boucle après trois échecs. Dimitri a validé à l'écran ; les figcaptions et le tableau des décisions sont la source sûre, pas le pixel.
- **Lâcher sur un rendez-vous ouvre l'agenda** : `[Likely]` faisable par `ACTION_VIEW` sur `CalendarContract.Events` + id ; non testé sur le Pixel.
- **La CI après les deux commits docs** : non regardée (`docs:` ne publie rien ; `CatalogTest` ne lit pas le handoff).

## 5. Les pièges de CE chantier

*Environnement et outillage*
- **Artefact d'une autre organisation** (lien `?org=…`) : lecture refusée tant que Dimitri ne l'a pas partagé publiquement ; ensuite, un artefact de type Design rend « Loading… » sur sa page : lire ses fichiers (`action: list, scope: files`, puis `read` avec `paths` sur `project/*.dc.html`). Sans ça, une session tourne en rond sur la page de connexion.
- **Push sur `main` refusé en 403** : le compte git actif est `dimitri-allovoisins`. Pousser avec `git push "https://x-access-token:$(gh auth token -u BahuaudDimitri)@github.com/BahuaudDimitri/Bercail-launcher.git" main`, jamais `gh auth switch`. Le hook avant push lance `./gradlew check` : plusieurs minutes, lancer en arrière-plan.
- **Python absent** sur ce poste : les scripts jetables passent par `node`.
- **L'outil de capture du navigateur** est coupé par le hook anti-boucle cc-forge après trois échecs consécutifs ; ne pas insister, demander à Dimitri de regarder.

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
| Référence publiée | `Published … at https://claude.ai/artifact/5NHHJQSjetVk85mu8XfSjD (Version 3, version id 1792061108-80ce)` | tool result Artifact |
| Rendu des planches conforme | `UNVERIFIED` (voir section 4) | — |

## 8. Par où reprendre

```bash
git fetch origin && git status -sb && git rev-parse HEAD origin/main
git log -3 --oneline
```

1. `origin/main` doit être au moins `a912828` (plus le commit de ce document) ; sinon, repousser avec le jeton scopé (section 5), en arrière-plan.
2. Relire `docs/handoff.md` « Décisions UX figées » (ligne « Écran Écoute (à préciser) ») et la feuille de route 6.1 « Reste à préciser ».
3. Séance lecteur : partir des références musique de Dimitri, relire la référence publiée, maquetter Écoute seulement (deux ou trois pistes dans une planche séparée, puis la retenue republiée dans la référence), puis mettre à jour handoff et feuille de route dans un commit `docs:`.
4. Après le chantier : vague 1 (le fil) sur un poste outillé ; ce document se supprime quand la vague 5 est livrée.
