# :core:testing

Les faux (fakes) des sources de données du domaine, réutilisés par les tests et les aperçus. On préfère des faux à des mocks.

- **Expose** : une implémentation factice par interface de `:core:domain` (`FakeClock`, `FakeAgendaSource`, `FakeWeatherSource`, `FakeMessagesSource`, `FakeHomeSource`, `FakeMediaSource`, `FakeSettingsSource`, `FakeAppsSource`, `FakePhone`), et `FakeWorld`, qui les remplit avec les données du prototype à l'un de ses trois moments (matin, trajet, soir).
- **Ne doit pas** : contenir de règle métier (elles vont dans `:core:domain`), ni dépendre d'Android, ni entrer dans la version publiée de l'app.
- **Version de mise au point** : l'app debug affiche ces faux pour les sources pas encore branchées, afin de voir et d'essayer tous les états sur le téléphone. La version publiée montre à la place les états vides (`Unconnected`, dans le domaine).
