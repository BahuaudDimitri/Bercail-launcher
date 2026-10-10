# :core:testing

Les faux (fakes) des sources de données du domaine, réutilisés par les tests et les aperçus. On préfère des faux à des mocks.

- **Expose** : une implémentation factice par interface de `:core:domain` (`FakeClock`, `FakeAgendaSource`, `FakeWeatherSource`, `FakeMessagesSource`, `FakeHomeSource`, `FakeMediaSource`, `FakeSettingsSource`), et `FakeWorld`, qui les remplit avec les données du prototype à l'un de ses trois moments (matin, trajet, soir).
- **Ne doit pas** : contenir de règle métier (elles vont dans `:core:domain`), ni dépendre d'Android.
- **Provisoire** : tant qu'une vraie source n'existe pas (vagues 4 à 9), l'app affiche son faux. Chaque vague en remplace un.
