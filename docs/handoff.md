# Bercail — Passation design → Android

Oct 3, 2026 · @Dimitri

> Copie mise à jour le 11 octobre 2026 (vagues 4, 5 et 6 développées) du doc de passation (Claude Docs). Le doc d'origine reste la référence ; cette copie est mise à jour à chaque vague.

## Résumé

Bercail (« Fil » dans le prototype et dans les premières versions de ce document) est un launcher Android pour Pixel 9 et Galaxy S25, sombre et minimaliste. Il est pensé pour quatre usages : les messages (Beeper), la musique et la vidéo (Spotify, YouTube), la maison (lampes Philips Hue ; Home Assistant plus tard) et l'agenda. Toutes les décisions de ce document sont figées. Elles ne changent que par une décision explicite, notée ici.

**Principes directeurs**

- **Pratique avant tout.** Tout ce qui sert chaque jour est atteignable au pouce, dans la moitié basse de l'écran.
- **Rien ne bouge.** Les éléments gardent leur place d'un moment à l'autre ; seul le contenu change.
- **Des activités, pas des apps.** Pas de grille d'icônes sur l'accueil : des gens, une maison, un son, un agenda.
- **Agir sans ouvrir d'app.** Répondre, piloter la musique, allumer une lumière se font depuis l'accueil.
- **Un seul langage de tiroir.** Tout ce qui s'ouvre (recherche, conversation, journée, apps média) est le même tiroir en verre, depuis le bas.

**Références**

- Prototype interactif v14, référence visuelle et comportementale : [Fil Launcher](https://claude.ai/artifact/He7k95oFsyYaAQtFxDJEjz). En cas de doute, le prototype fait foi pour le rendu ; ce document fait foi pour les règles.
- Article de méthode : [How to turn your AI into a world-class designer](https://www.lennysnewsletter.com/p/how-to-turn-your-ai-into-a-world) (Lenny's Newsletter).
- Archives du prototype : fichiers `fil-launcher-v2.html` à `fil-launcher-v13.html`, à copier dans le dépôt sous `design/prototype/`.

## Décisions UX figées

L'accueil a deux écrans (Accueil et Écoute) et un seul tiroir en verre en bas, qui change de contenu selon le contexte.

| Zone | Décision |
| --- | --- |
| Haut de l'accueil | Météo en une ligne (température + phrase), puis « Dans 1 h 20 · 09:30 », le titre du prochain rendez-vous en grand, et « Ensuite … à … ». Toucher ce bloc ouvre le tiroir « Aujourd'hui ». |
| Ligne du temps | Verticale par défaut, le long du bord droit, de 7 h (haut) à minuit (au-dessus du tiroir). Passé en trait plein, futur en pointillé, point lumineux pour maintenant, heure écrite à côté de chaque rendez-vous, le prochain entouré. Variante horizontale disponible dans les réglages. Aucun point de message sur la ligne. |
| Milieu | Zone d'ambiance, non interactive sauf la pastille musique et le glissement horizontal entre écrans. Points de pagination en bas, visibles seulement si un contenu média existe. |
| Tiroir replié (défaut) | Poignée, puis deux lignes : messages (un point couleur par personne + « 2 non lus · Léa, Tom »), maison (« Salon 60 %, Volets ouverts »). Puis la barre « Chercher ». |
| Tiroir ouvert (Essentiel) | 4 favoris en tuiles pastel + « Tous » (ouvre Beeper, compte les non-lus hors favoris), 4 commandes maison rondes + « Tout » (ouvre l'app Hue). Barre « Chercher » inchangée. |
| Écran Écoute | Existe seulement si un contenu est chargé (lecture ou pause). Grande pochette avec halo de sa couleur, titre sur 2 lignes max, artiste, bouton « Ouvrir dans \<app> ». Le tiroir devient la télécommande : progression (touchable), précédent, lecture/pause, suivant. La ligne du temps s'efface. |
| Pastille musique | Sur l'accueil, en bas à gauche du milieu. Avec contenu : pochette ronde + « Titre · Artiste » + égaliseur animé (figé en pause), ouvre Écoute. Sans contenu : icône note + « Écouter… », ouvre le tiroir des apps média. |
| Tiroir apps média | App principale en tête (mise en avant, « Reprendre « dernier contenu » », bouton lecture), puis les autres apps média installées. |
| Recherche | La barre « Chercher » est le champ lui-même. Au toucher, le tiroir grandit jusqu'en haut, ses lignes se replient, la liste apparaît. « Réglages de Fil » en tête, apps favorites, puis A à Z avec alphabet en vague sur le bord (lettres utilisées seulement). En tapant : résultats ancrés en bas, sections Apps, Gens, Messages, Maison, Agenda, Réglages ; sinon propositions web et Play Store. |
| Conversation | Tiroir : en-tête personne + réseau (« WhatsApp via Beeper ») + lien Beeper, bulles (couleur de la personne / les miennes en clair), dictée, champ, envoyer. Les réponses rapides du prototype ont été retirées le 13 octobre 2026 (voir la vague 8). |
| Fond | Brume (taches floues aux couleurs de la pochette) quand un contenu est chargé. Sans contenu : fond d'écran de l'utilisateur, en fondu. Option : garder la brume aux couleurs du moment. |
| Météo visible | Pluie (traits en biais + gouttes sur la vitre sur l'image), vent (rafales ondulées), neige (flocons), nuit (étoiles, sauf pluie). Image traitée : plus claire et chaude au soleil, plus sombre et froide sous la pluie, pâle sous la neige. |

## Règles de comportement

Chaque règle ci-dessous est vérifiable et doit devenir au moins un test.

**Musique et écrans**

- Trois états média : *Aucun* (pas de session), *Pause*, *Lecture*. L'écran Écoute n'est accessible qu'en Pause ou Lecture.
- Passage en *Aucun* alors que l'écran Écoute est affiché : retour automatique à l'Accueil.
- Accès à Écoute : glisser vers la gauche au milieu de l'écran, toucher la pastille, ou les points de pagination. Glisser vers la droite revient à l'Accueil. En état *Aucun*, le glissement vers la gauche ne fait rien.
- Choisir une app dans le tiroir média ouvre l'app ; dès qu'une session média apparaît, la pastille passe en mode « en lecture ».
- Précédent : revient au début si la position dépasse 5 s, sinon morceau précédent.

**Tiroir**

- Replier/déplier : toucher la poignée, toucher une ligne du résumé, ou glisser de plus de 24 dp vers le haut/bas n'importe où sur le tiroir. Le glissement continue de compter si le doigt sort du tiroir. Un glissement n'active aucun bouton.
- Passage Accueil ↔ Écoute : hauteur du tiroir animée (\~400 ms), contenus en fondu ; rien ne « saute ».
- L'état replié/ouvert est conservé quand on revient sur l'Accueil.
- La barre « Chercher » ne change jamais de position, quel que soit l'état.

**Recherche**

- Focus sur le champ : le tiroir passe en position absolue, sa hauteur s'anime de la hauteur courante à tout l'écran (\~450 ms), les lignes se replient, la liste apparaît en fondu.
- Fermeture (flèche, retour système, ouverture d'un résultat) : animation inverse vers la hauteur d'avant, champ vidé.
- Recherche insensible aux accents et à la casse. Toucher une commande maison dans les résultats la bascule sur place.
- Alphabet : seulement les lettres ayant au moins une app ; glisser dessus fait défiler jusqu'à la section, avec effet de vague (lettres proches agrandies et décalées).

**Conversation**

- Ouvrir une conversation marque ses messages comme lus. Le badge et le résumé se mettent à jour.
- Le dernier message est toujours visible ; l'historique défile à l'intérieur, sans barre de défilement visible.
- (Réponses rapides : retirées le 13 octobre 2026, voir la vague 8.) « Annuler l'envoi » s'affiche sous le message envoyé, 6 s, avec un anneau de compte à rebours ; cible ≥ 40 dp ; jamais à l'endroit d'une réponse rapide.
- Le brouillon (tapé ou dicté) est conservé par personne à la fermeture. Bouton envoyer désactivé si le champ est vide.
- Mes réponses n'apparaissent jamais comme écrites par le contact.

**Textes générés**

- Un libellé par appareil, identique partout (résumé, tuile, recherche) : « Salon 60 % », « Volets ouverts », « Cinéma active », « Chauffage 21 °C ». Rien d'allumé : « Maison au repos ».
- Scène Cinéma : Salon à 10 % et volets fermés d'un seul toucher.
- Phrase météo dépendante de l'heure (pas de « nuit claire » le matin) ; neige → 0° dans la maquette, valeur réelle en production.
- « Dans X min » sous 1 h, « Dans 1 h 20 » au-delà. Plus rien : « Plus rien aujourd'hui ».

## Direction artistique

Interface sombre unique, en relief doux et en verre, avec des pastels pour les personnes et la maison. Valeurs reprises du prototype v14, à transposer en tokens Compose.

| Token | Valeur | Usage |
| --- | --- | --- |
| Fond de base | `#15161F` (nuit `#101119` → `#1B1C2A`) | Sous la brume |
| Texte | `#ECEBF4` | Texte principal |
| Texte discret | `#B2B0C4` | Libellés, heures |
| Verre tiroir | noir bleuté 34 % (ouvert 50 %, recherche 80 %, feuilles 62 %), flou 26 + saturation 1.5 | Tiroir et feuilles |
| Tuile | blanc 7.5 % + liseré blanc 10 % | Boutons éteints, puces |
| Pastel 1 | `#F2A49A` | Léa, Chauffage |
| Pastel 2 | `#9CC4F0` | Tom, Volets |
| Pastel 3 | `#9FD8C4` | Équipe, Spotify |
| Pastel 4 | `#B4A7F0` | Maman, Cinéma, Beeper |
| Pastel 5 | `#F6C28B` | Julien, Salon, Home Assistant |
| Éteint | `#4A4A5C` | Commande éteinte dans la recherche |

**Typographie** : Outfit (Google Fonts), graisses 200 à 700. Titre du rendez-vous 30 sp graisse 300 ; texte courant 13,5 à 15 sp ; libellés 11 à 12,5 sp, jamais moins de 10,5 sp.

**Formes** : tuiles personnes 46 dp rayon 16 ; commandes maison rondes 48 dp ; tiroir et feuilles rayon 30 en haut ; pilules rayon plein ; cibles tactiles ≥ 40 dp.

**Mouvements** : tiroir et feuilles 350 à 450 ms, courbe (0.2, 0.8, 0.2, 1) ; fondus 200 à 350 ms ; ligne du temps qui se rétracte en 500 ms ; aucune animation ne déplace une cible pendant qu'on la vise. Tout respecte « réduire les animations ».

**Brume** : quatre taches radiales aux deux couleurs de la pochette (extraites avec Palette), mélange additif, dérive lente (vent : plus loin et plus vite), légère respiration en lecture, 30 images/s max, pause hors écran. Un voile sombre en haut garantit la lisibilité de l'agenda. Sans pochette : couleurs du moment (matin pêche, journée bleu-vert, soir lavande).

## Réglages de Fil

Les réglages s'ouvrent depuis la ligne « Réglages de Fil » en tête de la recherche (ou en tapant « réglages »). Ils sont stockés localement (DataStore).

| Réglage | Valeurs | Défaut |
| --- | --- | --- |
| Fond sans musique | Mon image · Brume du moment | Mon image (fond du système) |
| App média principale | Liste des apps média installées | Spotify |
| Ligne du temps | Verticale · Horizontale | Verticale |
| Tiroir au départ | Replié · Ouvert | Replié |
| Écoute automatique | Casque connecté → écran Écoute | Activé |
| Favoris | 4 personnes (contacts Beeper) | À choisir au premier lancement |
| Commandes maison | 4 lampes, pièces ou scènes, choisies dans la liste récupérée depuis le pont Hue | À choisir au premier lancement |
| Sources | Beeper (autorisations de l'API Beeper + accès aux notifications), pont Hue (appairage par le bouton du pont, autorisation réseau local), météo (position approximative ou ville) | Assistant de premier lancement |
| Android | Lien vers les Paramètres du téléphone | — |

## Design system d'abord

La première vague construit le module `core:designsystem` d'après le prototype. Ensuite, les écrans sont assemblés **uniquement** avec ses composants : aucun bouton, texte ou couleur brute dans les modules d'écran.

| Composant | Rôle |
| --- | --- |
| `FilTheme` et tokens | Couleurs, typographie Outfit, espacements, rayons, durées et courbes d'animation |
| `FilText` | Styles nommés : titre d'agenda, corps, libellé, discret |
| `FilIcon` | Jeu d'icônes du prototype : lampe, film, volets, flamme, bulle, maison, agenda, roue, note, lecture, pause, précédent, suivant, micro, envoyer, retour |
| `FilButton` | Primaire clair, secondaire en verre, rond à icône |
| `FilPersonTile` | Tuile pastel d'une personne + pastille de non-lus |
| `FilHomeToggle` | Commande maison ronde, allumée ou éteinte, avec nom et état |
| `FilPill` | Pastille musique, réponses rapides, puces |
| `FilSearchField` | Barre « Chercher » qui est aussi le champ |
| `FilDrawer` | Tiroir en verre : poignée, sections qui se replient, glisser haut/bas, hauteur animée |
| `FilSheet` | Le même tiroir pour la conversation, la journée, les apps média |
| `FilListRow` | Ligne de liste : apps, résultats, événements |
| `FilAlphabetRail` | Alphabet en vague |
| `FilTimeline` | Ligne du temps verticale ou horizontale |
| `FilBubble` | Bulle de message, avec « Annuler l'envoi » et son anneau |
| `FilProgress` | Barre de progression touchable |
| `FilSegmented`, `FilSwitch` | Choix et interrupteurs des réglages |
| `FilBrume`, `FilWeatherLayer` | Fond Brume et effets météo (pluie, gouttes, vent, neige, étoiles) |

**Règle « seulement le design system »**, vérifiée automatiquement à trois niveaux :

1. **Dépendances Gradle** : seuls `core:designsystem` et `app` dépendent de Compose Material ; un module d'écran qui l'ajoute fait échouer le build.
2. **Tests d'architecture (Konsist)** : échouent si un fichier d'un module d'écran importe `androidx.compose.material*`, utilise `BasicText` ou `BasicTextField`, écrit une couleur en dur (`Color(0x…)`) ou une taille de texte en dur.
3. **detekt** : règle `ForbiddenImport` avec la même liste, pour un retour immédiat dans l'éditeur.

Chaque composant a ses tests de comportement, une capture d'écran de référence par état (Roborazzi) et une entrée dans une **galerie** (écran de debug qui affiche tous les composants). Un besoin non couvert = on ajoute d'abord le composant au design system, avec ses tests, puis on l'utilise.

## Méthode de travail

Tout se passe sur `main`, sans branche, dans le dépôt public [BahuaudDimitri/Bercail-launcher](https://github.com/BahuaudDimitri/Bercail-launcher). Les contrôles tournent donc **avant** chaque push, puis la CI les rejoue.

**Dépôt public, décidé.** Un dépôt public sur GitHub gratuit a des minutes de CI illimitées (runners standards), des releases téléchargeables sans jeton (utile à la mise à jour automatique) et les règles de protection de branche. Condition : aucun secret dans le dépôt.

| Secret | Où il vit |
| --- | --- |
| Clé d'appairage du pont Hue (plus tard : jeton Home Assistant) | Saisis dans l'app au premier lancement, stockés chiffrés sur le téléphone |
| Clé de signature de l'APK | Secrets GitHub, jamais dans le code |
| `local.properties`, fichiers de clés | Exclus par `.gitignore` |
| Filet de sécurité | Protection contre les secrets de GitHub activée + gitleaks en CI |

**Tout sur main**

- Hook `pre-commit` : format (ktlint) et detekt sur les fichiers modifiés, en quelques secondes.
- Hook `pre-push` : `./gradlew check` complet (tests, couverture, architecture, captures). Pas de push si rouge.
- CI rouge sur `main` = priorité absolue : on corrige ou on annule le commit tout de suite. Aucune publication tant que `main` est rouge.
- Commits courts au format Conventional Commits (`feat:`, `fix:`, `test:`, `refactor:`, `chore:`) ; chacun compile et passe les tests. Ce format alimente le numéro de version et le journal des changements.

**Réutiliser avant de créer.** Toute demande ou proposition de fonctionnalité commence par un scan de l'existant :

1. Lire le catalogue `docs/CATALOG.md`.
2. Chercher dans le code (composants, sources de données, règles du domaine).
3. Écrire dans le plan ce qui est réutilisé et ce qui manque. On ne crée que ce qui manque.

**Savoir où est quoi**

- `docs/CATALOG.md` : une ligne par composant, écran, source de données et règle métier, avec module, fichier, rôle et tests associés. Mis à jour dans le même commit que le code.
- Un test vérifie que chaque module et chaque composant public du design system figure au catalogue ; un oubli fait échouer la CI.
- Un `README.md` court par module (rôle, ce qu'il expose, ce qu'il ne doit pas faire).
- Un `CLAUDE.md` à la racine reprend ces règles pour les agents de développement.

**Phases**

1. Précisions : réponses aux questions ouvertes de ce doc.
2. Analyses : vérifications techniques sur le Pixel 9 et le Galaxy S25 (liste dans « À vérifier pendant l'analyse », en fin de document).
3. Plan d'implémentation en vagues, chaque vague avec ses tests listés d'abord.
4. Développement vague par vague. Vague 1 : socle du projet, outils de qualité, CI, puis design system. Chaque vague se termine par une CI verte et un APK installé sur le Pixel.

## Architecture Android proposée

Une seule app Kotlin + Jetpack Compose, déclarée comme écran d'accueil, découpée en modules Gradle pour que chaque règle se teste seule. Proposition à valider pendant la phase de précisions.

Modules Gradle, en 4 couches : `app` (assemblage, launcher) → modules d'écran (un par vague) → `core:designsystem` et `core:domain` (interfaces des sources) ← modules de données (Beeper, média, agenda, Hue, météo, apps), avec `core:testing` pour les faux. Le schéma interactif est dans le doc d'origine.

Les écrans ne connaissent que le module `domain` et ses interfaces ; chaque module de données les implémente, et `testing` en fournit des versions factices pour les tests et les aperçus. Les réglages (DataStore) vivent dans `domain` derrière une interface, comme les autres sources.

**Stack**

- Kotlin, Jetpack Compose (thème propre, pas de Material par défaut), Gradle en Kotlin DSL avec catalogue de versions.
- Coroutines et Flow pour tous les flux (notifications, média, maison).
- Hilt pour l'injection, DataStore pour les réglages, OkHttp + kotlinx.serialization pour le pont Hue (HTTPS + flux d'événements) et la météo.
- AndroidX Palette pour les couleurs de pochette, `RuntimeShader` (AGSL) pour la brume.
- Version minimale : Android 16 (API 36), celle du Galaxy S25 en One UI 8.5. Compilée et ciblée pour Android 17 (API 37), celle du Pixel 9. Versions exactes des outils fixées à l'initialisation du projet.

| Source | API Android | Accès demandé |
| --- | --- | --- |
| Messages Beeper | `API Beeper (fournisseur de contenu com.beeper.api, expérimental) : conversations, historique, non-lus, envoi. NotificationListenerService + RemoteInput en complément (arrivée instantanée, photos de profil) et en secours.` | Accès aux notifications |
| Média | `MediaSessionManager` + `MediaController` | Accès aux notifications (même service) |
| Agenda | `CalendarContract` | Lecture de l'agenda |
| Apps installées | `LauncherApps` | Aucun pour un launcher (à vérifier) |
| Maison | Pont Philips Hue en local : API CLIP v2 (HTTPS, certificat du pont épinglé), flux d'événements tant que l'accueil est visible, clé obtenue par appui sur le bouton du pont. Home Assistant (WebSocket) plus tard, comme seconde source. | Internet ; jeton stocké chiffré |
| Météo | Open-Meteo (HTTP) | Internet ; ville saisie ou position approximative |
| Casque | `AudioDeviceCallback` | Aucun |
| Fond et flou | Thème qui laisse voir le fond d'écran du système, flou du tiroir par `RenderEffect` | Aucun |

## Performance et batterie (priorité absolue)

Bercail n'est gardé que s'il ne pèse pas sur la batterie. Chaque fonction se juge d'abord à son coût en énergie et en fluidité ; en cas de conflit avec l'esthétique, la batterie gagne.

**Budget** (chiffres confirmés après les mesures de l'analyse) :

- Sur une journée normale, Bercail ne consomme pas plus que Niagara (5,9 mAh mesurés le 7 octobre) + 3 mAh pour Brume, soit moins de 10 mAh/jour (0,25 % de batterie), mesuré avec batterystats.
- Écran éteint ou launcher caché : zéro travail, zéro réveil dû à Bercail.
- Retour à l'accueil et animations sans saccade (moins de 1 % d'images ratées).

**Règles** :

- Tout s'arrête quand l'accueil n'est pas visible : Brume, effets météo, connexion Home Assistant, suivi des conversations Beeper. Reprise à l'affichage, avec les dernières données en cache.
- Brume et la météo animée : 20 images/s (mesuré : aussi fluide, environ 25 % moins cher que 30 ; pas de rendu basse définition, qui coûte plus qu'il n'épargne), en demandant à l'écran de baisser sa fréquence de rafraîchissement (l'écran du Pixel 9 descend à 60, 40, 30, 24 ou 20 Hz). Image fixe quand l'économiseur de batterie est actif.
- Brume se fige après 10 secondes sans toucher l'écran, et reprend au toucher ou au changement de morceau. Ses teintes restent sombres : sur un écran OLED, des taches claires coûtent jusqu'à +50 mW d'écran. Pistes à mesurer en vague 2 pour baisser le coût CPU : animation portée par le fil de rendu (RenderThread) plutôt que par la boucle d'images de Compose.
- Ni service au premier plan, ni verrou de réveil, ni interrogation en boucle. Météo : une fois par heure au plus, plus un rafraîchissement à l'affichage si les données ont plus de 30 min. Recherche de mise à jour : une fois par jour.
- Tout est événementiel : notifications, changements de conversation Beeper, sessions média, états Home Assistant.

**Contrôles** :

- Test d'architecture (Konsist) : interdit les verrous de réveil, les services au premier plan et les alarmes exactes.
- Profils de démarrage (Baseline Profiles) et R8 en version publiée.
- Mesures Macrobenchmark sur téléphone branché, avant chaque version importante : démarrage, fluidité, et sur le Pixel 9 énergie consommée (capteurs de puissance intégrés), Brume animée contre Brume fixe. Une régression de plus de 10 % bloque la version.
- Relevé batterystats d'une journée réelle après chaque vague qui touche l'accueil.

## Stratégie de tests (TDD d'abord)

Aucun code de production n'est écrit sans un test qui échoue d'abord. Chaque règle de la section « Règles de comportement » a son test, nommé en français d'après la règle.

**Cycle imposé** : rouge (le test décrit le comportement et échoue) → vert (le minimum de code) → refactor. Un commit par cycle quand c'est possible, le test et le code dans le même commit.

| Niveau | Ce qu'on teste | Outils proposés | Où ça tourne |
| --- | --- | --- | --- |
| Unitaire pur | Règles métier : états média, résumé du tiroir, libellés maison, phrases météo, recherche, annulation d'envoi | JUnit, kotlinx-coroutines-test, Turbine (flux), AssertK | Chaque push, en quelques secondes |
| ViewModel | Ce que chaque écran expose selon les événements | Idem + faux dépôts (fakes) | Chaque push |
| Intégration Android | Lecture des notifications, sessions média, agenda, liste des apps | Robolectric | Chaque push |
| UI Compose | Gestes (tiroir, glissement Écoute, recherche), états affichés, accessibilité | Compose UI Test (sur Robolectric) | Chaque push |
| Captures d'écran | Rendu des écrans clés comparé à une référence | Roborazzi | Chaque push, échec si différence |
| Réseau | Client Home Assistant, météo | MockWebServer + JSON enregistrés | Chaque push |
| Bout en bout | Parcours complets sur un vrai Android | Tests instrumentés sur émulateur | À chaque push sur main (en CI) |

**Règles**

- Faux plutôt que mocks : chaque source (Beeper, média, agenda, Home Assistant, météo, apps) est derrière une interface avec une implémentation factice réutilisable dans les tests et les aperçus.
- Horloge injectée : aucun test ne dépend de l'heure réelle (les « moments » matin, trajet, soir deviennent des jeux de données).
- Couverture minimale sur le code métier : 90 % des lignes, mesurée par Kover, bloquante en CI. Seuil global : 80 %.
- Qualité bloquante : ktlint (format) et detekt (analyse statique), zéro avertissement.

Le porteur du projet débute en Kotlin : chaque vague de développement commencera par une courte explication des tests qu'elle ajoute, en français.

## CI/CD

GitHub Actions rejoue tous les contrôles à chaque push sur `main`, puis publie une version que les téléphones installent ensuite d'eux-mêmes, uniquement si tout est vert.

**Contrôles (gates), dans l'ordre, tous bloquants**

1. Recherche de secrets (gitleaks).
2. Format et analyse statique : ktlint, detekt (dont la règle « seulement le design system »), lint Android.
3. Architecture : tests Konsist, dépendances entre modules, test du catalogue.
4. Compilation debug et release.
5. Tests : unitaires, ViewModel, Robolectric, Compose UI, réseau.
6. Captures d'écran comparées aux références (Roborazzi).
7. Couverture (Kover) : 90 % sur le domaine, 80 % au global.
8. Tests instrumentés sur émulateurs (Android 16 et 17) en CI ; sur les vrais téléphones à la demande, en une commande, Pixel 9 ou Galaxy S25 branché en USB.

**Publication (CD), seulement après les 8 contrôles verts**

1. Numéro de version calculé à partir des commits (`feat:` = version mineure, `fix:` = correctif).
2. APK release signé, avec la clé stockée dans les secrets GitHub.
3. Release GitHub : l'APK et le journal des changements généré depuis les commits.

**Mise à jour automatique sur les téléphones** : intégrée à Bercail (voir « Décisions de la phase de précisions »). Obtainium reste la roue de secours, puisqu'il lit les mêmes releases.

**En plus**

- Un passage complet chaque nuit, même sans push, pour détecter ce qui casse avec le temps.
- Mise à jour automatique des dépendances : Renovate, seule exception à « tout sur main ». Il crée une branche éphémère, la fusionne seul dans main si la CI est verte, sans pull request. Mises à jour regroupées une fois par semaine.
- Rapports de tests, couverture et captures publiés comme artefacts de chaque passage.

## Hors périmètre de la v1

- Widgets Android tiers sur l'accueil.
- Thème clair.
- Dossiers, icônes personnalisables, packs d'icônes.
- Volet Google Discover et « At a Glance » natif (non accessibles à un launcher tiers).
- Messageries autres que via Beeper.
- Publication sur le Play Store (installation directe de l'APK sur le Pixel).

## Décisions de la phase de précisions

Tranchées le 6 octobre 2026. Elles remplacent les questions ouvertes et priment sur le reste du document en cas d'écart.

- **Nom** : Bercail partout. Le dépôt, le paquet `io.github.bahuauddimitri.bercail` et le nom sous l'icône. « Fil », dans ce document et dans le prototype, désigne Bercail ; l'interface dit « Réglages de Bercail ».
- **Appareils** : le Pixel 9 (Android 17) est l'appareil de référence, le seul visé et mesuré en v1. Le Galaxy S25 (One UI 8.5, Android 16) sera adapté plus tard ; la version minimale reste Android 16 (API 36) pour lui garder la porte ouverte, cible Android 17 (API 37).
- **Contrôles en cascade + suites séparées** (« récursivité, branches de tests ») : les mêmes contrôles reviennent à chaque étage, de plus en plus complets. Avant le commit : ktlint + detekt. Avant le push : `./gradlew check`. En CI à chaque push, et un passage complet chaque nuit. Les tests sont rangés en suites lançables séparément : unitaires, interface (Robolectric), captures (Roborazzi), instrumentés.
- **Dépendances** : Renovate, seule exception à « tout sur main ». Une branche éphémère, fusionnée seule si la CI est verte, sans pull request.
- **Langue** : code, commentaires et commits (Conventional Commits) en anglais. Noms de tests en phrases françaises (`le tiroir s'ouvre après 24 dp`). Textes de l'interface en français.
- **Mise à jour de l'app** : intégrée à Bercail, dans une vague dédiée après le design system. Une fois par jour, Bercail lit la dernière release GitHub (API publique, sans jeton), télécharge l'APK et l'installe via PackageInstaller. Autorisation « installer des apps inconnues » à donner une fois ; installation silencieuse dès que Bercail est l'installeur de sa propre app. Test de bout en bout du parcours de mise à jour en CI. Obtainium reste la roue de secours. Sur le S25, désactiver Auto Blocker ou autoriser Bercail. Clé de signature sauvegardée hors du dépôt, à un emplacement documenté. Avant cette vague : installation à la main depuis la page Releases.
- **Messages** : API Beeper comme source principale (fournisseur de contenu `com.beeper.api`) : conversations, historique, non-lus et contacts de tous les réseaux, envoi de texte dans n'importe quelle conversation. Seule la liste des conversations signale ses changements : on recharge alors les messages concernés. Limites : API marquée expérimentale, texte seulement. Constaté sur le Pixel : les notifications Beeper sont au format conversation (MessagingStyle) avec trois actions, « Répondre », « Marquer comme lu » et « Couper le son » ; Beeper publie aussi un raccourci par conversation avec la photo de profil, lisible par le launcher. Donc : photos via ces raccourcis, marquage « lu » via l'action de notification, arrivée instantanée et secours via les notifications. Le tout derrière une interface du domaine, avec des tests de contrat contre un faux fournisseur de contenu. Sans photo : pastille de couleur avec initiales. Les API officielles des messageries sont écartées (réservées aux comptes pros, ou risque de bannissement).
- **Messages, mesuré sur le Pixel** : la liste des conversations est instantanée (100 en 27 ms) mais l'historique est lent (\~1 s pour 30 messages, 9,5 s au premier accès). Donc : l'accueil n'utilise que la liste des conversations (aperçu, non-lus) ; l'historique est gardé en cache, chargé à l'ouverture d'une conversation, avec les messages des notifications affichés tout de suite. Aucune lecture d'historique en arrière-plan. Pas de colonne « lu jusqu'à » : le marquage « lu » passe par l'action de la notification.
- **Maison** : pas de serveur Home Assistant pour l'instant, seulement un pont Philips Hue (BSB002, trouvé sur le réseau local, API locale v2 disponible). La v1 parle donc **directement au pont Hue**, sans serveur ni abonnement : appairage unique par appui sur le bouton du pont, clé chiffrée sur le téléphone, certificat du pont épinglé, flux d'événements du pont écouté seulement tant que l'accueil est visible. Les 4 commandes sont des lampes, pièces ou scènes Hue choisies dans les réglages ; « Tout » ouvre l'app Hue. La zone Maison reste derrière une interface du domaine : Home Assistant pourra s'ajouter plus tard comme seconde source, sans toucher aux écrans.
- **Autorisation « réseau local »** : en visant Android 17, une app doit demander l'autorisation `ACCESS_LOCAL_NETWORK` pour joindre un appareil de la maison. Bercail la demande au moment de l'appairage du pont Hue, explique pourquoi, et la zone Maison reste utilisable (grisée, avec un bouton pour l'accorder) si elle est refusée. Couvert par des tests.
- **Météo** : Open-Meteo, à partir de la position approximative du téléphone (ou d'une ville saisie).
- **Effets météo du fond d'écran** : on garde le fond d'écran météo natif de Google (éclaircit au soleil, pluie, neige…), que tu utilises déjà. Le système le dessine quel que soit le launcher, son coût est déjà dans ta consommation actuelle ; le reconstruire dans Bercail ferait payer deux fois. Bercail ne dessine sa propre météo que sur Brume. Quand Brume couvre l'écran, Bercail cache le fond d'écran au système pour que l'effet natif s'arrête (à vérifier au prototype technique). Le coût du fond d'écran natif est relevé dans la mesure de référence.
- **Apps média** : détection automatique des apps capables de jouer du son ou de la vidéo. App principale choisie dans les réglages.
- **Sessions média** (constaté sur le Pixel) : Spotify, YouTube et YouTube Music exposent titre, artiste ou chaîne, état, position et boutons. Plusieurs sessions peuvent être en pause en même temps : Bercail affiche celle qui a joué en dernier. La télécommande n'affiche que les boutons que l'app accepte (YouTube n'a pas « précédent »).
- **Tests sur vrais téléphones** : émulateurs en CI. Avant une version importante, Pixel 9 ou Galaxy S25 branché en USB, une commande (`./gradlew connectedCheck`).

### À vérifier pendant l'analyse

- [x] Pixel 9 relié au PC (ADB) : Android 17, launcher actuel Niagara.
- [x] Notifications Beeper : format conversation, actions Répondre / Marquer comme lu / Couper le son, raccourcis avec photos.
- [x] Sessions média Spotify, YouTube, YouTube Music : métadonnées et boutons présents. Pochette Spotify confirmée au prototype (300×300 + lien).
- [x] Gestes et apps récentes avec un launcher tiers : fluides (constaté avec Niagara).
- [x] Mesure de référence (7 octobre) : 14 h 22 sur batterie, 2 h 45 d'écran, 2 133 mAh consommés sur une capacité réelle de 4 155 mAh. **Niagara : 5,9 mAh/jour (0,14 %)**, dont 5,6 pour rester en vie (écoute des notifications). Accueil visible environ 11 min/jour. Fond d'écran : image fixe (ImageWallpaper), coût négligeable.
- [x] API Beeper : autorisations OK. Conversations très rapides (100 en 27 ms ; WhatsApp, Instagram, RCS, SMS, Beeper). **Historique lent** : \~1 s pour 30 messages, 9,5 s au premier accès. Colonne « last\_read » absente en pratique ; colonnes réelles des messages : roomId, originalId, senderContactId, timestamp, isSentByMe, isDeleted, type, text\_content, image\_path/width/height, video\_path/width/height, order, reactions, displayName. Envoi non testé.
- [x] Maison : pont Hue trouvé sur le réseau (BSB002, API 1.78, API locale v2). Pas de serveur Home Assistant : la v1 parle au pont Hue en direct. Appairage réel au prototype technique.
- [x] Prototype Brume mesuré (capteurs de puissance du Pixel via Perfetto, 2 passages de 60 s par mode, mi-luminosité). Voir « Résultats des mesures » ci-dessous. Hue : appairage par bouton et flux d'événements en direct confirmés (événements groupés \~1/s, flux silencieux au repos).
- [ ] Plus tard : Galaxy S25 (gestes, batterie Samsung, Auto Blocker).

### Résultats des mesures de Brume

| Mode | Puissance du téléphone | Coût de l'animation | Images ratées |
| --- | --- | --- | --- |
| Fond d'écran seul | \~650 à 770 mW | référence | — |
| Brume fixe | \~725 mW | 0 (mais +50 mW d'écran : taches plus claires que le fond) | — |
| Brume animée 60 Hz | \~1 010 mW | \~+275 mW | — |
| Brume animée 30 Hz | \~840 à 910 mW | \~+105 à +130 mW | 0 à 0,05 % |
| **Brume animée 20 Hz** | **\~840 mW** | **\~+100 mW** | 0,08 % |
| 20 Hz, rendu basse définition | \~930 mW | pire : la couche hors écran coûte plus de CPU que ce qu'elle épargne au GPU | 0 % |

- Le coût de l'animation vient surtout du **CPU** (boucle d'images, environ +50 à +70 mW) et de la mémoire, peu du GPU (+20 mW).
- L'écran descend bien à 30 ou 20 Hz quand on le demande.
- Le fond d'écran laissé actif sous Brume ne coûte presque rien (image fixe), mais on le cache quand même.
- **Estimation journalière** : Brume n'apparaît qu'en musique, sur \~11 min d'accueil par jour. Animée à 20 Hz en permanence pendant la musique : \~2 à 4 mAh/jour, soit moins de 0,1 % de batterie.
- Mesures brutes et prototype : dossier Bercail-spike sur le PC, hors du dépôt.

## Plan d'implémentation (brouillon)

Brouillon du 6 octobre 2026, mis à jour avec les mesures du 7 octobre. À valider avant de lancer la vague 1.

**Rituel de chaque vague** :

- Au début : scan de l'existant (`docs/CATALOG.md` + recherche dans le code) pour réutiliser, puis une courte explication en français des tests que la vague ajoute.
- Pendant : TDD (un test qui échoue, le code, le test vert), petits commits sur main en Conventional Commits, catalogue mis à jour dans le même commit.
- À la fin : CI verte, release publiée, APK installé sur le Pixel. Pour les vagues qui touchent l'accueil : relevé batterystats d'une journée comparé à Niagara.

**Préfixe des composants** : `Bc` (BcText, BcButton, BcPersonTile…) remplace le préfixe `Fil` du chapitre design system. Court, et propre à Bercail.

### Vague 0 — Préparation (toi, guidé)

- [x] Dépôt GitHub : détection des secrets et protection au push activées.
- [x] Clé de signature de l'APK créée hors du dépôt, sauvegardée dans 1Password (fichier + mot de passe), rangée dans les secrets GitHub (BERCAIL\_KEYSTORE\_BASE64, BERCAIL\_KEYSTORE\_PASSWORD, BERCAIL\_KEY\_ALIAS, BERCAIL\_KEY\_PASSWORD).
- [ ] Renovate : installé en vague 1, une fois sa configuration poussée sur main (évite sa branche d'accueil).

### Vague 1 — Socle, qualité et CI

Aucune fonction visible : on construit la chaîne qui protège tout le reste.

- Projet Gradle multi-modules selon l'architecture (`app`, `core:domain`, `core:design`, `core:testing`), catalogue de versions, plugins de convention partagés. Les autres modules naissent avec leur vague.
- ktlint, detekt (dont `ForbiddenImport`), lint Android avec avertissements bloquants, Konsist (règles d'architecture, règle « seulement le design system », interdits batterie), Kover avec ses seuils.
- Hooks Git versionnés (avant commit : ktlint + detekt ; avant push : `./gradlew check`), installés par une tâche Gradle.
- GitHub Actions : les 8 contrôles à chaque push, gitleaks, passage nocturne complet avec émulateurs Android 16 et 17, version tirée des Conventional Commits, APK signé publié en release. Renovate configuré.
- Documentation : `CLAUDE.md`, `docs/CATALOG.md` + son test de cohérence, README par module, prototypes copiés dans `design/prototype/`, ce document exporté dans `docs/`.
- Une app Bercail minimale, déclarée comme launcher, qui affiche un écran sombre vide.
- **Fini quand** : un push sur main déclenche les 8 contrôles, une release signée apparaît, et l'APK s'installe sur le Pixel.

**Terminée le 9 octobre 2026** : release signée [v0.1.0](https://github.com/BahuaudDimitri/Bercail-launcher/releases/tag/v0.1.0) publiée par la CI et installée sur le Pixel. Module nommé `core:designsystem`. Écarts et leçons :

- Instrumentés : l'apostrophe droite est interdite dans les noms de tests Android (format DEX) ; on écrit `’`, une règle d'architecture le vérifie.
- Robolectric sur Android 17 : Espresso 3.7.0 imposé et accès aux internes du JDK ouvert.
- L'émulateur Android 17 de la CI démarre lentement : 4 cœurs, 4 Go, 25 min de délai.
- Un job émulateur vert ne prouve rien si l'APK ne s'est pas installé : du 10 au 14 octobre, l'image Android 17 manquait de disque, Gradle avalait l'erreur et le job passait au vert avec zéro test. Depuis, l'émulateur a 4 Go de disque et un job sans test exécuté est rouge (`require-device-tests.sh`).
- Les journaux de la CI demandent un compte GitHub connecté : les échecs de tests sont donc aussi publiés en annotations publiques.
- detekt 2.0 (alpha) : la 1.23 ne suit pas Kotlin 2.4.
- Reste : installer l'app Renovate sur le dépôt.

### Vague 2 — Design system

- Jetons (couleurs, police Outfit, formes, mouvements), `BcTheme`, puis les composants du chapitre design system, un par un, chacun avec ses tests d'interface et ses captures Roborazzi.
- Brume : shader AGSL et effets météo en composants, avec les règles batterie (20 Hz, figée après 10 s sans toucher, arrêt hors écran, image fixe en économie d'énergie), et mesure d'énergie de la variante portée par le fil de rendu.
- Galerie de debug qui montre chaque composant dans tous ses états.
- **Fini quand** : la galerie sur le Pixel ressemble au prototype, et la règle « seulement le design system » est active en CI.

**Terminée le 9 octobre 2026** : jetons, `BcTheme` et 25 composants sur `main`, chacun avec ses tests et ses captures (≈ 95 tests), galerie vérifiée sur le Pixel, règle « seulement le design system » active en CI. **Brume mesurée sur le Pixel** (`scripts/measure-brume.sh`, 2 × 60 s, mi-luminosité) : immobile 765 mW, animée 837 mW, soit **+71 mW** (le prototype à 20 Hz : +100 à +120 mW), écran à 20 Hz, images ratées 0,4 à 0,7 %. Surcoût surtout GPU (+23 mW), CPU inchangé : la variante portée par le fil de rendu n'est pas nécessaire. Estimation : environ 1 mAh/jour (au pire 3,4 mAh), dans le budget. Écarts et leçons :

- Brume est dessinée avec des dégradés, comme le prototype, plutôt qu'avec un shader AGSL : même rendu, testable en capture. Son coût réel se mesure sur le Pixel (mode « Brume plein écran » de l'app de debug) ; la variante portée par le fil de rendu ne sera essayée que si la mesure dépasse le budget.
- Pas de flou derrière le verre du tiroir : le fond d'écran est dessiné par le système, hors de portée d'un flou Compose, et flouter Brume à chaque image coûterait de la batterie. Verre teint seul, à revoir en vague 10.
- Le lint Android a attrapé deux coûts évitables (décalage recomposant à chaque image, largeur d'écran au lieu du conteneur) : corrigés.
- Captures avec texte identiques sous Windows et Linux : pas de seuil de tolérance nécessaire.
- L'émulateur Android 17 plante parfois sur GitHub (adb, code 224) : un second essai automatique, seulement si aucun test n'a tourné.
- Fréquence de l'écran : sur le Pixel 9, l'écran ne descend à 20 Hz qu'avec la préférence de la fenêtre ET le vote de la vue ; l'un sans l'autre, ou le vote de calque Compose, laisse 60 Hz. Pendant un toucher et la seconde qui suit, la fréquence normale revient pour que le tiroir reste fluide (testé).
- Les captures de Brume passées par l'écran simulé sortaient parfois blanches (rendu matériel simulé, dépendant du temps) : Brume est capturée en la dessinant directement dans une image.

### Vague 3 — Accueil avec données factices

- Écran d'accueil complet : en-tête météo et prochain rendez-vous, ligne du temps verticale, tiroir et ses gestes (seuil 24 dp), personnes, maison, pastille média, modes d'écran.
- Toutes les sources viennent des faux de `core:testing` : on valide le comportement sans aucune API réelle.
- **Fini quand** : l'accueil se comporte comme le prototype, toutes les règles de comportement ont leur test.

**Terminée le 10 octobre 2026** : l'accueil (écrans Accueil et Écoute, tiroir) tourne sur le Pixel avec les données du prototype, release [v0.6.0](https://github.com/BahuaudDimitri/Bercail-launcher/releases/tag/v0.6.0) publiée par la CI, 307 tests au total (environ 190 de plus). Mesuré sur le Pixel : écran à 20 Hz pendant que Brume bouge ; après 10 s sans toucher, 0 image dessinée sur l'Accueil et environ 3 par seconde sur l'écran Écoute (la progression du morceau) ; accueil caché, 0 image. Le fond d'écran du système est bien retiré sous Brume. Écarts et leçons :

- **Périmètre.** La vague livre les deux écrans de l'accueil et le tiroir. Les feuilles « Aujourd'hui », apps média et conversation, et la recherche, restent dans leurs vagues (6, 7, 8 et 4) : leurs boutons existent et sont testés, mais n'ouvrent encore rien. La barre « Chercher » est à sa place, inactive.
- **Un écran = un module.** Nouveau module `:feature:home`, qui ne voit que le domaine et le design system. L'app branche les sources dans `HomeSources.kt` : seule l'heure est réelle (`DeviceClock`, qu'Android prévient à chaque minute), les autres sont les faux de `core:testing` jusqu'à leur vague. Hilt attendra la première vraie source.
- **Batterie.** L'égaliseur de la pastille se fige avec Brume après 10 s (sinon il redessinait l'écran 20 fois par seconde tant que la musique jouait). La position du morceau n'est suivie que sur l'écran Écoute. Le relevé batterystats d'une journée est reporté à la vague 4, quand Bercail sera le launcher du quotidien.
- **Phrase météo simplifiée** : « Pluie ce matin », « Nuit claire ». Les précisions du prototype (« jusqu'à 16 h », km/h) viendront avec les vraies prévisions, en vague 6. Quatre moments : matin 5 h à 12 h, après-midi 12 h à 18 h, soir 18 h à 20 h, nuit 20 h à 5 h.
- **Pastels des personnes** : les favoris prennent les cinq pastels dans l'ordre (deux favoris n'ont jamais la même couleur), les autres en reçoivent un fixe d'après leur identifiant.
- **Réglages** : le modèle existe avec ce que l'accueil lit (favoris, ligne du temps, tiroir au départ, fond sans musique) et ses valeurs par défaut ; l'écran des réglages et leur stockage restent en vague 4.
- **Design system** : quatre composants ajoutés (ligne de résumé, points de pagination, grande pochette, ligne météo) ; le tiroir sans poignée devient la télécommande et laisse la barre de navigation du téléphone sous son contenu. Sans image de pochette (vague 7), la pochette est un dégradé de ses deux couleurs.
- **Pas encore vu sur le téléphone** : le retour du fond d'écran quand la musique s'arrête, car le faux lecteur joue toujours. Testé sur l'écran simulé seulement ; à revoir en vague 7.
- **Noms des tests instrumentés** : Android refuse aussi la virgule, ce qui a rendu la CI rouge une fois. La règle d'architecture n'accepte plus que lettres, chiffres, espaces, tiret et `’`, et `./gradlew check` construit maintenant l'APK de ces tests.

### Vague 4 — Apps, recherche et réglages : Bercail devient ton launcher

- Apps installées (LauncherApps), favoris, recherche plein tiroir avec alphabet en vague, « Réglages de Bercail » (DataStore).
- **Fini quand** : tu remplaces Niagara par Bercail au quotidien. Première vraie comparaison de batterie sur une journée.

**Développée le 10 octobre 2026**, pas encore close : release [v0.8.0](https://github.com/BahuaudDimitri/Bercail-launcher/releases/tag/v0.8.0) publiée par la CI (émulateurs Android 16 et 17 verts, recherche et réglages compris), 430 tests au total. **Reste à faire pour la clore** : installer la dernière release, choisir Bercail comme écran d'accueil, puis relever la batterie sur une journée face à Niagara. Vérifié sur le Pixel le 10 octobre avec la version de mise au point : liste et icônes des vraies apps, clavier, lancement d'une app, réglage appliqué tout de suite, app favorite en tête de la recherche, 0 image dessinée au repos. Trois défauts vus à cette occasion et corrigés : l'accueil transparaissait derrière la recherche, la recherche derrière les réglages, et le clavier restait sorti sous les réglages. Écarts et leçons :

- **Plus de fausses données dans la version publiée.** Les sources pas encore branchées (agenda, météo, messages, maison, musique) montrent leur état vide : « Plus rien aujourd'hui · Journée libre », « Aucun message », « Maison au repos », « Écouter… ». Les données du prototype restent dans la version de mise au point (debug), pour tout voir et tout essayer ; la version publiée n'embarque aucun faux.
- **Vraies sources** : apps installées (`:data:apps`, écoutées seulement quand la recherche est ouverte, sans l'autorisation de voir tous les paquets) et réglages enregistrés (`:data:settings`, DataStore). La liste des apps passe par `PackageManager`, plus simple à tester ; `LauncherApps` ne sert qu'à être prévenu des installations.
- **Vraies icônes des apps** dans la liste, dessinées une fois hors du fil principal et gardées (120 au plus) ; la tuile à initiale du prototype reste en secours.
- **Recherche** (`:feature:search`) : la barre ouvre la recherche quand le doigt se lève, pas au focus (Android peut rendre le focus au champ après fermeture). Le haut de l'accueil garde sa mise en page sous le tiroir qui monte. La touche « rechercher » du clavier ouvre la première app trouvée (ajout). La section « Messages » des résultats viendra avec la vague 8.
- **Réglages** (`:feature:settings`) : seulement ce qui marche déjà — fond sans musique, ligne du temps, tiroir au départ, apps favorites (liste à cocher, absente du prototype), liens vers l'écran d'accueil par défaut et les Paramètres. Favoris, commandes maison, app média et écoute automatique arriveront avec leurs vagues.
- **Bouton Accueil** : il ramène toujours à l'accueil simple (réglages et recherche fermés, page Accueil).
- **Voile sur le fond d'écran** (ajout) : sans Brume, un léger assombrissement et le même voile sombre en haut gardent l'agenda lisible sur n'importe quelle image, comme dans le prototype.
- **« Journée libre » avant 18 h**, « Soirée libre » ensuite, quand il ne reste aucun rendez-vous.
- **Leçons de test** : les captures d'un composant qui dépend du temps se font en dessinant directement dans une image (le voile sortait parfois blanc via l'écran simulé) ; les modules de données ont leur propre convention de build, sans Compose.

### Vague 5 — Mise à jour intégrée

- Placée tôt, car dès la vague 4 tu utilises Bercail tous les jours : chaque vague suivante arrive alors toute seule sur le téléphone.
- Lecture quotidienne de la dernière release, téléchargement, installation via PackageInstaller, test de bout en bout du parcours.

**Développée le 11 octobre 2026**, pas encore close : release [v0.9.0](https://github.com/BahuaudDimitri/Bercail-launcher/releases/tag/v0.9.0) publiée par la CI, environ 540 tests au total (une centaine de plus). **Reste à faire pour la clore** : voir la prochaine version arriver seule, sans rien demander (ce sera la première release de la vague suivante). Fait le 11 octobre sur le Pixel : v0.9.0 installée à la main, puis v0.10.0 téléchargée et installée par Bercail lui-même, après l'accord et l'autorisation « installer des applis inconnues » donnés une fois ; Bercail est désormais son propre installeur. La recherche annonce une version qui attend un accord sous « Réglages de Bercail » (ajout, v0.10.0). Vérifié sur le Pixel le 11 octobre : le parcours complet sur le vrai Android 17 (lecture, téléchargement, vérification, remise à l'installeur d'Android, refus d'un paquet qui n'est pas Bercail), et Android exige bien un accord tant que l'autorisation n'est pas donnée. Écarts et leçons :

- **Au rythme de l'accueil, sans tâche de fond.** La lecture a lieu quand l'accueil s'affiche (une fois par jour ; une heure après un échec), jamais à heure fixe : ni tâche programmée ni alarme, une règle d'architecture l'interdit. Le téléchargement s'interrompt si l'accueil est caché et reprend au retour.
- **Installation quand l'accueil est caché.** Android redémarre Bercail en l'installant : autant que personne ne regarde l'accueil. Si Android veut un accord (la première fois), Bercail n'insiste pas et n'affiche rien de lui-même.
- **Ligne « Mise à jour » dans les Réglages de Bercail** (ajout) : la version installée, où en est la suivante, et un toucher pour vérifier, télécharger ou installer. C'est par elle que passe la première installation, avec l'écran d'accord d'Android.
- **Sécurité.** API publique de GitHub, sans compte ni jeton. Seul un paquet hébergé dans les releases du dépôt, en https, est accepté ; sa taille et son empreinte SHA-256 sont vérifiées ; Android vérifie ensuite la signature et refuse un paquet plus ancien ou qui n'est pas Bercail. Https seulement dans la version publiée (règle d'architecture).
- **Une panne de la mise à jour ne fait jamais tomber l'accueil** : elle est notée dans le journal du téléphone, et Bercail laisse passer une heure avant de réessayer seul.
- **La version de mise au point ne se met pas à jour** : elle n'a pas de numéro de version, et sa signature n'est pas celle des releases.
- **Test de bout en bout** sur émulateur en CI et sur téléphone, avec un petit serveur local qui joue GitHub. Le paquet servi n'est pas Bercail (se remplacer en plein test arrêterait le test) : le parcours va jusqu'au refus d'Android. L'installation réussie se vérifie avec deux vraies releases, à la clôture.
- **Nouveau module** `:data:update`, et première utilisation du réseau par Bercail.

### Vague 6 — Agenda et météo

- Agenda réel (CalendarContract), tiroir « Aujourd'hui ». Météo Open-Meteo (une fois par heure au plus), phrases météo selon l'heure.

**Développée le 11 octobre 2026**, pas encore close : trois releases publiées par la CI, [v0.11.0](https://github.com/BahuaudDimitri/Bercail-launcher/releases/tag/v0.11.0) (agenda et feuille « Aujourd'hui »), [v0.12.0](https://github.com/BahuaudDimitri/Bercail-launcher/releases/tag/v0.12.0) (météo) et [v0.13.0](https://github.com/BahuaudDimitri/Bercail-launcher/releases/tag/v0.13.0) (choix d'une ville), environ 700 tests au total (160 de plus). **Vu sur le Pixel** (version de mise au point) : le 11 octobre, avant l'accord, l'accueil affiche « Relier l'agenda » ; après l'accord, la feuille « Aujourd'hui » liste les rendez-vous du jour ; les 11 tests instrumentés passent. Le 12 octobre, la ligne météo s'affiche (« 19° · Bruine jusqu'à 22 h »), pour la position du téléphone. **Défaut météo du 12 octobre** : deux fois (12 h 56 et 20 h 07), la requête vers Open-Meteo a échoué au moment où l'accueil s'affichait, sans laisser de trace, et Bercail attendait alors un quart d'heure : d'où l'accueil sans météo. La position était connue ; la cause exacte de l'échec n'a pas pu être reproduite (cinq essais réussis ensuite, dont un après sept minutes en arrière-plan). Deux garde-fous publiés en [v0.13.1](https://github.com/BahuaudDimitri/Bercail-launcher/releases/tag/v0.13.1) : un second essai cinq secondes après un échec, et une trace qui dit pourquoi une requête échoue (`adb logcat -s Bercail`, jamais la position). **Reste à faire pour la clore** : donner les autorisations sur la version publiée et y vérifier l'agenda et la météo ; si la ligne manque encore, lire la trace. Le relevé batterie d'une journée se fera avec celui de la vague 4. Écarts et leçons :

- **Rien n'est lu avant ton accord.** Sans le droit de lire l'agenda, l'accueil n'annonce pas une fausse « Journée libre » : il affiche « Relier l'agenda », et la feuille de la journée propose le bouton. Bercail ne pose la question d'Android que lorsqu'on touche ce bouton (dans la feuille ou dans les réglages), jamais de lui-même. Si Android ne demande plus (après deux refus), Bercail ouvre sa page dans les Paramètres du téléphone. Pas d'assistant de premier lancement pour l'instant : la feuille et les réglages en tiennent lieu.
- **Agenda** (`:data:agenda`) : les rendez-vous qui commencent aujourd'hui, de tous les comptes de l'agenda du téléphone ; ni refusés, ni annulés, ni ceux d'un agenda masqué. Les événements « journée entière » sont listés en tête de la feuille mais ne sont jamais annoncés comme « le prochain rendez-vous » ni marqués sur la ligne du temps. Android prévient des changements ; rien n'est lu accueil caché. Lecture seule : l'autorisation d'écrire n'est pas demandée.
- **Feuille « Aujourd'hui »** : la date en toutes lettres, un bouton vers l'app agenda, les rendez-vous (passés atténués, prochain mis en avant). Toucher un rendez-vous l'ouvre dans l'app agenda. Un rendez-vous trouvé par la recherche ouvre la feuille sur lui. Ce que la feuille couvre s'efface (pas de flou, voir vague 2), l'en-tête reste.
- **Météo, rythme (écart)** : une requête par heure au plus, et seulement quand l'accueil est affiché. Après un échec, un second essai cinq secondes plus tard (l'accueil s'affiche au moment où le téléphone se réveille, quand le réseau n'est pas toujours revenu) ; après deux échecs, un quart d'heure d'attente. Le rafraîchissement « à l'affichage après 30 min » prévu au départ est abandonné : Bercail garde la prévision heure par heure, la ligne suit donc l'heure sans nouvelle requête, et chaque requête réveille le réseau. La prévision est gardée sur le téléphone et montrée six heures au plus ; au-delà, mieux vaut ne rien dire. Aucune tâche de fond : ce sont les minutes de l'horloge qui déclenchent.
- **Phrases météo** tirées des heures à venir : « Pluie fine jusqu'à 16 h », « Averses attendues vers 17 h » (dans les six heures), « Vent 35 km/h, rafales à 50 » (à partir de 30 km/h, ou 50 en rafales), « Ciel dégagé, 17° cet après-midi » le matin, « Nuit claire » la nuit seulement. La pluie passe avant le vent, le vent avant le ciel.
- **Lieu et vie privée** : la position approximative du téléphone (jamais la précise, cette autorisation n'est pas déclarée), ou une ville cherchée par son nom dans les réglages. La position n'est envoyée à Open-Meteo qu'arrondie à un kilomètre environ, sans identifiant. Elle vient d'abord de ce qu'Android sait déjà ; au-delà de six heures, une seule question au téléphone, par le réseau, jamais par le GPS. Avec une ville choisie, aucune position n'est lue. Le nom tapé ne part que lorsqu'on demande la recherche.
- **Version de mise au point** : elle lit maintenant le vrai agenda et la vraie météo, comme la version publiée ; seuls les messages, la maison et la musique restent ceux du prototype.
- **Nouveaux modules** `:data:agenda` et `:data:weather` ; les règles (phrases, rythme) sont dans le domaine, testées sans téléphone.
- **Leçons de test** : le téléphone simulé répond « oui » tout seul aux demandes d'autorisation ; la logique de la réponse est donc testée à part, avec une réponse que le test contrôle. Les réponses d'Open-Meteo (prévisions pour Paris, recherche de « Nantes ») sont enregistrées dans le dépôt et rejouées par un serveur local.

### Vague 7 — Média et écran Écoute

- Sessions média (via l'écoute des notifications), règle « dernière session jouée », boutons selon l'app, écran Écoute, couleurs de Brume tirées de la pochette, tiroir des apps média. Détails des pochettes à confirmer par le prototype.

**Développée le 12 octobre 2026**, pas encore close : la vraie musique sur l'accueil puis le tiroir des apps média, publiés ensemble par la CI en [v0.14.0](https://github.com/BahuaudDimitri/Bercail-launcher/releases/tag/v0.14.0), environ 800 tests au total (une centaine de plus). **Vu sur le Pixel** le 12 octobre (version de mise au point, accès donné par toi) : la pastille et l'écran Écoute montrent la session que le téléphone classe en premier, avec sa vraie pochette, Brume dans ses teintes, la position et les trois boutons ; « Ouvrir dans Spotify » amène Spotify au premier plan ; les Réglages reconnaissent six apps média (NotebookLM, Prime Video, Spotify, Twitch, YouTube, YouTube Music) ; les 11 tests instrumentés passent, dont un de bout en bout avec une vraie session média. **Reste à faire pour la clore** : donner l'accès sur la version publiée (Android répondra sans doute d'abord « Paramètre restreint », voir plus bas), essayer la feuille « Écouter » et son bouton lecture (pas essayés sur le téléphone : il faut qu'aucune app n'ait de contenu chargé, et le bouton lance du son), puis le relevé batterie d'une journée avec celui de la vague 4. Écarts et leçons :

- **Un accès, mais aucune notification lue.** (Complété en vague 8 : le service reste relié tant que l'accueil est affiché, pour savoir qu'un message arrive, et ne regarde que le nom de l'app qui notifie.) Android ne dit ce qui joue qu'à une app qui a l'« accès aux notifications ». Bercail le demande seulement quand tu touches « Autoriser dans Android » (feuille « Écouter ») ou la ligne « Musique en cours » des Réglages, et ouvre alors directement sa propre page dans les réglages d'Android. Son service se détache d'Android dès qu'il est relié : aucune notification ne lui est livrée et rien ne le garde éveillé. Vérifié sur le Pixel : Android le range parmi les écouteurs « en sommeil », et les sessions restent lisibles. La vague 8 (messages) décidera si elle a besoin des notifications de Beeper, et le dira.
- **« Paramètre restreint ».** Pour une app installée hors du Play Store, Android grise cet accès : il faut ouvrir la page de Bercail dans les Paramètres du téléphone, le menu aux trois points, « Autoriser les paramètres restreints », puis recommencer. La feuille l'explique et offre le bouton vers cette page. Non rencontré avec la version de mise au point (installée par câble) ; à constater sur la version publiée.
- **Rien accueil caché.** Android prévient de chaque changement (session ouverte ou fermée, morceau, pause) : ni minuterie ni boucle. Mesuré sur le Pixel : le téléphone compte un écouteur de sessions de plus quand l'accueil de Bercail est affiché, et plus aucun dès qu'il est caché. Seule la position d'un contenu en lecture est regardée une fois par seconde, télécommande affichée.
- **Quelle session.** Celle qui joue, sinon celle qui a joué en dernier : c'est l'ordre dans lequel Android donne les sessions. Une session arrêtée ou sans titre ne compte pas.
- **Boutons selon l'app.** La télécommande n'a que les boutons que l'app accepte ; un bouton absent laisse sa place vide, Lecture reste au centre. Un direct n'a pas de barre de progression. « Précédent » ne revient au début (après 5 s) que si l'app sait se déplacer dans le contenu ; sinon il passe au morceau d'avant.
- **Pochette et couleurs.** L'image donnée par l'app, copiée et réduite à 512 points au plus ; ses deux couleurs pour Brume sont la plus présente, puis la plus présente de celles qui s'en détachent (noir et blanc exclus, oranges compris : l'outil Palette les écarte par défaut). Sans pochette : un dégradé, et Brume garde les couleurs du moment.
- **Spotify** ajoute une mention après l'artiste (« • À partir de la lecture aléatoire intelligente ») : elle est retirée. Une vidéo sans artiste connu s'affiche avec son seul titre.
- **« Ouvrir dans … »** passe par le chemin que l'app propose vers ce qui joue (son lecteur, sa vidéo), sinon l'ouvre comme par son icône.
- **Feuille « Écouter »** (le tiroir des apps média) : l'app principale en tête, mise en avant, puis les autres apps qui savent jouer : celles qui répondent au bouton lecture d'un casque, ou déclarées apps de musique. Toucher une app l'ouvre. L'app principale se choisit dans les Réglages (Spotify par défaut, sinon la première).
- **« Reprendre « … » » (écart).** Bercail retient le dernier contenu qu'il a vu chargé, et l'app principale propose de le reprendre si c'était le sien. Son bouton lecture appuie sur la touche lecture du téléphone, comme le ferait un casque : Android la remet à l'app qui a joué en dernier, sans que Bercail puisse en viser une autre. Si rien ne joue deux secondes plus tard, Bercail ouvre l'app. Un launcher n'a pas le droit de relancer directement une app précise sans l'ouvrir.
- **Pas fait : l'écoute automatique** (casque connecté → écran Écoute). Elle figure dans le tableau des réglages mais pas dans la ligne de cette vague : à décider, ici ou en vague 10.
- **Version de mise au point** : elle montre maintenant la vraie musique ; seuls les messages et la maison restent ceux du prototype.
- **Nouveau module** `:data:media` ; la règle « quelle session » et les lignes de la feuille sont dans le domaine, testées sans téléphone. Une image (icône, pochette) traverse le domaine en pixels bruts (`Picture`).
- **Leçons de test** : de fausses apps média ouvrent leurs sessions dans le téléphone simulé ; un test instrumenté ouvre une vraie session sur un vrai Android (il se donne l'accès lui-même sur un émulateur, jamais sur un vrai téléphone, où il est sauté sans ton accord). Un test qui écoute sans jamais s'arrêter bloque toute la suite : chaque écoute est maintenant arrêtée avec son test.

### Vague 8 — Messages

- API Beeper (conversations, historique, envoi), notifications (arrivée instantanée, « Marquer comme lu »), raccourcis de conversation (photos), tiroir de conversation, annulation d'envoi 6 s, brouillons. Comportement exact de l'API à confirmer par le prototype.

**Développée les 12 et 13 octobre 2026**, pas encore close : les vraies conversations de Beeper dans le tiroir, la feuille de conversation, puis les messages dans la recherche, publiés par la CI en [v0.15.0](https://github.com/BahuaudDimitri/Bercail-launcher/releases/tag/v0.15.0), puis deux correctifs ([v0.15.1](https://github.com/BahuaudDimitri/Bercail-launcher/releases/tag/v0.15.1), [v0.15.2](https://github.com/BahuaudDimitri/Bercail-launcher/releases/tag/v0.15.2)), environ 970 tests au total (170 de plus). **Vu sur le Pixel** les 12 et 13 octobre (version de mise au point, accès donné par toi sur l'écran d'Android) : le tiroir montre les vraies conversations et leurs non-lus ; une conversation s'ouvre avec ses vrais messages, ses trois réponses rapides et son champ, qui reste au-dessus du clavier ; trois tests tournent contre le vrai Beeper (100 conversations sur quatre réseaux, les 30 derniers messages bien rendus, une recherche en 0,3 s). **Pas vérifié sur le téléphone** : l'arrivée d'un message une fois corrigée (voir plus bas), faute d'« accès aux notifications » sur la version essayée ; l'envoi (je n'ai envoyé aucun message). **Reste à faire pour la clore** : relier Beeper et donner l'« accès aux notifications » sur la version publiée, vérifier qu'un message reçu s'affiche aussitôt, envoyer toi-même un premier message depuis Bercail, essayer « Annuler l'envoi » et la dictée, puis conclure l'essai des photos de profil (les deux autres points « à décider » ont été tranchés le 13 octobre, voir ci-dessous). Écarts et leçons :

- **Accès.** Beeper donne deux droits à une autre app, lire et envoyer. Bercail les demande ensemble, seulement quand tu touches « Relier Beeper » (tiroir ouvert) ou la ligne « Messages de Beeper » des Réglages ; c'est la question ordinaire d'Android. Rien ne quitte le téléphone : Beeper est interrogé sur place. Sans Beeper installé, Android refuse sans rien afficher et Bercail ouvre sa page dans les Paramètres : à soigner si Bercail doit un jour servir sans Beeper.
- **Ce que l'accueil lit.** Les cent conversations qui ont bougé en dernier, sans les archivées ni celles que tu as rangées en « moins importantes ». Une conversation en sourdine ne compte ni dans le résumé ni dans « Tous » ; choisie comme favorite, elle garde sa tuile et son badge. Vérifié contre le vrai Beeper : quatre réseaux, tous nommés (WhatsApp, Instagram, Google Messages, Beeper).
- **Favoris (écart).** Quatre au plus, groupes compris, choisis dans les Réglages (« Personnes favorites »). Tant que tu n'as pas choisi, les tuiles sont les quatre dernières conversations sans sourdine, pour que le tiroir serve tout de suite.
- **Rien accueil caché.** Ni minuterie ni boucle. Mesuré sur le Pixel : Bercail n'observe Beeper que lorsque son accueil est affiché, et plus du tout dès qu'il est caché.
- **Arrivée d'un message (défaut signalé par toi le 13 octobre, corrigé en v0.15.2).** La documentation de Beeper dit qu'il prévient quand sa liste de conversations change. Mesuré sur le Pixel : en quatre minutes la liste a changé cinq fois et Beeper n'a prévenu d'aucun de ces changements ; la pastille ne bougeait donc qu'au retour sur l'accueil. Correctif : chaque notification que Beeper affiche ou retire sert de signal pour relire la liste (tout de suite, puis une seconde après). Bercail ne regarde que le nom de l'app qui notifie, jamais la notification ; son service n'est relié à Android que tant que l'accueil est affiché, et se détache dès qu'il est caché. Cela demande l'« accès aux notifications » (le même que pour la musique) : sans lui, la liste n'est relue qu'au retour sur l'accueil, et les Réglages le disent.
- **Feuille de conversation.** Les trente derniers messages, lus seulement tant que la feuille est ouverte (une à deux secondes sur le Pixel), gardés en mémoire pour la réouverture et jamais écrits dans un fichier. Beeper ne donne que du texte : une photo ou une vidéo s'annonce « Pièce jointe ». Dans un groupe, chaque bulle nomme son auteur ; les tiennes sont signées « Toi ».
- **Envoi et « Annuler l'envoi ».** Un message envoyé s'affiche tout de suite mais ne part qu'après six secondes ; « Annuler l'envoi » le reprend et rend son texte au champ. Un second envoi, ou la fermeture de la feuille, le fait partir aussitôt. Si Beeper ne le prend pas, la bulle disparaît, le texte revient dans le champ et la feuille le dit.
- **Réponses rapides (retirées le 13 octobre, à ta demande).** Tu ne t'en sers jamais et tu les trouves impersonnelles : la feuille n'a plus que les messages, le champ et la dictée. Elles étaient trois, fixes (« D'accord », « J'arrive », « Je te rappelle »).
- **Dictée.** Le micro ouvre la saisie vocale d'Android ; ce qui est entendu s'ajoute au champ. Bercail n'écoute jamais lui-même et ne demande pas le droit au micro.
- **« Lu » (écart, tranché le 13 octobre : on en reste là).** Beeper n'offre aucun moyen de lui dire qu'une conversation a été lue. Bercail note donc, pour lui seul, que tu l'as ouverte : son badge et le résumé s'effacent dans Bercail jusqu'au message suivant, mais le badge de Beeper et sa notification restent. La voie prévue au départ, l'action « Marquer comme lu » de la notification, demande que Bercail lise les notifications de Beeper (leurs boutons), et plus seulement le nom de l'app comme aujourd'hui. Mesuré sur le Pixel le 13 octobre : chaque notification de Beeper nomme sa conversation et porte ce bouton, c'est donc faisable. Mais il faudrait lire davantage des notifications, ton correspondant verrait « lu » sans que tu aies ouvert Beeper, et cela ne marcherait que tant que la notification est encore affichée. Jugé disproportionné : pas fait, à rouvrir si le badge resté dans Beeper te gêne.
- **Photos de profil (écart, à décider).** Pas faites : les tuiles montrent l'initiale sur son pastel. Les raccourcis de Beeper portent un numéro sans lien avec l'identifiant de la conversation ; il faudrait les rattacher par le nom, ce qui est fragile, et Android ne les donne qu'au launcher par défaut. Pour la même raison, le bouton « Beeper » de la feuille ouvre Beeper, pas la conversation précise.
- **Recherche.** À partir de trois lettres, et une fois la frappe en pause (0,3 s), Beeper est interrogé sur les messages qui contiennent ce qui est tapé ; les cinq plus récents s'affichent sous « Messages ». Une personne ou un message trouvé ouvre la conversation.
- **Clavier.** Une feuille prend 62 % de l'écran au plus ; clavier sorti, elle monte à 94 %, sinon il ne restait la place que d'une bulle (constaté sur le Pixel, corrigé).
- **Version de mise au point** : elle lit maintenant le vrai Beeper ; seule la maison reste celle du prototype.
- **Nouveau module** `:data:messages` ; nouveaux composants `BcReplyField` (champ de réponse) et `BcUndoCountdown` (l'anneau qui se vide).
- **Leçons de test** : Beeper est joué par un faux fournisseur de contenu, à la même adresse et avec les mêmes colonnes ; un test instrumenté (`MessagesDeviceTest`) interroge le vrai Beeper, seulement là où il est relié, et ne relève que des dates et des compteurs, aucun nom ni message. Les règles de la conversation ont leurs tests écrits avant le code, mais plusieurs n'ont pas été vus échouer avant d'être verts.

### Vague 9 — Maison (Hue)

- Autorisation réseau local, recherche et appairage du pont, 4 commandes choisies dans les réglages, état en direct tant que l'accueil est visible.

**Reportée le 13 octobre 2026 (décision de Dimitri)** : la maison n'est pour l'instant pilotée que par Google Home, et un passage à Home Assistant est prévu. La vague sera reprise après ce passage, et son contenu précisé à ce moment-là. D'ici là, la version publiée garde l'état vide de la maison, et la version de mise au point celle du prototype.

### Vague 10 — Finitions et v1.0

- Effets météo sur Brume, profils de démarrage, mesures Macrobenchmark et énergie, accessibilité (TalkBack, tailles de police), relevé batterie final, version 1.0.
- **Ajouté le 12 octobre 2026 (demande de Dimitri)** : retravailler l'animation de l'alphabet de la recherche (la ligne de lettres), dans l'esprit de celle de Niagara Launcher, mise au point ensemble sur le téléphone ; puis une passe sur toutes les animations pour que l'ensemble soit le plus fluide possible. Les règles batterie restent la limite (rien ne bouge accueil caché, animations continues à 20 images/s au plus).
- **Alphabet de la recherche, fait le 13 octobre 2026.** Dimitri a montré l'effet de Niagara sur un enregistrement de son écran : la colonne de lettres se courbe vers la gauche autour du doigt, sur une dizaine de lettres de chaque côté, sans que les lettres grossissent, et une bulle porte la lettre atteinte. Bercail fait de même, avec un tic sous le doigt à chaque lettre ; la colonne se redresse et la bulle s'efface quand le doigt se lève. Essayé par Dimitri sur le Pixel (version de mise au point) et validé tel quel. Deux différences avec Niagara, gardées : la colonne occupe toute la hauteur de la liste, et la liste défile jusqu'à la lettre au lieu de n'afficher que ses apps. Le mouvement est bref et ne tourne que sous le doigt : rien à voir avec la limite de 20 images/s des animations continues.
- **Alphabet, correctif du 13 octobre (signalé par Dimitri).** Clavier sorti, la dernière lettre était coupée en deux : la colonne écrasait les lettres qui ne tenaient plus. Elles sont maintenant réparties sur la hauteur disponible, chacune entière, et se serrent quand la place manque (v0.17.1).
- **Ligne du temps sous le doigt, faite le 13 octobre 2026.** Un toucher ouvre la journée sur le rendez-vous le plus proche du doigt. En glissant le long de la ligne, elle se courbe autour du doigt comme l'alphabet (même geste, même courbe, partagés dans le design system), une bulle nomme le rendez-vous le plus proche (« 18:30 · Courses »), avec un tic à chacun ; lâcher ouvre la journée sur lui. Un jour sans rendez-vous, elle se courbe sans bulle et ouvre simplement la journée. Dans les deux réglages ; couchée, elle se soulève moins (32 dp au lieu de 72) parce que l'en-tête est juste au-dessus. Seule la ligne et environ 48 dp autour prennent le doigt : sur les heures écrites à côté, l'accueil garde ses gestes. Essayée par Dimitri sur le Pixel (version de mise au point) et validée telle quelle. Leçon : le tiroir ne réagit qu'aux gestes qui commencent sur lui ; le seul conflit possible était le changement d'écran, pour la ligne couchée.
- **Glisser la ligne des messages, fait le 13 octobre 2026.** Tiroir replié, glisser « 2 non lus · … » vers la droite fait suivre la ligne sous le doigt et écrit « Ouvrir Beeper » à sa place ; passé 64 dp, le texte s'éclaircit avec un tic, et lâcher ouvre Beeper. Trop court ou vers la gauche, la ligne revient. Le toucher et le glissement vertical ouvrent toujours le tiroir. La ligne de la maison ne glisse pas tant que la vague Maison est reportée ; celle des messages non plus tant que Beeper n'est pas relié. Essayé et validé par Dimitri sur le Pixel.
- **Voile au toucher, fait le 13 octobre 2026 (demande de Dimitri : « regarde les marges lorsqu'un élément est cliqué »).** Bercail n'avait pas son propre effet de toucher : il montrait le rectangle sombre par défaut, collé au texte et à angles droits. Il a maintenant un voile clair à coins ronds, là tout de suite et qui s'efface au lâcher : autour d'une ligne nue (résumé, en-tête, tuile, commande maison) il déborde de 10 dp sur les côtés et de 4 dp en haut et en bas ; il prend la forme de ce qui en a une (bouton, rangée, pastille) ; il est sombre sur le bouton clair ; l'interrupteur et les points de pagination n'en ont pas. Essayé et validé par Dimitri sur le Pixel.
- **Glisser la pastille musique, fait le 14 octobre 2026.** Glisser la pastille vers la droite la fait suivre le doigt et écrit « Ouvrir Spotify » (le nom de l'app qui joue) à sa place ; lâcher assez loin ouvre cette app. Vers la gauche, le doigt reste à l'accueil, qui passe à l'écran Écoute comme avant ; le toucher y mène toujours aussi. Même geste que la ligne des messages, partagé dans le design system. Essayé et validé par Dimitri sur le Pixel.
- **La météo sur le fil, décidée le 14 octobre 2026, à développer.** La ligne météo quitte l'en-tête : la ligne du temps verticale prend la couleur du ciel prévu, porte les températures et se prolonge jusqu'au réveil du lendemain, marqué par un anneau aligné sur la pastille musique. Décisions, spécification, règles et plan en quatre étapes : `docs/passation-meteo-sur-le-fil.md` ; maquette : `design/prototype/meteo-sur-le-fil.html`.
- **Retours à l'usage, recueillis le 14 octobre 2026, à préciser.** Une vingtaine de points rangés en 7 lots, chacun à préciser en séance avant tout développement : `docs/roadmap-retours-usage.md`.
- **Lancée le 13 octobre 2026**, les points traités un par un, chacun discuté avec Dimitri avant d'être développé. **Ajoutés ce jour-là, à discuter** : une aide pour les trajets (idée encore ouverte) ; les gestes de l'accueil, par exemple glisser sur la ligne « 2 non lus » pour ouvrir Beeper directement ; la ligne du temps touchable, avec le même effet que l'alphabet.

## Annexe : lire un test Kotlin

Les tests sont ton outil de contrôle : leur nom dit la règle vérifiée, en français, et la CI dit si elle tient. Voici comment en lire un sans connaître Kotlin.

```kotlin
class DrawerGestureTest {

    @Test
    fun `le tiroir s'ouvre après 24 dp de glissement`() {
        val drawer = DrawerState(isOpen = false)   // 1. Préparer
        drawer.onDrag(25.dp)                       // 2. Agir
        assertThat(drawer.isOpen).isTrue()         // 3. Vérifier
    }
}
```

- `class …Test` : une boîte qui regroupe les tests d'un même sujet.
- `@Test` : « ce qui suit est un test ».
- Le nom entre accents graves est la règle, écrite en français. C'est la ligne la plus importante.
- Le corps suit toujours trois temps : préparer une situation, faire une action, vérifier le résultat. `assertThat(x).isEqualTo(y)` se lit « je vérifie que x vaut y ».

Un test d'interface se lit de la même façon :

```kotlin
@Test
fun `toucher Chercher agrandit le tiroir jusqu'en haut`() {
    compose.setContent { HomeScreen(state = fakeHome()) }   // afficher l'écran avec des données factices
    compose.onNodeWithText("Chercher").performClick()       // toucher « Chercher »
    compose.onNodeWithTag("drawer").assertIsFullHeight()    // vérifier
}
```

- `fake…` : des fausses données ou de fausses sources (faux Beeper, fausse horloge) pour tester sans téléphone ni réseau, toujours avec le même résultat.
- Les tests de capture (Roborazzi) photographient un composant et comparent l'image à celle de référence : un pixel qui bouge sans raison fait échouer la CI, et l'image de différence montre où.
- Les règles d'architecture (Konsist) se lisent comme des phrases : `aucun fichier des écrans n'importe androidx.compose.material`.

Pour lancer les tests toi-même : `./gradlew test`. Dans GitHub, l'onglet Actions montre une pastille verte ou rouge par push ; en cliquant sur une étape rouge, le nom du test qui échoue dit quelle règle est cassée.
