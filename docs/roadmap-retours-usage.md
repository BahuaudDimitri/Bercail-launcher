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
| 1 | Ménage rapide | 1.1 à 1.7 | Mécanique |
| 2 | Gestes et transitions | 2.1 à 2.4 | Mécanique + réglage fin |
| 3 | Accueil : infos | 3.1 à 3.3 | Maquette avant code |
| 4 | Musique | 4.1 à 4.2 | Investigation sur téléphone |
| 5 | Messages | 5.1 à 5.3 | Investigation + mesure |
| 6 | Direction artistique | 6.1 à 6.6 | Exploration, maquettes, mesures |

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

### 1.7 Chrome facilement accessible — `Précisé`

- **Constat :** « Chercher sur le web » n'apparaît que quand la recherche ne trouve rien, et ouvre l'app Google (`ACTION_WEB_SEARCH`), pas Chrome. Chrome peut déjà être mis en app favorite dans les Réglages.
- **Décidé (14 octobre) :** Chrome s'ouvre tel qu'il a été laissé, comme en touchant son icône.
- **Décision (15 octobre, séance 6.1) :** un **dock** de cinq icônes au trait, sans libellé, dans le bandeau replié de l'accueil, juste au-dessus de la barre « Chercher » : les apps favorites des réglages, cinq au plus, dans l'ordre choisi, Chrome en tête. Icône réelle de l'app rendue en monochrome dans un anneau (pas de glyphes dessinés à la main, pour tenir toutes les apps). Présent sur l'accueil seulement, pas dans la feuille ouverte ni sur Écoute. Le réglage « Apps favorites » devient « Dock ». Livré par la vague 3 de 6.1.

## Lot 2 — Gestes et transitions

### 2.1 Glisser vers le bas pour fermer les « modales » — `À préciser`

- **Piste :** ajouter le geste une fois dans `BcDrawer` et `BcPanel`, pour que toutes les feuilles en profitent. Seuil et vitesse à régler, avec un test du seuil comme pour le tiroir (24 dp).
- **À préciser :** la liste des écrans concernés. Et les écrans avec une liste qui défile (conversation, recherche) : le geste ne doit fermer que quand la liste est en haut.

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

### 4.2 Changer d'app musicale facilement — `À préciser`

- **Piste :** un appui long sur la pastille musique ouvre la liste des apps média détectées (`mediaApps` existe déjà dans l'écran Écoute). Le choix lance la lecture dans l'app choisie.
- **À préciser :**
  - Le geste.
  - Le choix change-t-il l'app principale des réglages, ou seulement la lecture en cours ?
  - Faut-il pouvoir transférer la lecture en cours (sans doute impossible entre apps) ?

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

### 6.2 Effet « frozen glass » sur les écrans transparents — `Précisé`

- **Piste :** flou d'arrière-plan avec la bibliothèque Haze (RenderEffect), seulement sur les panneaux et seulement quand rien ne bouge derrière. Coût GPU à mesurer avec Perfetto.
- **Décision (15 octobre, séance 6.1) :** le verre givré est la feuille, et seulement elle : tiroir ouvert, recherche, conversation, journée, apps média. Le bandeau replié de l'accueil n'a plus de verre du tout. Valeurs : noir bleuté à 46 % (80 % pour la recherche), flou 36 dp, saturation 1,7, reflet blanc 11 % → 0 sur le haut, liseré blanc 16 %, coins 32 dp, poignée. Rendu seulement feuille ouverte et Brume figée ; mesure Perfetto avant d'être gardé. Livré par la vague 2 de 6.1.

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
