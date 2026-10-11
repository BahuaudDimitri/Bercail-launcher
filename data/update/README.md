# :data:update

La mise à jour de Bercail par lui-même, derrière les interfaces `ReleaseFeed`, `UpdatePackages` et `UpdateJournal` du domaine. Les règles (une lecture par jour, installation quand l'accueil est caché) sont dans `Updater`, dans `:core:domain`.

- **Expose** : `GitHubReleases` (la dernière release, lue sur l'API publique de GitHub, sans compte ni jeton), `PhonePackages` (téléchargement du paquet, vérification de sa taille et de son empreinte, remise à Android par `PackageInstaller`) et `StoredUpdateNotes` (date de la dernière lecture et version trouvée, dans un fichier).
- **Sécurité** : seul un paquet hébergé dans les releases du dépôt de Bercail, en https, est accepté. Android vérifie ensuite lui-même qu'il est signé comme Bercail et qu'il n'est pas plus ancien. Aucun identifiant n'est envoyé.
- **Batterie** : ce module ne décide de rien et ne programme rien ; il ne travaille que lorsqu'on l'appelle, et un téléchargement s'interrompt dès que l'accueil est caché.
- **Tests** : `GitHubReleasesUiTest` et `PhonePackagesUiTest` (Robolectric, avec un serveur local qui rejoue la réponse enregistrée de GitHub), `StoredUpdateNotesTest`.
