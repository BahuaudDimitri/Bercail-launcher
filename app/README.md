# :app

L'application Bercail : l'activité déclarée à Android comme écran d'accueil, qui assemble les modules.

- **Expose** : `MainActivity` (launcher : catégorie HOME, une seule instance, retour bloqué, fond d'écran visible ; elle affiche l'accueil, glisse la recherche dans son tiroir et pose les réglages par-dessus ; le bouton Accueil ramène toujours à l'accueil simple), `BercailApplication` et `Sources` (toutes les sources, branchées une seule fois), `DeviceClock` (l'heure du téléphone).
- **Sources réelles** : heure, apps installées, liens vers le téléphone, réglages enregistrés. Les autres attendent leur vague : la version publiée (`src/release`) montre leurs états vides, la version de mise au point (`src/debug`) les données du prototype, pour tout voir et tout essayer sur le téléphone.
- **Ne doit pas** : contenir de règles métier (elles vont dans `:core:domain`), d'écran (ils vont dans leur module `:feature:…`) ni de composants visuels réutilisables (ils vont dans `:core:designsystem`). La version publiée n'embarque aucun faux.
- **Tests** : `LauncherUiTest` et `DeviceClockUiTest` (Robolectric), `LauncherDeviceTest` (sur émulateur ou téléphone).
