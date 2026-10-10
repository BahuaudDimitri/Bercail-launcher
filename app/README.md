# :app

L'application Bercail : l'activité déclarée à Android comme écran d'accueil, qui assemble les modules.

- **Expose** : `MainActivity` (launcher : catégorie HOME, une seule instance, retour bloqué, fond d'écran visible), qui affiche l'accueil de `:feature:home`, et `DeviceClock`, l'heure du téléphone.
- **Branche les sources** dans `HomeSources.kt`. Seule l'heure est réelle pour l'instant : chaque autre source est le faux du prototype (`:core:testing`) jusqu'à sa vague.
- **Ne doit pas** : contenir de règles métier (elles vont dans `:core:domain`), d'écran (ils vont dans leur module `:feature:…`) ni de composants visuels réutilisables (ils vont dans `:core:designsystem`).
- **Tests** : `LauncherUiTest` et `DeviceClockUiTest` (Robolectric), `LauncherDeviceTest` (sur émulateur ou téléphone).
