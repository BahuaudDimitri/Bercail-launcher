# :feature:settings

« Réglages de Bercail » : le panneau qui s'ouvre depuis la recherche.

- **Expose** : `SettingsRoute`, `SettingsScreen` (fond sans musique, agenda du téléphone et lieu des prévisions météo à autoriser, ligne du temps, tiroir au départ, apps favorites, mise à jour de Bercail, liens vers Android), `FavoriteAppsScreen` (la liste où l'on coche ses apps favorites), `SettingsViewModel`.
- **Ne doit pas** : connaître où les réglages sont enregistrés (il ne voit que `SettingsSource`), ni proposer un réglage qui ne fait encore rien : favoris, commandes maison et app média arriveront avec leurs vagues.
- **Batterie** : réglages, liste des apps et état de la mise à jour ne sont écoutés que lorsque le panneau est affiché ; chaque changement est enregistré une fois, tout de suite.
- **Tests** : `SettingsUiTest`, `SettingsScreenshotTest`.
