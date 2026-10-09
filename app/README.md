# :app

L'application Bercail : l'activité déclarée à Android comme écran d'accueil, qui assemble les modules.

- **Expose** : `MainActivity` (launcher : catégorie HOME, une seule instance, retour bloqué, fond d'écran visible) et l'écran d'accueil.
- **Ne doit pas** : contenir de règles métier (elles vont dans `:core:domain`) ni de composants visuels réutilisables (ils vont dans `:core:designsystem`).
- **Tests** : `LauncherUiTest` (Robolectric), `HomeScreenshotTest` (capture Roborazzi), `LauncherDeviceTest` (sur émulateur ou téléphone).
