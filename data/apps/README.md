# :data:apps

Les apps installées sur le téléphone et les liens vers l'extérieur, derrière les interfaces `AppsSource` et `Phone` du domaine.

- **Expose** : `DeviceApps` (liste des apps qu'on peut ouvrir, leur icône, leur dessin d'une seule couleur pour le dock, leur lancement, leurs raccourcis par `DeviceShortcuts`) et `DevicePhone` (recherche web, Play Store, Paramètres du téléphone, choix de l'écran d'accueil, page de Bercail dans les Paramètres).
- **Ne doit pas** : demander l'accès à la liste complète des paquets (`QUERY_ALL_PACKAGES`) : seules les apps qui s'ouvrent depuis un launcher sont visibles, déclarées dans son manifeste.
- **Batterie** : Android n'est interrogé que lorsque quelqu'un écoute la liste (recherche ouverte), puis prévient lui-même des installations et suppressions ; rien ne reste enregistré ensuite. Noms et icônes sont lus hors du fil principal. Les raccourcis d'une app ne sont lus qu'au toucher long sur son icône du dock, sans rien écouter ni garder ; Android ne les donne qu'à l'écran d'accueil par défaut (`hasShortcutHostPermission`), sinon il n'y en a aucun.
- **Tests** : `DeviceAppsUiTest`, `AppGlyphsUiTest`, `AppShortcutsUiTest` (Robolectric, avec `StartedShortcuts` qui note les raccourcis lancés).
