# Bercail — Feuille de route : retours à l'usage

Établie le 14 octobre 2026 à partir des retours de Dimitri après quelques jours d'usage quotidien. Elle complète `docs/handoff.md` (vagues 0 à 10) et `docs/passation-meteo-sur-le-fil.md`.

## Règle du jeu

1. **Rien ne se développe sans être précisé.** Avant chaque lot, une séance de précision reprend chaque point, un par un. On y répond à ses questions, on y tranche les options, et on dessine une maquette si besoin. Le point passe alors de « À préciser » à « Précisé ». Les décisions sont écrites ici, dans la fiche du point, puis le développement commence.
2. **On avance lot par lot, dans l'ordre**, sauf décision contraire en séance.
3. **On livre par petits commits sur main**, selon `CLAUDE.md` : TDD, Conventional Commits, `docs/CATALOG.md` à jour. Pas de grosse PR. Chaque `feat:` publie une version, que la mise à jour intégrée livre sur le téléphone. On teste donc à l'usage au fil de l'eau.
4. **Le budget batterie reste la priorité absolue** : moins de 10 mAh/jour, pas de polling, animations à 20 fps au plus. Tout point qui ajoute du mouvement ou du réseau passe par une mesure Perfetto avant d'être gardé.

Statuts : `À préciser` → `Précisé` → `En cours` → `Livré` (ou `Abandonné`, avec la raison, ou `Déplacé` vers un autre point).

## Vue d'ensemble

| Lot | Thème | Points | Nature |
|---|---|---|---|
| 0 | Réparer d'abord | 0.1 à 0.2 | Correction |
| 1 | Ménage rapide | 1.1 à 1.9 | Mécanique |
| 2 | Gestes et transitions | 2.1 à 2.4 | Mécanique + réglage fin |
| 3 | Accueil : infos | 3.1 à 3.3 | Maquette avant code |
| 4 | Musique | 4.1 à 4.2 | Investigation sur téléphone |
| 5 | Messages | 5.1 à 5.3 | Investigation + mesure |
| 6 | Direction artistique | 6.1 à 6.7 | Exploration, maquettes, mesures |

---

## Lot 0 — Réparer d'abord

### 0.1 Corriger les erreurs — `Livré`

**Livré le 14 octobre 2026** : `main` verte (run 38588075694), 14 tests exécutés sur l'émulateur Android 16, Android 17 signalé, release [v0.18.0](https://github.com/BahuaudDimitri/Bercail-launcher/releases/tag/v0.18.0) publiée par la CI avec les quatre commits de la vague 10 en attente. Reste à toi : laisser Bercail l'installer sur le Pixel.

- **Retour :** « Fix les erreurs ».
- **Constat (séance du 14 octobre, lu dans les runs GitHub Actions des 13 et 14 octobre) :**
  - Android 16 n'est pas instable : c'est une régression, rouge cinq fois de suite depuis « let a finger run along the line of the day ». Ce commit a donné à la ligne du temps l'action « Voir la journée », que l'en-tête portait déjà. Le test « toucher l'en-tête ouvre la feuille de la journée et sa poignée la referme » trouve deux éléments au lieu d'un ; `Failed to inject touch input` n'est que l'habillage du message.
  - Android 17 était un faux vert depuis au moins le 10 octobre : l'APK ne s'installe pas sur l'image (`not enough space`), Gradle avale l'erreur et le job passe au vert avec zéro test exécuté. Le plantage adb (code 224) de la nuit est un aléa distinct, déjà couvert par le second essai.
  - Les trois tests Beeper mis en pause par hypothèse (« Beeper is not linked on this phone ») apparaissent en échec dans les annotations publiques, même sur les runs verts.
  - Quatre commits attendent une CI verte pour être publiés depuis v0.17.0 : un `fix` de recherche et trois `feat` d'accueil.
- **Décisions :**
  - Android 16 : le test vise l'en-tête par son rôle de bouton. Rien ne change dans l'app ; la ligne du temps garde son action.
  - Android 17 : l'émulateur reçoit 4 Go de disque pour l'APK, le script attend le service paquets avant Gradle, le second essai part dès qu'aucun test n'a tourné, et un job émulateur devient rouge quand aucun test instrumenté n'a tourné. Plus de vert sans test.
  - Les tests mis en pause par hypothèse ne sont plus annotés en échec.
  - Rien à signaler à l'usage pour l'instant : le point se limite à la CI et à la publication des commits en attente.
- **Découverte en cours de route (14 octobre, après la correction) :** Android 17 n'avait **jamais** exécuté un test en CI (0 test dès le premier run vert disponible, le 9 octobre). Une fois les tests lancés, 13 sur 14 échouent pour une raison d'environnement : l'activité est cherchée dans le paquet du test (`…bercail.debug.test/MainActivity`) au lieu de celui de l'app. Les mêmes APK passent sur Android 16. Décision : le job Android 17 tourne et affiche ses échecs sans bloquer la publication, et l'enquête devient le point 0.2.
- **Fini quand :** `main` est verte, Android 16 avec ses tests exécutés et Android 17 signalé, et la release qui suit est installée sur le Pixel.

### 0.2 Faire parler l'émulateur Android 17 — `À préciser`

- **Constat (14 octobre) :** sur l'image « 37.0 » de la CI, avec AGP 9.4.1 et androidx.test 1.7.0, `ActivityScenario` construit l'intention avec le paquet du test et Android ne la résout pas. Les deux tests de mise à jour échouent aussi, autrement (`Waiting` au lieu de `Ready` ou `UpToDate`), sans doute pour la même racine. Rapports : artefact `device-reports-android-17` du run 38586618075.
- **Pistes :**
  - Lancer `./gradlew connectedCheck` sur le Pixel 9 branché (vrai Android 17) : si tout passe, l'image de l'émulateur est en cause, pas le téléphone.
  - Comparer avec une image Android 17 plus récente, ou avec `target: default` au lieu de `google_apis`.
  - Chercher une version d'androidx.test ou d'AGP qui connaît l'API 37.
- **À préciser :** le temps qu'on accepte d'y mettre, et si le Pixel peut être branché à un poste avec le SDK pour la première piste.

## Lot 1 — Ménage rapide

Séance de précision du lot 1 : 14 octobre 2026, avec Dimitri.

### 1.1 Supprimer l'option « Barre horizontale » — `Livré`

**Livré le 14 octobre 2026** : le choix a quitté les Réglages et la ligne couchée le code ; un ancien réglage « horizontal » est ignoré, puis effacé au premier réglage changé. Essayé et validé par Dimitri sur le Pixel (version debug).

- **Retour :** « Supprimer l'option Barre horizontale ».
- **Décision (14 octobre) :** on supprime tout le code, pas seulement l'option. Le choix disparaît des Réglages ; `TimelineOrientation.Horizontal` quitte le domaine et les réglages ; la branche horizontale disparaît de `HomeHeader.kt`, de `BcTimeline` (avec ses captures) et de `BcGallery`. Un réglage déjà enregistré en horizontal est lu comme vertical. À faire avant la météo sur le fil (3.1), qui n'aura plus qu'une ligne à gérer.

### 1.2 Retirer « Faire un audio » — `Livré`

**Livré le 14 octobre 2026** : le micro « Dicter » a quitté le champ de réponse, avec la dictée qui allait derrière ; le champ garde le texte et l'envoi. Poussé sans essai sur le Pixel, à la demande de Dimitri.

- **Retour :** « Je ne l'utilise pas ».
- **Constat :** Bercail n'enregistre aucun vocal. Le seul bouton avec un micro est « Dicter », dans le champ de réponse d'une conversation (`BcReplyField`, `Dictation.kt`) : il lance la dictée d'Android, qui écrit le message en texte, sans autorisation micro pour Bercail.
- **Décision (14 octobre) :** c'est ce bouton : on retire le micro et la dictée. Le champ de réponse garde le texte et l'envoi.

### 1.3 Pas d'automatisation détectée : retirer la ligne — `Abandonné`

- **Raison (14 octobre) :** la phrase n'existe pas dans Bercail ; Dimitri la voit dans une autre app. Rien à faire ici.

### 1.4 Recherche : toujours les apps avant les gens — `Livré`

**Livré le 16 octobre 2026** : en partant de la barre, les familles montent dans l'ordre Apps, Gens, Messages, Agenda, Maison, puis Réglages ; dans chaque famille, le titre puis les résultats dans leur ordre, de haut en bas (apps de A à Z, agenda dans l'ordre des heures). Figé par des tests de position ; Entrée ouvre toujours la première app trouvée. Poussé sans essai sur le Pixel.

- **Constat :** les résultats partent de la barre de recherche et montent. Aujourd'hui, en partant de la barre : Réglages, Agenda, Maison, Messages, Gens, et les apps tout en haut, le plus loin du pouce.
- **Décision (14 octobre) :** en partant de la barre : Apps, Gens, Messages, Agenda, Maison, puis Réglages au bout. Figé par un test. La touche Entrée ouvre toujours la première app.
- **Décision (16 octobre) :** dans chaque famille, le titre puis les résultats dans leur ordre, de haut en bas — choix de Dimitri le 16 octobre. La meilleure app est donc en haut du groupe « Apps », sous son titre.

### 1.5 Agenda : ouvrir directement quand il n'y a rien — `Livré`

**Livré le 16 octobre 2026** : quand plus rien n'est à venir aujourd'hui (journée vide, rendez-vous tous passés ou déjà commencés, ou seulement des journées entières), toucher l'en-tête ouvre l'app agenda du téléphone, à la date du jour ; tant qu'un rendez-vous est à venir, la feuille s'ouvre comme avant. Glisser l'en-tête vers la droite ouvre toujours l'app agenda, avec « Ouvrir l'agenda » écrit à sa place pendant le geste et proposé aux lecteurs d'écran (nouveau composant `BcSwipeBlock`, sur le glissement de `BcRowSwipe`). L'agenda pas encore relié garde la feuille, qui propose de l'autoriser ; la feuille « Rien de prévu aujourd'hui » reste accessible par la ligne du temps. Pas encore essayé sur le Pixel.

- **Retour :** « Si 0 events, au clic sur agenda, ouvrir directement l'agenda (et/ou au swipe) ».
- **Décisions (14 octobre) :**
  - Journée vide : toucher l'en-tête ouvre l'app d'agenda du téléphone, au lieu de la feuille « Rien de prévu aujourd'hui ». Avec des rendez-vous, le toucher ouvre la feuille comme aujourd'hui.
  - Glisser l'en-tête vers la droite ouvre l'app d'agenda, toujours, avec ou sans rendez-vous. C'est le geste de la ligne des messages et de la pastille musique (`BcRowSwipe`, « Ouvrir l'agenda ») : glisser à droite = ouvrir l'app.
  - L'app d'agenda est celle du téléphone (intention `CalendarContract`), pas une app choisie dans Bercail.
- **Décisions (16 octobre) :**
  - Agenda autorisé et plus aucun rendez-vous à venir aujourd'hui : le toucher ouvre l'app d'agenda ; sinon, la feuille. Les journées entières du jour ne comptent pas comme à venir ; un rendez-vous déjà commencé non plus (comme pour « Plus rien aujourd'hui » dans l'en-tête).
  - Agenda pas encore autorisé : le toucher ouvre la feuille, qui propose de l'autoriser.

### 1.6 Pouvoir changer le fond d'écran — `Déplacé au lot 6`

- **Décision (14 octobre) :** Dimitri veut revoir des fonds façon Brume plutôt qu'un simple accès au sélecteur d'Android. Le point devient 6.6. Rien n'est ajouté aux Réglages en attendant.

### 1.7 Chrome facilement accessible — `Livré`

- **Constat :** « Chercher sur le web » n'apparaît que quand la recherche ne trouve rien, et ouvre l'app Google (`ACTION_WEB_SEARCH`), pas Chrome. Chrome peut déjà être mis en app favorite dans les Réglages.
- **Décidé (14 octobre) :** Chrome s'ouvre tel qu'il a été laissé, comme en touchant son icône.
- **Décision (15 octobre, séance 6.1) :** un **dock** de cinq icônes au trait, sans libellé, dans le bandeau replié de l'accueil, juste au-dessus de la barre « Chercher » : les apps favorites des réglages, cinq au plus, dans l'ordre choisi, Chrome en tête. Icône réelle de l'app rendue en monochrome dans un anneau (pas de glyphes dessinés à la main, pour tenir toutes les apps). Présent sur l'accueil seulement, pas dans la feuille ouverte ni sur Écoute. Le réglage « Apps favorites » devient « Dock ». Livré par la vague 3 de 6.1.
- **Livré le 16 octobre 2026** par la vague 3 de 6.1 (décisions, commits et ce qui reste à voir sur le Pixel : voir 6.1) : Chrome se met dans le dock depuis le réglage « Dock », un toucher l'ouvre tel qu'il a été laissé, un toucher long montre ses raccourcis (« Nouvel onglet »…).

### 1.8 Packs d'icônes pour le dock — `Livré`

- **Décidé (20 octobre) :** nouveau réglage « Pack d'icônes », dans la section « Bandeau » sous « Dock » : « Aucun » (par défaut), puis les packs d'icônes installés, chacun par le nom de son app (une app qui répond à `org.adw.launcher.THEMES`, le standard ADW/Nova). Le pack choisi s'applique au dock seulement : la recherche, la feuille « Écouter » et le menu des raccourcis gardent leurs icônes. Pour chaque app du dock, son activité de lancement est cherchée dans l'`appfilter` du pack (dans `res/xml` ou dans les assets, les deux sont lus) ; le dessin trouvé est rendu en trait blanc, au même cadre et à la même taille que le dessin d'aujourd'hui (environ 28 dp dans sa place de 44 dp). Une app que le pack ne dessine pas garde son dessin d'une seule couleur, inchangé. Un pack désinstallé ramène à « Aucun » sans rien dire. Batterie : l'`appfilter` est lu une fois par choix de pack ou par mise à jour du pack, jamais à chaque image ; les dessins sont gardés comme ceux du dock, refaits quand le pack ou l'app change ; rien ne tourne accueil caché.
- **Livré le 20 octobre 2026** : `dbacf0d` (lecture des packs dans `:data:apps`, `IconPack` et `packGlyph` dans le domaine : un pack aux traits clairs, comme Lines, garde ses traits blancs et perd son ombre sombre), `16867f1` (le réglage et sa liste), `1099db0` (le dock dessiné par le pack choisi), `5c19f67` (lint). Vu sur le Pixel (version de mise au point, le même dock que la version publiée) : Lines Free dessine 4 des 5 apps du dock (Chrome, Contacts, Transit, Mihon) ; la cinquième, absente de son `appfilter`, garde son dessin d'une seule couleur, plus plein que les traits de Lines.

### 1.9 Une icône au choix pour chaque app du dock — `Livré`

- **Décidé (20 octobre) :** dans Réglages › Dock, toucher une app du dock (la ligne, hors de son interrupteur et de sa poignée) ouvre une page pour choisir son icône ; le toucher long du dock sur l'accueil ne change pas (raccourcis seulement). La page : un champ de recherche par nom d'icône (sans majuscules ni accents : « musique », « mail »), puis une grille de toutes les icônes de tous les packs installés. En tête, deux choix : « Icône du pack » (par défaut : ce que donne le pack choisi, sinon le dessin de l'app) et « Dessin d'origine » (le dessin d'une seule couleur de l'app, sans pack). Puis « Proposées » (les icônes que l'`appfilter` d'un pack donne à cette app, puis celles qui portent son nom), puis toutes les icônes, pack par pack (le nom du pack en titre), de A à Z. Le choix en cours est marqué de l'accent ; toucher une icône la choisit et ramène au dock. Les icônes sont dessinées en trait blanc, comme dans le dock. Les listes viennent du `drawable.xml` de chaque pack (dans `res/xml` ou les assets), sinon des dessins nommés dans son `appfilter` ; elles ne sont lues qu'à l'ouverture de la page, jamais pour l'accueil ; les petits dessins se font au fil du défilement, gardés en nombre limité ; rien ne tourne une fois la page fermée. Le choix est gardé par app (pack et nom du dessin, ou « origine » ; par défaut « pack ») et le dock le suit aussitôt. Si le pack du dessin choisi est désinstallé ou n'a plus ce dessin, l'icône du pack revient sans rien dire, le choix restant gardé. Accessibilité : chaque icône est lue par son nom (tirets bas changés en espaces) ; la page a sa flèche retour.
- **Livré le 20 octobre 2026** : `9f78c54` (catalogue des packs et dessin choisi dans `:data:apps`, `DockIcon`, `PackIcon`, `iconChoices` dans le domaine), `038d5f7` et `d9939cb` (la grille `BcIconGrid` du design system, et le panneau qui la laisse défiler seule), `08a8e1e` (la page de choix et le réglage enregistré), `c8269f9` (le dock suit le choix), `fb6c3b4` (catalogue). Vu sur le Pixel (version de mise au point) : Lines Free liste 2 981 icônes (son `drawable.xml` ; 3 267 entrées, des icônes revenant dans plusieurs catégories) ; le défilement rapide de la grille reste fluide (2,6 % d'images en retard, 90 % des images en moins de 20 ms, en version de mise au point) ; « google maps » choisi pour Chrome l'a redessiné aussitôt dans le dock, puis « Icône du pack » lui a rendu son dessin de Lines.
- **Retouche du 20 octobre 2026** (`a8a825c`) : sous chaque app du dock, la ligne dit « Place N · toucher pour changer l'icône » au lieu de « Place N » ; sur les écrans étroits (360 et 320 dp de large), elle passe sur deux lignes au lieu d'être coupée. « Proposées » ne change pas.

## Lot 2 — Gestes et transitions

### 2.1 Glisser vers le bas pour fermer les « modales » — `Livré`

**Livré le 17 octobre 2026** (`cac781b` décisions, `3744337` design system, `416dedc` accueil, `f7ed249` recherche) : les quatre feuilles du bas et la recherche plein écran se tirent vers le bas sous le doigt ; lâchées au-delà du tiers (de la feuille, ou de l'écran pour la recherche) ou d'un coup sec vers le bas, elles finissent de descendre et se ferment ; avant, elles remontent, sans rebond, à 20 images par seconde au plus. Les listes de la journée, d'« Écouter » et de la recherche défilent d'abord, puis le même geste tire le verre ; la conversation ne se tire que par son en-tête. La recherche fermée ainsi vide le champ et range le clavier dès que le doigt lâche ; le bandeau replié remonte alors à sa place depuis le bas de l'écran. Poussé sans essai sur le Pixel (aucun téléphone branché).

- **Ce que ça donne au doigt :** la feuille colle au doigt dès qu'il a bougé d'un peu plus d'un millimètre (le seuil de toucher d'Android, 8 dp sur le Pixel). Pour qu'elle parte d'elle-même, il faut la descendre d'un tiers de sa hauteur : environ 190 dp, soit 3 cm, pour une feuille à sa plus grande taille (62 % de l'écran), moins pour une feuille courte. Un geste vif vers le bas suffit aussi, même court : plus vite qu'une hauteur de feuille par seconde, soit environ 9 cm par seconde pour la plus grande. La recherche demande un geste plus long : le tiers de l'écran, environ 300 dp, soit 5 cm.
- **Choix faits au code, à revoir si Dimitri le souhaite :**
  - Pendant qu'on tire la recherche, ce qui apparaît derrière le verre est le fond (Brume ou le fond d'écran) : l'accueil, effacé pendant la recherche, ne revient qu'une fois la recherche fermée, en fondu, pendant que le bandeau replié remonte du bas de l'écran.
  - La feuille « gens et maison » tirée vers le bas laisse voir dessous l'emplacement vide du bandeau ; le bandeau revient en fondu une fois la feuille fermée, comme avant.
  - La conversation se tire par la poignée et toute la zone d'en-tête (nom, réseau, bouton « Beeper ») ; un toucher sur « Beeper » l'ouvre toujours, un glissement qui part de lui tire la feuille sans l'ouvrir.
- **Tests :** `BcSheetPullUiTest` (suit le doigt, revient lâchée avant le tiers sans rebond, se ferme au-delà ou d'un coup sec, la liste défile d'abord puis tire la feuille, un glissement sur les messages ne ferme jamais, 20 images par seconde au plus, plus rien ne bouge une fois posée), `BcBandSearchPullUiTest` (la recherche), `BcHeldTransitionUiTest` (le seuil au tiers), `HomeSheetPullUiTest` (la journée, « Écouter », la conversation dans l'accueil), `HomeSearchUiTest` et `SearchPullUiTest` (la vraie liste de recherche) ; capture `sheet_pulled` (une feuille tirée à mi-chemin).
- **Pas vérifié sur le Pixel** (aucun téléphone branché) : le geste au doigt sur les quatre feuilles et sur la recherche, et la sensation du seuil et du coup sec ; la fluidité à 20 images par seconde pendant qu'on tire, et le verre givré qui reste aligné sur Brume pendant qu'il descend ; la liste de la journée et celle d'« Écouter » qui passent la main à la feuille une fois en haut ; la liste de recherche (toutes les apps, puis les résultats) qui passe la main au verre, et l'alphabet qui ne tire jamais la recherche ; le clavier qui s'en va quand on lâche la recherche ; le bandeau replié qui remonte après la recherche ; la conversation avec un long historique, qui ne doit jamais se fermer en remontant les messages ; Brume figée pendant qu'on tire et repartie une fois fermé ; TalkBack (la poignée « Fermer la feuille », « Fermer » au-dessus, le geste retour).

- **Piste :** ajouter le geste une fois dans `BcDrawer` et `BcPanel`, pour que toutes les feuilles en profitent. Seuil et vitesse à régler, avec un test du seuil comme pour le tiroir (24 dp).
- **Constat (17 octobre) :** les feuilles du bas (`BcSheet` : conversation, journée, « Écouter » ; `BcBandSheet` : la feuille « gens et maison ») se ferment d'un coup dès 24 dp de glissement vers le bas, mais les listes qui défilent dedans (la journée, « Écouter », la conversation) prennent le geste pour elles : il ne marche pas sur elles. La recherche plein écran n'a aucun glissement pour se fermer.
- **Décisions (17 octobre, avec Dimitri) :**
  - La feuille suit le doigt : le verre descend sous le doigt ; lâcher au-delà d'un seuil, ou d'un coup sec vers le bas, la ferme ; sinon elle remonte à sa place, sans rebond. Même mécanique que la transition d'Écoute (2.4), 20 images par seconde au plus, rien ne tourne au repos. Pour toutes les feuilles du bas : conversation, journée, « Écouter » et la feuille « gens et maison ». L'ouverture de la feuille « gens et maison » en glissant vers le haut sur le bandeau ne change pas.
  - Feuilles avec une liste (journée, « Écouter ») : tant que la liste peut remonter, glisser vers le bas la fait défiler ; une fois en haut, le même geste continue en tirant la feuille vers le bas (défilement imbriqué, comme dans les apps Android).
  - Conversation : elle ne se ferme en glissant que depuis son en-tête (la poignée, le nom de la personne et la zone d'en-tête) ; glisser sur les messages fait toujours défiler l'historique, jamais fermer.
  - Recherche : glisser vers le bas ferme la recherche plein écran quand sa liste est en haut ou que rien ne défile, en suivant le doigt de la même façon, et le clavier s'en va avec elle. Fermer, c'est la fermeture d'aujourd'hui : champ vidé, comme la flèche. Les pages des Réglages (`BcPanel`) ne changent pas.
- **Choix techniques par défaut (orchestrateur, 17 octobre ; à revoir si Dimitri le souhaite) :** le seuil est le tiers de la hauteur de la feuille (pour la recherche, le tiers de l'écran), ou un coup sec vers le bas plus rapide qu'environ une hauteur de feuille par seconde ; lâchée avant, elle remonte. La règle des 24 dp disparaît pour fermer (le bandeau la garde pour ouvrir). L'accessibilité ne change pas : la poignée reste un bouton « Fermer la feuille », le geste retour et le toucher au-dessus ferment toujours. Brume reste figée tant qu'une feuille est ouverte, pendant qu'on la tire aussi ; elle repart une fois la feuille fermée.
- **Fini quand :** les tests le montrent (suit le doigt, revient lâchée trop tôt, se ferme au-delà du tiers ou d'un coup sec, la liste défile d'abord puis tire la feuille, un glissement sur les messages ne ferme jamais, 20 images par seconde au plus, plus rien ne tourne une fois posée), avec une capture d'une feuille tirée à mi-chemin, puis à l'usage sur le Pixel.

### 2.2 Double tap + glisser sur la barre verticale : rechercher une app — `À préciser`

- **Piste :** un double tap suivi d'un glissement ouvre la recherche, et le glissement parcourt les lettres comme `BcAlphabetRail`. Le doigt levé ouvre l'app ou la lettre.
- **À préciser :**
  - Ce que fait exactement le glissement : choisir une lettre, ou faire défiler la liste.
  - Le retour visuel pendant le geste.
  - La cohabitation avec la bulle de toucher de la ligne du temps et l'anneau du réveil.
  - À faire après la météo sur le fil, qui modifie la même barre.

### 2.3 Transitions d'ouverture des apps — `À préciser`

- **Piste :** `ActivityOptions.makeClipRevealAnimation` ou `makeScaleUpAnimation`, à partir des bornes de l'icône ou de la ligne touchée. Ça ne coûte rien en batterie. L'animation « Pixel » complète, où l'icône se transforme en fenêtre, est réservée au launcher système (QuickStep) et n'est pas atteignable.
- **À préciser :** le style voulu, et le retour vers Bercail (animation de fermeture).

### 2.4 Transitions entre les écrans de Bercail — `À préciser`

- **Retour (13 octobre) :** la vidéo Pinterest « Expandable Tab Bar » : un élément qui s'agrandit et devient l'écran suivant, au lieu d'un fondu. Ajouté au lot 2 le 14 octobre.
- **Pistes :** `SharedTransitionLayout` de Compose, sur un seul élément d'abord : la pastille musique qui devient la pochette de l'écran Écoute, les lignes du résumé qui deviennent les tuiles du tiroir, ou une tuile de personne qui s'ouvre en conversation. Animation courte, puis arrêt : rien ne tourne au repos.
- **Décidé (15 octobre, séance lecteur) :** la première transition est Accueil ⇄ Écoute, dessinée sur la planche 4 de 6.1 : tenue par le doigt (glisser vers la gauche pour entrer, vers le haut pour sortir), l'arche de la pochette se déroule du bord haut pendant que le fil se rétracte dans le même bord, le héros part à gauche, le bandeau descend, Brume change de couleurs ; elle se pose à 20 images/s au plus, sans rebond, et se joue à l'envers pour sortir. Codée par la vague 5 de 6.1.
- **Livré le 17 octobre 2026** par la vague 5 de 6.1 (commits, détails et ce qui reste à voir sur le Pixel : voir 6.1, « Vague 5 livrée ») : Accueil ⇄ Écoute suit le doigt dans les deux sens, se pose du côté le plus proche (ou de celui où on l'a lancé), et se joue seul en 400 ms au toucher de la pastille, au geste retour, par l'action du lecteur d'écran et quand la musique s'arrête. La planche image par image ne se lisait plus : la transition suit la description écrite ci-dessus.
- **À préciser :** les autres transitions (résumé → tiroir, personne → conversation), et leur style.

## Lot 3 — Accueil : infos

### 3.1 Manque la météo — `À préciser`

- **Piste :**
  - Couvert par `docs/passation-meteo-sur-le-fil.md`, déjà spécifiée et prête à développer.
  - Vérifier en plus pourquoi la météo n'apparaît pas aujourd'hui : position, ville non réglée, ou erreur Open-Meteo.
- **À préciser :** est-ce une absence (rien ne s'affiche) ou un manque de place et de lisibilité ?

### 3.2 Manque les trajets — `À préciser`

- **Pistes :**
  - Simple et sans clé : si le prochain événement de l'agenda a un lieu, un bouton « Y aller » ouvre Google Maps en navigation.
  - Un vrai temps de trajet demande une API payante (Routes API) avec une clé. C'est exclu : pas de secret dans ce dépôt public.
- **À préciser :**
  - Quels trajets : domicile-travail, prochain rendez-vous, transports en commun à Nantes.
  - Quelle info : durée, heure de départ, perturbations.
  - Où l'afficher.

### 3.3 Tirer parti de la barre verticale — `À préciser`

- **Pistes à maquetter :**
  - Lever et coucher du soleil.
  - Progression du morceau en cours.
  - Points pour les messages non lus, placés à leur heure d'arrivée.
  - Batterie restante en bout de ligne.
  - Météo et réveil, déjà prévus.
- **À préciser :** les pistes à garder et à maquetter, dans l'esprit de la piste C, sans surcharger la ligne.

## Lot 4 — Musique

### 4.1 Lecteur Spotify mieux intégré, surtout via Sonos — `À préciser`

- **Retour :** « Un peu sloppy, notamment quand Spotify est connecté directement à Sonos ».
- **Pistes :**
  - Détecter la lecture à distance (`MediaController.PlaybackInfo`, type REMOTE). La pastille affiche alors l'enceinte (« Salon ») et le volume pilote Sonos (`setVolumeTo`).
  - Plus ambitieux : l'API locale Sonos sur le réseau local. L'autorisation est déjà demandée pour Hue.
- **À préciser :**
  - Ce qui est « sloppy », précisément : pochette, état, latence des boutons, volume, session qui disparaît.
  - Captures et constat sur le Pixel, avec Spotify en local et Spotify sur Sonos.

### 4.2 Changer d'app musicale facilement — `Livré`

**Livré le 17 octobre 2026** (`2c2aef6` design system, `7ed7de0` accueil) : un appui long sur la pastille musique, en Lecture comme en Pause, ouvre la feuille « Écouter » sans quitter l'accueil ; toucher une app l'ouvre, le bouton lecture de l'app principale relance, et l'app média principale des réglages ne change pas. Un lecteur d'écran trouve l'action « Apps d'écoute » sur la pastille. Toucher et glisser la pastille font ce qu'ils faisaient. Tests : `BcMusicPillSwipeUiTest` (l'appui long, et les glissements lents qui ne le déclenchent jamais), `HomeListenUiTest` (la feuille, en Lecture et en Pause). **Pas vérifié sur le Pixel** (aucun téléphone branché) : la durée de l'appui long sous le doigt, et qu'un glissement lent de la pastille ne l'ouvre jamais ; TalkBack.

- **Piste :** un appui long sur la pastille musique ouvre la liste des apps média détectées (`mediaApps` existe déjà dans l'écran Écoute). Le choix lance la lecture dans l'app choisie.
- **Décisions (17 octobre, avec Dimitri) :**
  - Le geste : un appui long sur la pastille musique de l'accueil ouvre la feuille « Écouter » qui existe déjà (la liste des apps média, celle qu'ouvre la pastille quand rien n'est chargé), en Lecture comme en Pause.
  - Rien d'autre ne change : toucher une app de la feuille l'ouvre ; le bouton lecture plein d'accent de l'app principale relance ; choisir une app ne change **pas** le réglage « App média principale ». Toucher la pastille ouvre toujours Écoute, la glisser vers la droite ouvre toujours l'app qui joue, glisser vers la gauche mène toujours à Écoute.
  - Un lecteur d'écran trouve sur la pastille l'action « Apps d'écoute ».
  - Pas de transfert de la lecture d'une app à l'autre.
- **En cours de code (17 octobre) :** l'appui long n'est compté que si le doigt reste immobile ; dès qu'il glisse (vers la droite pour ouvrir l'app, vers la gauche vers Écoute), l'appui long est abandonné. La feuille se refermait à chaque nouvelle de ce qui joue ; elle ne se referme plus que quand un contenu arrive ou se met à jouer : ouverte par l'appui long, elle reste ouverte quand le morceau change ou se met en pause, et se referme quand le bouton lecture de l'app principale relance la musique.

## Lot 5 — Messages

### 5.1 Rafraîchissement des messages plus rapide — `À préciser`

- **Constat :** l'historique Beeper est lent, environ 1 s pour 30 messages et 9,5 s au premier accès (mesures du 7 octobre).
- **Pistes :**
  - Afficher tout de suite les messages tirés des notifications, puis compléter avec l'historique.
  - Précharger l'historique des 4 conversations favorites au déverrouillage (`ACTION_USER_PRESENT`) : un événement, pas du polling.
  - Mesurer le coût avant de garder.
- **À préciser :** où se voit la lenteur (liste, ouverture d'une conversation, après un envoi) et ce qui n'est « pas pratique ».

### 5.2 Différents types de messages (vocaux, transcription…) — `À préciser`

- **Piste :** l'API Beeper expose une colonne `type` et des chemins image et vidéo. Afficher vocal, image et vidéo avec une icône et un aperçu.
- **À préciser :**
  - Les types prioritaires.
  - La transcription : vérifier si Beeper l'expose ; sinon, la reconnaissance sur l'appareil, plus coûteuse.

### 5.3 Dépendre moins de Beeper (fonctions payantes) — `À préciser`

- **Constat :** interroger directement Instagram, WhatsApp ou Messenger est impossible sans API officielle et expose au bannissement du compte (écarté en phase de précisions).
- **Piste réaliste :** un mode secours qui écoute directement les notifications de chaque messagerie. On obtient la réception instantanée et la réponse, mais pas d'historique.
- **À préciser :**
  - Quelles fonctions payantes de Beeper te gênent.
  - Est-ce que le mode notifications seul suffirait pour certaines messageries ?

## Lot 6 — Direction artistique

### 6.1 Moderniser avec des apps inspirantes — `Précisé`

- **Références (15 octobre) :** quatre épingles Pinterest de thèmes KWGT/KLWP. Leur geste commun : un cordon tendu depuis le bord haut avec une poignée ronde, un accent unique sur fond nuit, une rangée d'icônes monochromes. Plus la planche « Bercail — pistes accueil » du 14 octobre (fil à plomb, météo sur le fil, réveil en bout de ligne).
- **Séance du 15 octobre :** trois planches. [Trois directions](https://claude.ai/artifact/4M2KFj6bJf7ypqCJrXgvC9) (A reskin, B relecture, C carte blanche) ; [Cadran](https://claude.ai/artifact/52Bp3K5kFqkUYEETGp4BaN) (C corrigée sur sept écrans) ; [Bercail Cadran, référence](https://claude.ai/artifact/5NHHJQSjetVk85mu8XfSjD) (onze écrans et états, le tableau des décisions, les vagues). **La référence fait foi pour le rendu** ; `docs/handoff.md` (« Décisions UX figées » et « Direction artistique », révisées le 15 octobre) fait foi pour les règles.
- **Décisions (15 octobre) :** direction **« Cadran »**. Le rendez-vous reste le héros, pas d'horloge. Le fil tendu depuis le bord, à 3 dp, sans crans, titres des rendez-vous sur le fil, anneau de maintenant, réveil en bout ; lâcher sur un rendez-vous l'ouvre dans l'agenda du téléphone. Le bas de l'accueil replié devient un bandeau plat : résumé, dock, barre « Chercher » en pilule bordée. La pastille musique reste, alignée sur le réveil. La feuille ouverte, en verre givré, ne contient que les gens et la maison, en anneaux. Les bulles restent. Vrais boutons en pilule partout. Trois accents du moment, ceux de Brume.
- **Écoute, arrêté (15 octobre, séance lecteur) :** deux épingles Pinterest de lecteurs (une arche de pochette qui descend du bord avec la progression le long de son contour ; un disque coupé par le haut). Planche [Bercail Écoute, trois pistes](https://claude.ai/artifact/Cfr4zVSNAVvVdetu3qmgJD) : A le disque, B l'arche, C pleine largeur et feuille, chacune avec sa transition depuis l'accueil. **B, l'arche, retenue** et retouchée (textes sous la pochette, arche élargie, progression réduite au demi-cercle du bas, temps écoulé et durée au centre), reportée dans la référence (version 4) et dans `docs/handoff.md` (ligne « Écran Écoute »). Décisions jointes : le héros s'efface aussi sur Écoute ; on referme en glissant vers le haut, plus de glissement latéral sur le lecteur ; plus de points de pagination, la pastille porte un chevron ; la barre native du téléphone est cachée sur Écoute seulement. Le lecteur provisoire façon Spotify est abandonné : la vague 5 code l'arche.
- **Vagues**, chacune livrable seule sur `main`, en TDD, chaque composant nouveau entrant d'abord dans le design system avec ses tests, sa capture et sa ligne de catalogue :
  1. **Le fil** : `BcColors` (accents du moment, jeton trait) ; `BcTimeline` (départ au bord, 3 dp, anneau, titres avec la règle d'écartement, réveil en bout, lâcher ouvre l'agenda). L'accueil garde son tiroir d'aujourd'hui.
  2. **Le bandeau et la feuille** : `BcBand` (plat), `BcSheet` en verre givré (6.2), feuille ouverte réduite aux gens et à la maison, `BcRing` à la place de `BcPersonTile` et `BcHomeToggle`, `BcSearchField` avec son liseré, `HomeDrawer` réécrit.
  3. **Le dock** : `BcDock`, icônes monochromes depuis `:data:apps`, réglage « Dock » (1.7).
  4. **Les feuilles** : recherche, conversation, journée, apps média sur `BcSheet` ; lignes à anneau ; `BcButton` en pilule partout ; réglages à plat.
  5. **Écoute** : l'arche. `BcCover` devient l'arche (300 × 440 dp depuis le bord haut), l'arc de progression tiré en Canvas (rayon 164 dp, angle du doigt autour du centre), temps et textes sous l'arc, lecteur seul ; la transition tenue par le doigt dans le pager de l'accueil, dans les deux sens, sortie en glissant vers le haut ; la barre native cachée sur Écoute seulement (`WindowInsetsControllerCompat`, à vérifier sur le Pixel) ; la pastille gagne son chevron et perd ses points de pagination. Règle de 2.4 appliquée à cet écran.
- **Vague 1 livrée le 16 octobre 2026**, en cinq commits poussés sur `main` : le fil part du bord dans l'accent du moment (`d58eb38`), les titres des rendez-vous (`70b591b`), lâcher ouvre le rendez-vous dans l'agenda et écarter le doigt annule (`4361cf3`), le réveil du lendemain au bout du fil (`f61db77`), le haut de l'accueil en 34 sp graisse 200 avec le glyphe du ciel (`eb86e9a`). Pas encore essayée sur le Pixel : à vérifier, le passage du trait dans la barre d'état près de la batterie, l'ouverture d'un rendez-vous et des alarmes (Agenda et Horloge de Google), le geste d'annulation sous le pouce.
- **Vague 1 : décisions (16 octobre)**, prises avec Dimitri avant le code :
  - Haut du fil : le trait part du bord haut de l'écran, et 7 h est placé juste sous la barre d'état : aucun rendez-vous, titre ni anneau de maintenant ne tombe dans la barre.
  - L'échelle reste 7 h → minuit, minuit à une place fixe ; le réveil du lendemain est un anneau juste après minuit, sans segment de nuit (la prolongation de `docs/passation-meteo-sur-le-fil.md` est remplacée).
  - Accent : celui du moment de Brume (pêche le matin, bleu-vert l'après-midi, lavande le soir et la nuit). L'anneau du réveil est dans l'accent du moment : pas de second accent.
  - Un rendez-vous est passé dès qu'il commence.
  - Titres du matin : à la hauteur du haut de l'accueil, un rendez-vous n'écrit que son heure.
  - Toucher le fil sans glisser ouvre la journée sur le rendez-vous le plus proche ; glisser puis lâcher l'ouvre dans l'agenda du téléphone.
  - Annuler : écarter le doigt vers la gauche, au-delà de la bulle, annule : la bulle s'efface et le fil se détend, et lâcher n'ouvre alors rien ; revenir sur le fil reprend le geste.
  - Réveil : celui des prochaines 24 h, lu dans Android (`AlarmManager.getNextAlarmClock`), avec la température prévue à son heure ; il apparaît même sans agenda relié. Toucher son anneau, ou lâcher dessus après un glissement (bulle « 07:15 · Réveil »), ouvre les alarmes de l'horloge du téléphone (`AlarmClock.ACTION_SHOW_ALARMS`).
  - Règle batterie : lire le prochain réveil est permis, programmer une alarme ou un travail reste interdit (`BatteryRulesTest`).
  - Haut de l'accueil : un commit de plus dans la vague 1 : titre du rendez-vous en 34 sp graisse 200, glyphe météo dans l'accent du moment.
  - Halo dessiné une fois, sans animation ; `DeviceAlarm` dans `:app`, comme `DeviceClock`.
  - En cours de code (16 octobre) : une nuit claire montre la lune au lieu du soleil dans la ligne météo ; ouvrir les alarmes demande le droit `SET_ALARM` (accordé à l'installation, sans question), que l'horloge exige pour cet écran.
- **Vague 2 livrée le 16 octobre 2026**, en cinq commits poussés sur `main` : les gens et la maison en anneaux, `BcRing` à la place de `BcPersonTile` et `BcHomeToggle` (`d1f19cb`) ; la barre Chercher en pilule bordée de 48 dp, curseur dans l'accent (`6415f21`) ; le verre des feuilles séparé de leur voile (`5f58480`, sans changement visible) ; le bandeau plat et la feuille posée dessus, `BcBand` et `BcBandSheet` à la place de `BcDrawer` (`378b2fc`) ; le verre givré (`afb8908`, voir 6.2). Pas encore essayée sur le Pixel : voir 6.2 pour la mesure, et à regarder la ligne résumé unique, la feuille qui monte et le glissement vers le bas.
- **Vague 2 : décisions (16 octobre)**, prises avec Dimitri avant le code :
  - Sur le fond d'écran de l'utilisateur, que le système dessine hors de portée de Bercail, le flou est impossible : la feuille garde un verre teinté sans flou, plus opaque (62 %).
  - Avec la musique : la feuille ouverte fige Brume, pour que le verre soit givré tout de suite et que rien ne se redessine derrière elle. La règle vaut pour toutes les feuilles (gens et maison, journée, apps média, conversation).
  - Résumé : une seule ligne, les messages puis la maison après un petit point. La glisser vers la droite ouvre Beeper ; la toucher ouvre la feuille.
  - La feuille ouverte se ferme par sa poignée, en glissant vers le bas ou par le geste retour. Toucher à côté ne la ferme pas : le fil et la pastille restent touchables.
  - Conversation, journée et apps média passent au verre givré dès la vague 2 : c'est le même `BcSheet`.
  - On suit la planche : la feuille ouverte couvre le bandeau, donc le dock et la barre Chercher. Celle-ci ne bouge jamais ; elle revient à sa place quand la feuille se ferme.
  - Choix techniques acceptés : `RenderEffect` natif d'Android, sans la bibliothèque Haze ; le commit du verre givré peut être annulé seul si la mesure Perfetto le refuse, la feuille gardant alors son verre teinté à 62 %.
  - En cours de code : le réglage « Tiroir » devient « Feuille » (« Gens et maison », état de départ « Fermée » ou « Ouverte ») ; sur Écoute, jusqu'à la vague 5, la télécommande reste dans le bandeau, à plat ; la recherche monte toujours dans son verre à 80 %, sans flou, jusqu'à la vague 4 ; le dock aura sa place dans le bandeau, entre le résumé et la barre (vague 3), sans place vide d'ici là.
- **Vague 3 livrée le 16 octobre 2026**, en dix commits sur `main` : le dessin d'une seule couleur des icônes (`4fb44f9`, la sonde qui ne faisait que journaliser retirée par `76f0397`) ; `BcDock` (`47fd707`) ; le réglage « Dock » et le dock en tête de la recherche (`9497dc6`) ; le dock dans le bandeau (`34c202d`) ; une première note de la vague (`0fcc13d`) ; la vraie icône en une seule couleur pour une app sans calque monochrome (`9283817`) ; la lecture des raccourcis d'une app (`4cfe754`) ; le menu des raccourcis dans le design system, `BcShortcutMenu` (`186b9b2`) ; le toucher long sur le dock de l'accueil (`5122176`) ; et cette note. **Pas vérifié sur le Pixel** (Dimitri en déplacement, sans téléphone branché ; il essaiera avec la version publiée) : les vraies icônes du dock (calque monochrome, ou vraie icône en une couleur), vues seulement dans les tests avec des icônes factices ; les raccourcis, qu'Android ne donne qu'à l'écran d'accueil par défaut, donc seulement avec la version publiée choisie comme écran d'accueil (la version de mise au point n'en montre aucun) ; le menu posé au-dessus de l'icône, et sa fermeture en touchant à côté.
- **Vague 3 : décisions (16 octobre)**, prises avec Dimitri :
  - L'anneau du dock : fin, 44 dp, blanc 35 %, le dessin de l'app d'environ 22 dp dedans ; `BcRing` réutilisé (il apprend le dessin d'une app et une taille), `BcDock` aligne cinq places sur la largeur, les apps depuis la gauche.
  - L'ordre : celui des touchers, la place de 1 à 5 affichée dans la liste du réglage « Dock » (les apps du dock d'abord, puis les autres) ; pour changer l'ordre, on retire puis on remet, sans écran pour réordonner. **Remplacé le 20 octobre 2026** : dans Réglages › Dock, les apps choisies (cinq au plus) viennent d'abord, chacune avec une poignée ; tenir la poignée et glisser l'app à sa place ; le nouvel ordre est enregistré et le dock de l'accueil le suit ; « Monter » et « Descendre » pour le lecteur d'écran ; les autres apps restent dessous, sans poignée ; sur l'accueil, l'appui long garde les raccourcis. Livré par `ab91007` (`BcReorderList` dans le design system) et `ffc2127` (le réglage). Décidé le même jour : les apps hors du dock gardent un espace vide de la largeur de la poignée, sans poignée dessinée, pour que tous les interrupteurs s'alignent sur une colonne ; livré par `f47471c`.
  - Cinq au plus : une fois cinq apps choisies, les interrupteurs des autres sont grisés, comme pour les personnes favorites. Les apps favorites déjà choisies passent dans le dock, les cinq premières seulement (même clé d'enregistrement, la liste enregistrée n'est pas réécrite) ; le dock et la recherche n'en prennent que cinq.
  - Dock vide (aucune app choisie) : pas de dock du tout ; le bandeau reste la ligne résumé puis la barre Chercher, sans place vide.
  - Une app sans calque monochrome : sa vraie icône en une seule couleur. D'une icône pleine (un logo sur son fond), ce qui se détache du fond, d'autant plus net que ça s'en détache, le fond effacé ; d'un dessin sur un fond transparent, sa forme. L'initiale ne reste que pour une app absente, ou une icône vide ou d'une seule couleur unie, où rien ne se lit.
  - Toucher long sur une icône du dock : les raccourcis de l'app (ceux qu'elle déclare, ceux qu'elle propose selon l'usage, ceux épinglés). Proposé par l'orchestrateur et retenu : un petit verre au-dessus de l'icône, une ligne par raccourci (son icône en une couleur dans un anneau, puis son nom) ; toucher une ligne lance le raccourci par Android (`LauncherApps.startShortcut`) ; le geste retour ou un toucher à côté referment le menu. Pas de menu quand l'app n'a pas de raccourci, ni quand Bercail n'est pas l'écran d'accueil par défaut (version de mise au point comprise) : le toucher long ne fait alors rien. Les raccourcis ne sont lus qu'au toucher long, sans rien écouter ni garder.
  - Recherche, vide : « Réglages de Bercail » en tête, puis les apps du dock (les mêmes cinq), puis A à Z.
  - Choix techniques acceptés : le dessin d'une seule couleur est fait par `:data:apps`, cadré comme l'icône sur 44 dp, gardé jusqu'à la mise à jour de l'app ; le dock lit la liste des apps installées tant que l'accueil est visible, rien d'autre ; pas de pastille de non-lus.
  - En cours de code : quatre raccourcis au plus, comme le launcher du Pixel, ceux que l'app déclare d'abord, puis ceux de l'usage, puis les épinglés, chacun dans l'ordre que l'app lui donne ; le menu est un verre teinté à 80 % comme la recherche (rien n'est givré au-dessus du bandeau), coins de 22 dp, lignes de 48 dp, anneaux de 32 dp ; le bouton Accueil et l'accueil caché referment aussi le menu ; un lecteur d'écran trouve l'action « Raccourcis » sur chaque icône du dock.
- **Vague 4 livrée le 17 octobre 2026**, en quatre livraisons poussées sur `main` après la note des décisions (`5fda12e`) :
  - Boutons : toutes les pilules bordées, avec une variante pleine d'accent (`0615c40`) ; le champ « Répondre » en pilule et l'envoi plein d'accent (`20b5e57`) ; le bouton lecture de l'app média principale plein d'accent (`7f89225`).
  - Lignes de liste : l'anneau de 36 dp et les titres de section en capitales (`3301152`) ; les apps de la recherche en une couleur dans leur anneau (`0003654`, remplacé le 17 par `43ea040`) ; les lettres trouvées dans l'accent (`b169e28`) ; la première app trouvée marquée, celle qu'ouvre Entrée (`a92e50e`) ; la feuille des apps média en lignes à anneau (`4625a87`, remplacé le 17 par `bfd87d5`) ; l'image en couleurs retirée des lignes (`8647ae7`, rendue sans anneau par `a3cc9bf`).
  - Feuilles : le verre partagé, coupé à l'écran pour la recherche (`5eb294b`) ; la recherche givrée et Brume figée pendant qu'elle est ouverte (`bb3c86c`) ; plus de voile sombre derrière les feuilles (`6273678`) ; l'en-tête de feuille, grand titre léger et bouton d'app (`6eca453`), posé sur la conversation, la journée et les apps média (`0f8dbc1`) ; l'accent sur le prochain rendez-vous et la journée entière marquée (`eac630e`) ; la liste de la journée le long d'un petit fil (`67899b4`, `b7817cd`) ; corrections : le curseur du champ « Répondre » dans l'accent (`73c9d7c`), seuls les dessins des 64 dernières apps gardés en mémoire (`723decd`) ; tests et captures (`669564c`, `96c37c9`, `37b2f9c`).
  - Recherche, icônes, réglages et fond : la meilleure app en bas du groupe Apps, contre la barre (`436e440`) ; la vraie icône d'une app en couleurs, sans anneau, dans les lignes (`a3cc9bf`), dans la recherche (`43ea040`) et dans la feuille « Écouter » (`bfd87d5`) ; le dock sans anneau, son dessin un peu plus grand (`1768e02`) ; les réglages à plat, filet blanc 10 %, commande à droite, interrupteur au trait, sélecteur bordé (`894782d`) ; « Autoriser » puis « Autorisé » sur les lignes d'accès, la ligne ouvrant la page d'Android pour le retirer (`0f7528b`, test `9ebba9f`) ; « Installer » plein d'accent, seul à installer (`3184b16`) ; le fond de base `#101119` (`0af3f9a`) ; et cette note.
  - **Pas vérifié sur le Pixel** (aucun téléphone branché) : la recherche givrée sur Brume ; les feuilles sans voile (et la fermeture en touchant au-dessus) ; l'anneau creux de la journée entière dans la liste de la journée ; les vraies icônes en couleurs dans la liste A à Z complète (fluidité du défilement) et dans la feuille « Écouter » ; le dock sans anneau et la taille de ses dessins (environ 28 dp) ; les réglages à plat (« Autoriser », « Autorisé », « Installer », pages d'Android pour retirer un accès).
- **Sans réveil (17 octobre)**, décidé avec Dimitri : quand aucun réveil ne sonne dans les 24 h, le bout du fil garde le même anneau creux, à la même place, mais en gris (ni accent ni halo), avec « Aucun réveil » à côté. Le toucher, ou lâcher dessus après un glissement (bulle « Aucun réveil »), ouvre les alarmes de l'horloge, comme avec un réveil, même sans agenda relié ; un lecteur d'écran entend le bouton « Aucun réveil », qui ouvre les alarmes. Rien de plus n'est lu ni programmé : c'est la même lecture du prochain réveil. Le dessin (`a33bb48`), puis l'accueil. **Pas vérifié sur le Pixel.**
- **Vague 4 : décisions (16 et 17 octobre)**, prises avec Dimitri avant le code :
  - Recherche : plein écran, sans coins ni poignée, dans le verre de la feuille à 80 % (noir bleuté proche de `#101119`), givré ; Brume se fige pendant la recherche, comme sous les autres feuilles.
  - Conversation, journée et apps média : plus de voile sombre derrière la feuille, mais toucher au-dessus la ferme toujours.
  - Feuille des apps média : l'app principale est une ligne à anneau comme les autres, mais son anneau est dans l'accent du moment, « Reprendre ‹ dernier contenu › » dessous, et à droite un bouton lecture rond, plein d'accent.
  - Journée : l'anneau d'accent marque toujours le prochain rendez-vous, comme sur le fil ; ouvrir la journée sur un rendez-vous fait seulement défiler jusqu'à lui.
  - Bouton plein d'accent : seulement Envoyer (message), Installer (mise à jour) et le bouton lecture de l'app média principale. Tout le reste (« Autoriser … », « Relier l'agenda », « Beeper », « Agenda »…) est une pilule bordée. Sur Écoute, lecture-pause reste plein clair (la vague 5 refera Écoute, sa mise en page ne bouge pas).
  - Réglages, lignes d'accès (« Musique en cours », « Messages de Beeper », agenda, position…) : plus d'interrupteur. Tant que l'accès manque, une pilule « Autoriser » à droite ; une fois accordé, « Autorisé » en gris, et toucher la ligne ouvre la page d'Android pour le retirer.
  - Réglages, ligne de mise à jour : quand une version attend, une pilule d'accent « Installer » à droite ; seul le bouton installe (toucher la ligne n'installe plus).
  - Réglages, filets entre les lignes : blanc 10 % (les 22 % restent pour le fil futur) ; la direction artistique est corrigée.
  - Choix techniques acceptés : les boutons sont dessinés à 36 dp dans une cible de 40 dp au moins (les tests des 40 dp portent sur la cible) ; les commandes faites d'une seule icône (flèche retour, × qui vide la recherche) restent des icônes nues ; le champ « Répondre » reste une pilule entière même quand il grandit jusqu'à 4 lignes ; le fond de base `BcColors.base` devient `#101119`, comme dans la direction artistique.
  - Recherche, ordre des résultats (17 octobre) : en tapant, les résultats montent depuis la barre (Apps tout en bas, juste au-dessus de « Chercher », puis Gens, Messages, Agenda, Maison, Réglages) ; dans le groupe Apps, la meilleure app, celle qu'ouvre Entrée (« Entrée » en gris à droite), est en bas contre la barre, les suivantes au-dessus d'elle. Les autres familles gardent leur ordre, de haut en bas.
  - Les icônes des apps (17 octobre ; Dimitri s'était mal exprimé, ces trois points remplacent ce qui disait « anneau » pour les apps plus haut) :
    - Recherche : chaque app avec sa vraie icône en couleurs, 36 dp, sans anneau autour ; les gens, les rendez-vous, la maison et le reste gardent leur anneau. La meilleure app garde « Entrée » en gris, sans anneau d'accent puisqu'il n'y a plus d'anneau. La recherche ne demande plus les dessins d'une seule couleur (le dock les garde).
    - Feuille « Écouter » : de même, les vraies icônes en couleurs, sans anneau ; l'app principale reste mise en avant par « Reprendre ‹ … › » et son bouton lecture plein d'accent, plus par un anneau d'accent.
    - Dock : plus d'anneau autour des icônes ; le dessin blanc d'une seule couleur seul, un peu plus grand faute d'anneau : cadré sur 56 dp au lieu de 44, le dessin fait environ 28 dp au lieu de 22, dans les mêmes cinq places de 44 dp (cible du toucher inchangée) ; même menu des raccourcis au toucher long, dont les lignes gardent leurs petits anneaux.
  - En cours de code (17 octobre) : une version prête dit seulement « Version 0.9.0 prête » (« · toucher pour l'installer » n'a plus de sens, c'est le bouton qui installe) ; prête sans accord à demander, elle garde « · elle s'installera quand tu quitteras l'accueil » et offre aussi « Installer ». Une version disponible mais pas encore téléchargée garde sa ligne qui se touche pour la télécharger, sans « Installer ». Sur « Ma position approximative », « Autorisé » s'affiche aussi quand une ville est choisie, et toucher la ligne reprend alors la position à la place de la ville ; la page d'Android pour la retirer s'ouvre quand la position est utilisée. Les textes « Toucher pour autoriser… » des lignes d'accès sont gardés tels quels, à côté du bouton. Un court choix (« Gens et maison », Fermée · Ouverte) se pose à droite de sa ligne, un long (« Sans musique ») dessous.
- **Vague 5 : décisions (17 octobre)**, prises avec Dimitri avant le code de l'arche :
  - Titre d'Écoute : 28 sp, graisse 300 (la direction artistique disait 24 sp par erreur ; corrigée).
  - Un direct (durée nulle : Twitch, une radio) : le demi-cercle reste dessiné, en gris, sans anneau à tirer (on ne peut pas se déplacer dans un direct), et « En direct » dans l'accent du moment remplace les deux temps au centre. La mise en page ne bouge pas.
  - Écoute automatique quand un casque se connecte : **plus tard**, pas dans cette vague ; elle sera décidée à part. La ligne du tableau des réglages reste telle quelle.
  - Choix techniques acceptés : se déplacer dans le morceau se fait comme sur la barre d'aujourd'hui, en touchant ou en glissant dans une bande d'environ 40 dp le long de l'arc, et seulement quand l'app l'accepte ; sans cela l'arc garde son anneau mais ne réagit pas. Un glissement vers le haut parti d'ailleurs referme Écoute. Le geste retour d'Android sur Écoute ramène à l'accueil, comme glisser vers le haut. L'album, que Bercail ne lisait pas encore, est lu avec le reste de ce qui joue ; sans album, la ligne montre l'artiste seul. Quand plus rien n'est chargé pendant qu'Écoute est affiché, l'accueil revient, par la transition jouée à l'envers.
- **Vague 5 livrée le 17 octobre 2026**, en trois livraisons poussées sur `main` après la note des décisions (`00a3210`) :
  - L'arche et l'arc, dans le design system : la pochette en arche pendue au bord haut, `BcArchCover` (`75415b7`) ; le demi-cercle de la progression sous l'arche, tiré en suivant l'angle du doigt, `BcArcProgress` (`5791d28`).
  - L'écran Écoute lui-même : l'album lu avec le reste de ce qui joue, « Artiste · Album » sous le titre, l'un des deux seul quand l'autre manque (`f1efa2f`) ; le titre d'Écoute en 28 sp graisse 300 (`5a08540`) ; le lecteur seul, placé depuis le bord haut comme sur la référence : l'arche pendue au bord, l'arc et ses temps, le titre sur deux lignes au plus, Précédent / lecture-pause (76 dp, plein clair) / Suivant (ronds bordés de 48 dp, seulement ceux que l'app accepte, lecture-pause toujours au milieu), « Ouvrir dans … », le chevron gris 45 % ; ni en-tête, ni fil, ni pastille, ni bandeau ; `BcCover` supprimée (`60273e3`) ; sortir en glissant vers le haut (parti hors de l'arc, qui garde le doigt pour se déplacer dans le morceau), aucun glissement latéral sur le lecteur, et pour un lecteur d'écran l'action « Revenir à l'accueil » sur le chevron (`3d7bbd3`) ; la pastille musique finit par un chevron, les points de pagination disparaissent, `BcPageDots` supprimé (`873c575`) ; la note de cette livraison (`7094572`) ; le fondu des feuilles passé à l'Accueil comme une valeur lue à chaque image, sans changement visible (`367007c`).
  - La transition tenue par le doigt et la barre d'état : `BcHeldTransition`, un passage d'un écran à l'autre que le doigt tient, qui se pose seul et ne change que 20 fois par seconde au plus (`978bf97`) ; Accueil ⇄ Écoute tenu par le doigt (`f2fc843`) ; la barre d'état du téléphone cachée sur Écoute seulement (`be6f500`) ; et cette note.
  - Ce que fait la transition : en glissant vers la gauche au milieu de l'Accueil (seulement en Pause ou en Lecture), l'arche descend du bord haut à mesure du doigt, comme une toile qu'on déroule (traverser toute la largeur de l'écran la pose entière), avec son arc ; le héros et la pastille partent vers la gauche, du même pas que le doigt ; le fil se rétracte dans le bord haut, coupé par le bas ; le bandeau descend sous l'écran ; les textes et les boutons du lecteur apparaissent dans la seconde moitié du chemin. À mi-chemin : la moitié basse de l'arche est descendue, le héros et la pastille sont à moitié sortis par la gauche, le fil n'a plus que sa moitié haute, le bandeau est à moitié descendu, et le lecteur n'a encore ni texte ni bouton. Sur Écoute, glisser vers le haut remonte l'arche sous le doigt (440 dp, la hauteur de l'arche, pour tout le chemin). Lâcher pose la transition du côté le plus proche, ou de celui où on l'a lancée d'un coup sec (plus d'une largeur d'écran par seconde), sur la courbe du prototype, sans rebond. La pastille, le geste retour, l'action du lecteur d'écran et la musique qui s'arrête la jouent seule, en 400 ms (`BcMotion.screenMillis`). Brume ne change pas pendant la transition : elle est déjà aux couleurs de la pochette sur l'Accueil dès qu'un contenu est chargé.
  - 20 images par seconde au plus, sous le doigt comme en se posant : la transition ne change d'état qu'au rythme du limiteur des animations continues (`FrameThrottle`, celui de Brume et de l'égaliseur), une fois toutes les 50 ms au plus, et ne demande plus aucune image une fois posée, ni quand le doigt est immobile. Les feuilles et la recherche gardent leurs animations courtes d'avant, non limitées.
  - La barre d'état du téléphone (heure, batterie, réseau) se cache quand Écoute est posé, se montre un instant par un glissement depuis le bord haut, et revient dès qu'on commence à repartir (le doigt qui remonte l'arche, le geste retour, la musique qui s'arrête), ou que l'accueil n'est plus à l'écran ; elle se recache en revenant sur Écoute. **À juger sur le Pixel** : si cet aller-retour agace, la cacher partout ou nulle part sera une décision à prendre plus tard (passation de la direction « Cadran », point 4).
  - Choix faits au code, à revoir si Dimitri le souhaite : la pastille part à gauche avec le héros ; le fil se rétracte coupé par le bas, sans fondu ; l'arche descend d'un bloc (elle n'est pas coupée) ; les textes et boutons apparaissent à partir de la moitié du chemin. La planche image par image ne se lisait plus : la transition suit la description écrite (2.4).
  - Sur le Pixel 9 (411 × 923 dp), le chevron finit à 804 dp du bord haut : il reste 119 dp dessous (barre de navigation comprise). Écart à la référence : les icônes de Précédent et Suivant font 22 dp au lieu de 20.
  - Plus tard, comme décidé : l'Écoute automatique quand un casque se connecte.
  - **Pas vérifié sur le Pixel** (aucun téléphone branché) : l'arche sous une vraie pochette, et sous une vignette de vidéo ; tirer l'arc au doigt, et glisser vers le haut sans le toucher ; la transition tenue par le doigt, dans les deux sens, et sa fluidité à 20 images par seconde ; l'aller-retour de la barre d'état (et le haut de l'Accueil, qui pourrait descendre de la hauteur de la barre au moment où elle revient) ; le geste retour sur Écoute ; l'action « Revenir à l'accueil » avec TalkBack ; un vrai album lu dans Spotify et YouTube Music ; un titre de deux lignes et un direct (Twitch, radio).
- **17 octobre, après la vague 5** (`3171953`), décidé avec Dimitri : Écoute, tout un peu plus bas. L'arche grandit de 10 %, 440 → 484 dp, même largeur de 300 dp et même arrondi de 150 dp en bas (seule la partie droite s'allonge) ; tout ce qui est dessous (l'arc, toujours concentrique à la courbe de l'arche, les temps, le titre, « Artiste · Album », la télécommande, « Ouvrir dans ‹app› », le chevron) descend des mêmes 44 dp, et la transition tenue par le doigt suit la nouvelle hauteur (484 dp pour tout le chemin). Sur le Pixel 9 (411 × 923 dp), le chevron finit à 848 dp du bord haut : il reste 75 dp dessous (barre de navigation comprise, au lieu de 119) ; un titre de deux lignes tient sans rien chevaucher.
- **17 octobre, des pochettes plus nettes** (`94f54a3`) : Bercail réduisait chaque pochette à 512 px, alors que l'arche la montre sur environ 1 270 px de haut sur le Pixel 9 (484 dp à 2,625 px par dp) : l'image était agrandie plus de deux fois et paraissait pixelisée. Une pochette est maintenant gardée jusqu'à 1 280 px de long (jamais agrandie par Bercail), l'arche agrandit en douceur ce qui reste plus petit, et l'image garde aussi des versions réduites pour rester nette dans la petite pastille. Les deux couleurs de Brume sont toujours lues sur une copie d'environ 112 × 112 px : pas plus lent. Mémoire : une pochette carrée de 1 280 px pèse 6,5 Mo (1 Mo avant) ; Bercail en garde une par app média dont la session est ouverte (souvent une, deux ou trois avec Spotify, YouTube Music et une app de podcasts : environ 20 Mo), plus l'image dessinée à l'écran (une par pochette pour la pastille, une de plus pour l'arche pendant Écoute). Une vignette vidéo 16:9 est gardée à 1 280 × 720 px : recadrée au centre de l'arche, elle reste agrandie environ 1,8 fois.
  - **Pas vérifié sur le Pixel** : la netteté de la pochette dans l'arche et dans la pastille ; la vraie taille de l'image que Spotify (et YouTube Music) mettent dans leur session. Si elle est déjà petite (sous 600 px environ), une étape suivante pourrait lire une fois par morceau la grande image dont l'app donne l'adresse (`METADATA_KEY_ART_URI` / `METADATA_KEY_ALBUM_ART_URI`) ; rien n'est construit pour l'instant.

### 6.2 Effet « frozen glass » sur les écrans transparents — `Précisé`

- **Piste :** flou d'arrière-plan avec la bibliothèque Haze (RenderEffect), seulement sur les panneaux et seulement quand rien ne bouge derrière. Coût GPU à mesurer avec Perfetto.
- **Décision (15 octobre, séance 6.1) :** le verre givré est la feuille, et seulement elle : tiroir ouvert, recherche, conversation, journée, apps média. Le bandeau replié de l'accueil n'a plus de verre du tout. Valeurs : noir bleuté à 46 % (80 % pour la recherche), flou 36 dp, saturation 1,7, reflet blanc 11 % → 0 sur le haut, liseré blanc 16 %, coins 32 dp, poignée. Rendu seulement feuille ouverte et Brume figée ; mesure Perfetto avant d'être gardé. Livré par la vague 2 de 6.1.
- **Décisions (16 octobre, vague 2)** : le flou ne touche que Brume, que Bercail dessine ; sur le fond d'écran de l'utilisateur, dessiné par le système, la feuille reste teintée à 62 %. Une feuille ouverte fige Brume. Flou par `RenderEffect` natif (minimum Android 16, rien à prévoir pour les anciennes versions), sans dépendance. Pas de flou en économie d'énergie ni quand Android supprime les animations.
- **Développé le 16 octobre (`afb8908`), pas encore gardé** : Brume n'est enregistrée pour le verre que tant qu'une feuille est ouverte ; figée, elle l'est une fois. Vérifié par les tests (`BcFrostUiTest`, `HomeLooksTest`) : givré seulement feuille ouverte sur Brume, aucun enregistrement feuille fermée, teinté en économie d'énergie, sans animations ou sans Brume, Brume figée sous toute feuille. Les captures Robolectric ne montrent sans doute pas le flou : il se juge sur le Pixel.
- **Reste à faire, sur le Pixel** (avant de passer à `Livré`) : `BRUME_MODES="still still+tinted still+frosted still+blinking still+toggling" bash scripts/measure-brume.sh` ; à garder si la feuille givrée immobile coûte autant que la feuille teintée (à l'écart de mesure près) et si les images ratées restent sous 1 %, sinon annuler `afb8908` seul.

### 6.3 Revoir les effets météo — `À préciser`

- **À préciser :** ce qui ne va pas : trop discret, trop chargé, pas assez lisible, pas assez différent selon le temps.

### 6.4 La brume ne bouge pas — `À préciser`

- **Constat :** voulu par conception. La brume n'avance qu'à 20 fps et seulement pendant la lecture musicale, puis s'arrête après 10 s sans toucher l'écran (`BcBrume.kt`).
- **Pistes :**
  - Une courte dérive à l'arrivée sur l'accueil, par exemple 10 s à 10 fps, puis l'arrêt.
  - Ou laisser tel quel.
  - À mesurer.
- **À préciser :** l'effet souhaité et le budget acceptable.

### 6.5 Tuiles Google Home dans Bercail — `À préciser`

- **Constat :** un launcher ne peut pas afficher les tuiles « Contrôle des appareils » du système, car la permission est réservée aux apps système.
- **Piste :** l'API Google Home pour Android (OAuth, appareils reliés à Google Home). C'est faisable mais lourd. Commencer par une vérification de faisabilité.
- **À préciser :**
  - Les appareils visés.
  - Remplacent-ils ou complètent-ils les commandes Hue actuelles ?

### 6.6 Des fonds d'écran façon Brume — `À préciser`

- **Retour (14 octobre, venu du point 1.6) :** « J'aimerais revoir les fonds d'écran de type Brume ».
- **Piste :** des fonds propres à Bercail (dégradés pastel, Brume immobile), à travailler avec 6.3 et 6.4. Un fond fixe ne coûte rien ; tout mouvement passe par une mesure.
- **À préciser :** ce qui ne te plaît pas dans les fonds actuels, et si un fond Bercail remplace le fond météo de Google ou s'y ajoute.

### 6.7 Tous les écrans tiennent sur un écran plus petit — `Livré`

- **Constat (20 octobre) :** la CI a montré, sur un émulateur de 320 × 640 dp, qu'Écoute ne tient pas sous environ 850 dp de haut : l'arche de 300 × 484 dp et tout ce qui est dessous (temps, titre, télécommande à 648 dp, « Ouvrir dans ‹app› », chevron) sont placés pour les 923 dp du Pixel 9. Une « Taille d'affichage » agrandie dans Android donne aussi moins de dp.
- **Décidé (20 octobre) :** tous les écrans se resserrent pour tenir, en hauteur comme en largeur. Sur le Pixel 9 à sa taille d'affichage normale (411 × 923 dp), rien ne change. Sur Écoute, un écran moins haut raccourcit l'arche en lui faisant perdre d'abord sa partie droite, sa courbe de 150 dp gardée aussi longtemps que possible, et resserre les écarts entre les textes, la télécommande, le bouton et le chevron ; le chevron et chaque commande restent entiers à l'écran, avec des cibles d'au moins 40 dp. Un écran plus étroit réduit l'arche en gardant ses proportions, l'arc restant concentrique. La transition tenue par le doigt suit la hauteur réelle de l'arche. Tout se déduit de la taille disponible, la mise en page de 923 dp pour référence, sans palier réglé à la main par appareil. Les autres écrans (accueil, feuilles, recherche, réglages) sont vérifiés et corrigés : rien de coupé, rien qui se chevauche, rien hors de l'écran ou sous les barres du système ; un texte peut passer à la ligne ou s'abréger, jamais en heurter un autre. Tailles vérifiées : 411 × 923, 393 × 780, 360 × 640, 320 × 640, et le Pixel 9 en « Taille d'affichage » maximale (360 × 808).
- **Révisé (20 octobre, décidé avec Dimitri) :** sur un écran moins haut, l'arche reste haute aussi longtemps que possible. Dans l'ordre : 1. les écarts entre les temps, le titre, la télécommande, « Ouvrir dans ‹app› » et le chevron se resserrent jusqu'à leur minimum ; 2. seulement ensuite l'arche perd sa partie droite, sa courbe de 150 dp gardée ; 3. seulement ensuite l'arche entière rétrécit. Avant, les écarts et la partie droite se resserraient dans la même proportion. Les règles en largeur ne changent pas, et rien ne change à 923 dp. Livré dans `1cd4b7d` : sur 360 × 640, l'arche fait environ 263 × 311 dp (au lieu de 282 de haut) ; sur 411 × 850, elle garde ses 300 × 484 dp.
- **Livré le 20 octobre 2026** : `4ee10d9` (design system : l'arche et son arc prennent une taille, `BcRingRow`), `a947fbf` (accueil et Écoute), `621a72d` et `1d7bd12` (tests de la recherche et des réglages, rien à y corriger).
  - Écoute : l'arche et son arc rétrécissent ensemble pour que l'arc tienne avec 16 dp de chaque côté ; en hauteur, voir l'ordre révisé ci-dessous. Les boutons gardent leur taille (76 et 48 dp), et 24 dp au moins restent sous le chevron, au-dessus de la barre de navigation.
  - Accueil : les cinq anneaux de la feuille (gens, maison) se partagent la largeur au lieu d'écraser le dernier (« Tous » et « Tout » n'avaient plus que 32 dp sur 320 dp) ; la pastille musique s'arrête avant la ligne du temps, au lieu de passer sous le réveil avec un titre long.
  - Recherche et réglages : rien à corriger, désormais vérifié à chaque taille.
  - Tests : chaque écran, chaque feuille, la recherche et chaque page des réglages aux cinq tailles (éléments importants entiers et sans chevauchement), la mise en page d'Écoute calculée, et trois captures à 360 × 640 (accueil, feuille ouverte, Écoute).
- **Pas vérifié sur le Pixel** (le téléphone était verrouillé : la version de mise au point y est installée, mais rien n'y a été regardé ; sa taille d'affichage n'a pas été changée) :
  - Écoute et l'accueil à la taille normale, identiques à avant.
  - Les interrupteurs alignés du réglage « Dock ».
  - Écoute et l'accueil avec la « Taille d'affichage » au maximum, puis avec la police agrandie.
  - Écoute avec la navigation à trois boutons (barre plus haute que celle des gestes).
  - Le geste qui ramène l'arche vers le haut sur un écran plus petit.
