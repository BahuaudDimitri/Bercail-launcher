# :data:settings

Les réglages de Bercail, enregistrés sur le téléphone (DataStore), derrière l'interface `SettingsSource` du domaine.

- **Expose** : `StoredSettings`, et `settingsStore(file)` qui ouvre le fichier des réglages.
- **Ne doit pas** : dépendre d'Android (l'app lui donne l'emplacement du fichier), ni contenir de règle métier ou de valeur par défaut : celles-ci sont dans `Settings`, côté domaine.
- **Batterie** : le fichier n'est lu que lorsque quelqu'un écoute les réglages, puis gardé en mémoire ; il n'est réécrit que lorsqu'un réglage change.
- **Tests** : `StoredSettingsTest` (valeurs par défaut, changements, survie à un redémarrage, valeur inconnue ignorée).
