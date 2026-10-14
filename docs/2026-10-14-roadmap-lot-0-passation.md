# Passation — Feuille de route, lot 0 (14 octobre 2026)

**Point d'entrée du chantier « retours à l'usage ».** La session suivante lit ce document, puis `docs/roadmap-retours-usage.md` (la feuille de route elle-même, avec les décisions dans chaque fiche). Reprendre à la section 8.

## 1. Où on en est

| Sujet | Décisions | État |
|---|---|---|
| 0.1 Corriger les erreurs (CI rouge) | Test de l'en-tête resserré sur le rôle bouton ; émulateurs avec 4 Go de disque, attente du service paquets, second essai dès qu'aucun test n'a tourné, job rouge sans test exécuté ; tests en pause par hypothèse non annotés ; Android 17 signalé, non bloquant | **Livré** : `main` verte, release v0.18.0 publiée |
| 0.2 Faire parler l'émulateur Android 17 | Aucune : fiche écrite avec constat et pistes | **À préciser** |
| Lots 1 à 6 | Aucune | À préciser, dans l'ordre |

État git : branche `main`, SHA `a23f0a3`, poussé, arbre propre, pas de PR (le dépôt n'en utilise pas). Dernier run CI vert : 38588075694 (commit `9c83d61`) ; le commit `a23f0a3` n'est que de la documentation, son run n'a pas été attendu.

Documents du chantier : `docs/roadmap-retours-usage.md` (feuille de route, fiches 0.1 et 0.2 à jour), `docs/handoff.md` (leçon ajoutée dans la vague 1 sur le faux vert Android 17), `docs/passation-meteo-sur-le-fil.md` (point 3.1, intact). Aucun amendement postérieur.

## 2. Ce qui reste, dans l'ordre

1. **Point 0.2** (`docs/roadmap-retours-usage.md`, fiche 0.2) : séance de précision, puis enquête. Pas de plan écrit. Dépend d'un poste avec SDK Android et du Pixel branché pour la première piste (vérifier sur vrai Android 17) ; sans cela, les pistes 2 et 3 (autre image, autres versions) se font en CI, à ~15 min le run.
2. **Lot 1** (1.1 à 1.7) : séance de précision avant toute ligne de code, règle 1 de la feuille de route. Le point 1.1 (supprimer la barre horizontale) simplifie la météo sur le fil (3.1) : le faire avant 3.1.
3. **Lots 2 à 6** : rien d'engagé. Le point 2.2 attend la météo sur le fil (même barre).

## 3. Décisions — verrouillées et ouvertes

**Verrouillées (séance du 14 octobre, avec Dimitri)** :
- Le test de l'en-tête vise le rôle bouton ; la ligne du temps **garde** son action « Voir la journée ». Ne pas la retirer pour un test.
- Un job émulateur sans test exécuté est rouge. Ne jamais revenir à un vert sans preuve.
- Android 17 : job **signalé, non bloquant** (`continue-on-error` sur l'entrée de matrice `blocking: false`) tant que le point 0.2 n'est pas livré. Ni retiré de la CI, ni bloquant.
- Pas d'erreur à l'usage à traiter dans le lot 0 pour l'instant.

**Ouvertes** :
- Point 0.2 : le temps à y consacrer, et où brancher le Pixel. **Ne tranche pas à sa place** : c'est la séance de précision.
- Point 0.2 : si l'émulateur Android 17 reste muet, rester en « signalé » ou retirer l'entrée ? Appartient à Dimitri.

## 4. Ce qui n'a PAS été vérifié — `UNVERIFIED`

- **Aucun gate Gradle n'a tourné en local** : pas de JDK, pas de SDK, pas d'adb sur ce poste. Tout le Kotlin et le workflow ont été validés par la CI seule. Raison structurelle : elle se représentera à chaque session sur ce poste.
- **La release v0.18.0 n'est pas installée sur le Pixel** : la mise à jour intégrée doit la proposer ; non constaté.
- **Le second essai sur émulateur frais** (`crashed=true`) n'a pas été exercé depuis la refonte : sur les runs observés, le premier essai a toujours exécuté des tests. Le chemin « aucun test → second essai → garde-fou » n'a de preuve que sur fixtures.
- **Le job Android 17 en `continue-on-error`** : vérifié sur un run que `Publication` se lance malgré son échec, pas que le workflow reste lisible (le run est « vert » avec un job rouge dedans).
- Le run de `a23f0a3` (docs seulement) n'a pas été attendu.

## 5. Les pièges de CE chantier

**Faux verts**
- *Symptôme* : job Android 17 vert depuis le 9 octobre. *Mode de panne* : l'APK ne s'installe pas (disque, ou service paquets pas prêt 76 s après le boot), le moteur de test d'AGP 9.4.1 consigne l'erreur, écrit `tests="0"` et `BUILD SUCCESSFUL` ; sans garde-fou, le job est vert avec zéro test, en silence. Garde-fou : `.github/scripts/require-device-tests.sh`.
- *Symptôme* : `Failed to inject touch input` sur Android 16, pris pour un aléa d'émulateur. *Mode de panne* : c'est l'habillage de Compose quand un sélecteur trouve deux nœuds ; la vraie raison est à la ligne suivante du rapport (`Expected exactly '1' node but found '2'`). Un message « émulateur » peut cacher une régression déterministe : compter les runs rouges d'affilée avant de parler d'instabilité.
- *Symptôme* : trois tests Beeper « failed » dans le rapport HTML et les annotations. *Mode de panne* : `AssumptionViolatedException` est classée échec par le rapport Gradle, pas par la tâche ; elle bruite sans faire échouer. Filtrée dans `annotate-failures.sh`.

**Tests qui ne testent rien**
- Le rapport de l'app sur Android 17 contenait `0 tests` sur tous les runs verts vérifiés (9, 10, 11, 12, 13 octobre). La phrase du handoff « émulateurs Android 16 et 17 verts » décrivait un vert sans test. Toujours lire le **compte de tests** du rapport, pas la couleur du job.

**Pièges de harnais**
- Le rapport instrumenté s'appelle `TEST-test(AVD) - 16-_app-.xml` : espaces et parenthèses. Une boucle `for f in $(find …)` le découpe et ne trouve rien ; le premier garde-fou a mis les deux jobs en rouge à tort. Utiliser `find -exec` ou `read -d ''`.
- `gh run list -c <sha>` ne filtre pas par commit (le watch a cherché un run vide) : prendre l'id avec `--json databaseId,headSha` et vérifier le SHA.
- `gh run view --json jobs` donne la `conclusion` des steps : un step en `continue-on-error` qui a échoué y apparaît `success` ; son `outcome` n'est pas exposé. Pour savoir si des tests ont échoué, lire le log ou le rapport.
- `${{ a && '' || 'b' }}` en expression GitHub Actions rend toujours `'b'` (chaîne vide = faux). Passer par un champ de matrice (`note`).
- Les artefacts `device-reports-android-NN` ne portent que les HTML (`**/build/reports/androidTests/`), pas les XML : pour un stack trace, c'est le HTML dépouillé de ses balises.

**Environnement et outillage** (détail en mémoire, [[bercail-repo-env]])
- Pas de JDK ni de SDK sur ce poste : vérification par la CI, scripts shell sur fixtures jetables.
- Push : Git Credential Manager porte le compte AlloVoisins ; passer par `http.extraHeader` Basic avec le token `BahuaudDimitri` (un `Bearer` ne suffit pas).
- `git commit -F <fichier du scratchpad>` : « Filename too long » ; passer le message par stdin.

## 6. Ce qui a bien marché, à refaire

- **Lire les rapports avant de toucher au code** : télécharger l'artefact (`gh run download -n device-reports-android-16`), dépouiller le HTML (`sed -e 's/<[^>]*>//g'`), lire la ligne sous l'erreur. Le diagnostic complet (régression vs aléa, faux vert) a tenu en une dizaine de commandes, sans émulateur local.
- **Compter les runs rouges d'affilée** (`gh run list --json conclusion,headSha`) pour distinguer régression (5 rouges consécutifs) et instabilité.
- **Un script de CI = une fixture positive et deux négatives avant câblage** (`tests ran`, `zéro test`, `aucun rapport`), avec le **nom réel** du fichier de rapport. Le faux adb qui refuse trois fois puis répond a validé l'attente du service paquets sans émulateur.
- **Séance de précision par `AskUserQuestion`**, constat tagué avant les questions, décisions écrites dans la fiche **avant** le premier commit de code ; puis remontée immédiate quand un fait nouveau (Android 17 jamais testé) changeait l'arbitrage.

## 7. Table `claim | preuve | commande`

| Claim | Preuve | Commande |
|---|---|---|
| `main` verte au commit `9c83d61` | `run=success`, jobs : gitleaks, 2 à 7, Android 16, Publication `success` ; Android 17 `failure` (signalé) | `gh run view 38588075694 -R BahuaudDimitri/Bercail-launcher --json conclusion,jobs` |
| Release v0.18.0 publiée | `Bercail 0.18.0 Latest v0.18.0 2026-10-14T15:28:30Z` | `gh release list -R BahuaudDimitri/Bercail-launcher -L 2` |
| Android 16 exécute ses tests | Garde-fou `require-device-tests.sh` passé sur le run 38586618075 et 38588075694 (14 tests sur le rapport du 14 octobre) | rapport `device-reports-android-16`, `index.html` : `14 tests` |
| Android 17 exécute enfin ses tests, 13 échecs sur 14 | `Test Summary 14 tests 13 failures 0 skipped` ; `Unable to resolve activity … cmp=io.github.bahuauddimitri.bercail.debug.test/…MainActivity` | artefact `device-reports-android-17` du run 38586618075 |
| Android 17 n'a jamais testé en CI | `0 tests` sur les runs 37984281223 (9 oct), 38049367995, 38172128575, 38301974002, 38460718231 | `gh run download <id> -n device-reports-android-17` puis `index.html` |
| Régression Android 16 depuis `9eeee6a` | 5 runs rouges consécutifs (d41e09d, f98f38a ×2, 95fddab, a5b5e89), 2 runs annulés avant (15682e8, 9eeee6a) | `gh run list -b main -L 12 --json conclusion,headSha` |
| Garde-fou valide sur fixtures | `ok` → `app: 10 instrumented tests ran.` exit 0 ; `zero` et `none` → `::error` exit 1 | `(cd <fixture> && bash .github/scripts/require-device-tests.sh)` |
| Attente du service paquets valide | faux adb : 3 refus puis `gradlew called: connectedDebugAndroidTest`, exit 0, 5 appels adb | `PATH=<stubs>:$PATH ADB_FAILS=3 bash .github/scripts/run-device-tests.sh` |
| Distant à jour | `## main...origin/main`, `a23f0a3` des deux côtés | `git fetch && git status -sb && git log -1 origin/main` |
| Gate Gradle local | — | `UNVERIFIED` : pas de JDK sur le poste |
| v0.18.0 installée sur le Pixel | — | `UNVERIFIED` |

Compte d'échecs de référence : Android 16, 14 tests, 0 échec (3 tests Beeper en pause par hypothèse, comptés « failed » par le rapport HTML, pas par la tâche). Android 17 : 14 tests, 13 échecs, c'est l'état connu du point 0.2.

## 8. Par où reprendre

```bash
git fetch origin && git status -sb && git log --oneline -5
GH_TOKEN=$(gh auth token -u BahuaudDimitri) gh run list -R BahuaudDimitri/Bercail-launcher -b main -L 3
```

1. Vérifier que le run de `a23f0a3` est vert (docs seulement, aucune publication attendue) et que Bercail a proposé v0.18.0 sur le Pixel.
2. Ouvrir `docs/roadmap-retours-usage.md`, fiche 0.2 : séance de précision (temps alloué, Pixel branché ou non), puis enquête. Lire d'abord `docs/handoff.md` section « Décisions de la phase de précisions » (elle prime).
3. Puis la séance de précision du lot 1, en un lot de questions, avant le premier commit.

Après le chantier lot 0 : lot 1 (ménage), 1.1 avant 3.1.
