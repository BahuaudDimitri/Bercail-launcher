# :data:apps

Les apps installées sur le téléphone et les liens vers l'extérieur, derrière les interfaces `AppsSource` et `Phone` du domaine.

- **Expose** : `DeviceApps` (liste des apps qu'on peut ouvrir, leur icône, leur lancement) et `DevicePhone` (recherche web, Play Store, Paramètres du téléphone, choix de l'écran d'accueil).
- **Ne doit pas** : demander l'accès à la liste complète des paquets (`QUERY_ALL_PACKAGES`) : seules les apps qui s'ouvrent depuis un launcher sont visibles, déclarées dans son manifeste.
- **Batterie** : Android n'est interrogé que lorsque quelqu'un écoute la liste (recherche ouverte), puis prévient lui-même des installations et suppressions ; rien ne reste enregistré ensuite. Noms et icônes sont lus hors du fil principal.
- **Tests** : `DeviceAppsUiTest` (Robolectric).
