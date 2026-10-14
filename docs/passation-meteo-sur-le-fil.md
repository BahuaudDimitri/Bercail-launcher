# Passation — la météo sur le fil

14 octobre 2026 · Dimitri. Décidé sur la planche de design ; maquette de référence : `design/prototype/meteo-sur-le-fil.html` (390 × 844 px = le Pixel 9 en dp).

Cette passation sert de point de départ à la session de développement. Le développeur repart de zéro, sans l'historique de la conversation de design : tout ce qu'il faut est ici, dans `CLAUDE.md` et dans `docs/handoff.md`.

## Résumé

La météo quitte l'en-tête de l'accueil et passe sur la ligne du temps verticale. La ligne colore ses heures selon le ciel prévu (pluie, éclaircies, nuit), porte les températures, puis se prolonge après minuit jusqu'au réveil du lendemain, marqué par un anneau creux.

Pourquoi : l'en-tête respire (il ne garde que l'agenda), la météo se lit là où l'on lit déjà le temps qui passe, et la ligne gagne une fin utile, au pouce.

## Décisions figées

Tranchées le 14 octobre 2026. Elles s'ajoutent à `docs/handoff.md` et y priment pour la zone haute de l'accueil.

| Sujet | Avant | Maintenant |
|---|---|---|
| Ligne météo de l'en-tête | « 14° · Pluie fine jusqu'à 16 h » au-dessus de l'agenda | Retirée. L'en-tête ne garde que « Dans 1 h 10 · 15:00 », le titre, « Ensuite … » |
| Ligne du temps | Passé plein, futur pointillé blanc | Le futur prend la couleur du ciel prévu, heure par heure ; le passé reste blanc plein |
| Températures | Absentes de la ligne | Écrites le long du trait, une par plage de ciel |
| Fin de la ligne | Minuit, au-dessus du tiroir | Se prolonge après minuit jusqu'au réveil du lendemain |
| Réveil | Absent | Anneau creux lavande au bout du fil, heure + « réveil · 9° » à côté. Variante retenue : l'anneau (la pilule et le demi-soleil ont été écartés) |
| Alignement | — | Le centre de l'anneau est à la hauteur du centre de la pastille musique |
| Vent | — | Rangé dans « Sec » : pas de couleur propre (Brume dessine déjà les rafales) |
| Neige | — | Blanc (`BcColors.text`) à 55 % |
| Variante horizontale | Existe dans les réglages | Supprimée avant ce chantier (feuille de route, point 1.1, décidé le 14 octobre) : la météo sur le fil ne concerne que la ligne verticale |

Rien d'autre ne bouge : tiroir, pastille musique, Brume, écran Écoute restent tels quels.

## Spécification visuelle

Valeurs tirées de la maquette. Toutes les couleurs existent déjà dans `BcColors` : aucun nouveau token de couleur.

| Élément | Rendu | Token / valeur |
|---|---|---|
| Passé (7 h → maintenant) | Trait plein blanc, 1,5 dp | `BcColors.text` à 55 %, comme aujourd'hui |
| Futur, heures de pluie | Tirets plus épais et serrés, plus petites hachures en biais à droite du trait (gouttes) | `pastelBlue` à 95 % ; trait 2,5 dp, tiret 5 / espace 4 ; hachures 6 dp, 18°, 50 à 70 % |
| Futur, ciel sec (soleil, nuages, vent) | Pointillé habituel teinté | `pastelApricot` à 55 % ; 1,5 dp, tiret 3 / espace 5 |
| Futur, neige | Pointillé habituel | `BcColors.text` à 55 % ; 1,5 dp, tiret 3 / espace 5 |
| Après minuit → réveil | Pointillé très fin | `pastelLavender` à 40 % ; 1 dp, tiret 1 / espace 5 |
| Maintenant | Point plein + halo, halo teinté du ciel actuel | Halo `pastelBlue` à 70 % sous la pluie, blanc sinon |
| Températures | Texte vertical à droite du trait, une par plage de ciel, dans la couleur de la plage | `BcTextStyle.Micro` (10,5 sp), Medium |
| Nom du ciel | À gauche du trait, au début de chaque plage, comme une heure de rendez-vous : « Pluie fine », « Éclaircies », « Nuit claire » | `Micro`, couleur de la plage |
| Minuit | Petit trait horizontal de 18 dp en travers de la ligne | `BcColors.text` à 30 % |
| Réveil | Anneau creux 12 dp, bord 1,5 dp, léger halo ; cible tactile 48 dp invisible | `pastelLavender` ; halo lavande à 50 %, flou 10 dp |
| Texte du réveil | « 07:15 » au-dessus de « réveil · 9° », aligné à droite, à gauche de l'anneau | 15 sp Medium (`BcTextStyle.Body` ou nouveau style) + `Micro` muted |

**Alignement.** Le centre de l'anneau tombe à la hauteur du centre de la pastille musique (`MusicPill`, `PILL_BOTTOM` = 40 dp au-dessus du tiroir, dans `HomeMiddle.kt`). Sur la maquette : pastille de y 556 à 600, centre 578 ; anneau centré à 578. Aujourd'hui `VERTICAL_BOTTOM` = 24 dp (dans `HomeHeader.kt`) : la ligne doit maintenant finir à hauteur de la pastille, et ce calcul doit suivre la hauteur réelle du tiroir.

**Échelle de la ligne.** La ligne couvre désormais de 7 h au réveil du lendemain, pas jusqu'à minuit. Minuit devient un repère sur la ligne, plus son extrémité (voir questions ouvertes pour l'échelle exacte).

## Règles de comportement

Chaque règle devient au moins un test, nommé en phrase française.

**Ciel sur la ligne**

- Chaque heure à venir prend la couleur de son ciel prévu ; les heures passées restent blanches.
- Deux heures voisines au même ciel forment une seule plage : un seul nom, une seule température.
- Une heure venteuse sans pluie ni neige est une heure sèche.
- Sans prévision (pas d'accès à la position, réseau absent, prévision trop ancienne), la ligne garde son rendu actuel, blanc pointillé, sans nom ni température.
- Le nom de plage suit la règle existante de `Weather.kt` : pas de « nuit claire » le matin.
- Un nom de plage qui chevauche l'heure d'un rendez-vous s'efface au profit du rendez-vous.

**Réveil**

- Avec un réveil prévu dans les prochaines 24 h, la ligne finit sur l'anneau, avec l'heure et la température prévue à cette heure.
- Sans réveil, la ligne finit à minuit comme aujourd'hui, sans anneau.
- Toucher l'anneau ouvre l'app d'horloge, sur l'alarme si Android le permet.
- Glisser le doigt le long de la ligne jusqu'à l'anneau : la bulle dit « Réveil · 07:15 », et lever le doigt ouvre l'horloge.
- Le lecteur d'écran annonce « Réveil demain à 07:15 ».

**Mise en page**

- Le centre de l'anneau est aligné sur le centre de la pastille musique, quelle que soit la hauteur du tiroir.
- Sur l'écran Écoute, la ligne s'efface toujours, anneau compris.
- L'en-tête n'affiche plus la ligne météo ; `BcWeatherLine` n'est plus utilisé sur l'accueil vertical.

## Plan d'implémentation

Quatre étapes, chacune livrable seule et verte en CI, en TDD et sur `main` comme le veut `CLAUDE.md`. Commencer par lire `docs/CATALOG.md`.

**Déjà là, à réutiliser**

- `Forecast` / `HourForecast` (`:core:domain`, `weather/Weather.kt`) : ciel `Sky` et température par heure. Aucune nouvelle requête Open-Meteo n'est nécessaire.
- `dayLineAt` / `DayLine` (`:core:domain`, `agenda/Agenda.kt`) : position 0–1 des rendez-vous et de maintenant sur la journée (0 = 7 h, 1 = minuit).
- `BcTimeline` (`:core:designsystem`, `component/BcTimeline.kt`) : tracé, marques, bulle sous le doigt, courbe, haptique.
- `MusicPill` + `PILL_BOTTOM` (`HomeMiddle.kt`) et `VerticalTimeline` (`HomeHeader.kt`).

**Étapes**

1. **Domaine — plages de ciel.** Dans `:core:domain`, une fonction pure qui découpe les heures à venir de la ligne en plages `(début, fin, genre de ciel, température, nom)`. Genres : Pluie (tout `Sky.falls` sans neige), Neige, Sec (y compris le vent), Nuit (sec après le coucher, réutiliser la logique `DayPart` existante). Tests unitaires : fusion des heures voisines, pas de plage dans le passé, aucune prévision → aucune plage, vent → sec. Couverture 90 % exigée sur ce module.
2. **Design system — `BcTimeline` en couleurs.** Ajouter un paramètre optionnel `skies: List<BcTimelineSky>` (début, fin, genre parmi un petit enum `Rain | Dry | Night | Snow`, texte de température, nom). Sans `skies`, rendu strictement identique à aujourd'hui (captures existantes inchangées). Nouvelles captures Roborazzi : pluie puis éclaircies, journée sèche, nuit, neige. Mettre `BcGallery` à jour.
3. **Domaine + données — le réveil.** Interface `AlarmSource` dans `:core:domain` (prochaine alarme : instant + intention d'ouverture), faux dans `:core:testing`, implémentation dans `:app` ou un nouveau `:data:alarm` via `AlarmManager.getNextAlarmClock()`, lue à l'affichage de l'accueil et sur la diffusion `ACTION_NEXT_ALARM_CLOCK_CHANGED`, jamais en boucle. Étendre l'échelle de la ligne de 7 h au réveil. `BcTimeline` gagne une marque de fin `end: BcTimelineEnd?` (anneau, libellé, ouverture).
4. **Accueil — assemblage.** `HomeViewModel` calcule plages et réveil ; `HomeHeader` retire `BcWeatherLine` en mode vertical ; `VerticalTimeline` reçoit `skies` et `end`, et sa marge basse s'aligne sur le centre de la pastille musique au lieu de `VERTICAL_BOTTOM`. Tests d'interface (`*UiTest`) pour chaque règle de la section précédente, puis capture de l'accueil complet comparée à la maquette.

Mettre `docs/CATALOG.md` (et un nouveau module dans `settings.gradle.kts` + son `README.md` si `:data:alarm` est créé) à jour dans les mêmes commits (`CatalogTest` le vérifie), et consigner l'avancement dans `docs/handoff.md`, vague 10. Les étapes 1, 2 et 3 sont indépendantes ; la 4 les assemble. Commits : `feat:` dès que l'app visible change (étape 4 au moins, qui publie une version mineure), sinon `refactor:` ou `test:`.

## Batterie, risques et questions ouvertes

**Batterie.** Aucun coût nouveau attendu : les couleurs et tirets sont statiques, dessinés par le `Canvas` déjà présent, sans animation ajoutée. Les prévisions sont déjà chargées pour la ligne météo. La lecture du réveil est ponctuelle (à l'affichage et sur diffusion système), sans verrou de réveil, alarme exacte ni boucle : `BatteryRulesTest` doit rester vert.

**Risques**

- Lisibilité : le bleu pastel à 95 % sur Brume claire. Vérifier le contraste sur les captures avec une pochette claire.
- Encombrement à droite : noms de ciel, heures de rendez-vous et texte du réveil se disputent la même colonne de 78 dp (`VERTICAL_WIDTH`).
- Températures en texte vertical : à valider sur le téléphone, elles peuvent se lire mal.
- Alarmes d'apps tierces : `getNextAlarmClock` ne voit que les alarmes déclarées comme réveil, pas tous les rappels.
- CI : réparée le 14 octobre (feuille de route, point 0.1). Android 16 bloque, Android 17 est signalé sans bloquer (point 0.2). Un nouveau test qui vise « Voir la journée » doit viser le bouton de l'en-tête par son rôle : la ligne du temps porte la même action.

**Questions ouvertes**

- [ ] Échelle de la ligne : la nuit (minuit → réveil) est-elle comprimée (par exemple 15 % de la hauteur) ou à la même échelle que le jour ? La maquette la comprime.
- [ ] Réveil au-delà de 24 h ou absent : la ligne finit-elle à minuit comme aujourd'hui ? (proposé : oui)
- [ ] Toucher l'anneau : ouvrir l'horloge, ou une petite feuille pour décaler le réveil ?
- [x] Variante horizontale : supprimée (point 1.1 de la feuille de route, tranché le 14 octobre).
- [x] Vent : rangé dans « Sec » (tranché le 14 octobre).
- [x] Neige : blanc à 55 % (tranché le 14 octobre).

## Références

- Maquette : `design/prototype/meteo-sur-le-fil.html`, copie de l'artboard « C · La météo sur le fil (anneau) » de la planche de design « Bercail — pistes accueil » (sur claude.ai). Les autres pistes (A · Ciel, B · Fil à plomb, version pilule) sont écartées.
- `CLAUDE.md` (règles), `docs/handoff.md` (décisions), `docs/CATALOG.md` (où est quoi).
- [How to turn your AI into a world-class designer](https://www.lennysnewsletter.com/p/how-to-turn-your-ai-into-a-world) (Lenny's Newsletter) : méthode suivie, notamment la soustraction (retirer la ligne météo plutôt qu'ajouter un widget).
