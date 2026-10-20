# :feature:settings

« Réglages de Bercail » : le panneau qui s'ouvre depuis la recherche.

- **Expose** : `SettingsRoute`, `SettingsScreen` (fond sans musique, accès à autoriser d'une pilule « Autoriser », « Autorisé » une fois accordés et la ligne ouvre alors la page d'Android pour les retirer, ligne du temps, feuille des gens et de la maison au départ, dock, mise à jour de Bercail, liens vers Android), `DockScreen` (les apps du dock avec leur place et une poignée pour changer leur ordre, puis les autres : cinq au plus), `FavoritePeopleScreen` (les conversations de Beeper où l'on coche ses quatre personnes favorites au plus), `IconPackScreen` (le pack d'icônes du dock : « Aucun » ou un pack installé), `MediaAppScreen` (l'app média principale), `PlaceScreen` (le lieu des prévisions météo : la position approximative du téléphone, ou une ville cherchée par son nom), `SettingsViewModel`.
- **Ne doit pas** : connaître où les réglages sont enregistrés (il ne voit que `SettingsSource`), ni proposer un réglage qui ne fait encore rien : les commandes maison arriveront avec leur vague.
- **Batterie** : réglages, liste des apps, packs d'icônes et état de la mise à jour ne sont écoutés que lorsque le panneau est affiché ; chaque changement est enregistré une fois, tout de suite.
- **Tests** : `SettingsUiTest`, `SettingsScreenshotTest`.
