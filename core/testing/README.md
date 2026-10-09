# :core:testing

Les faux (fakes) des sources de données du domaine, réutilisés par les tests et les aperçus. On préfère des faux à des mocks.

- **Expose** : une implémentation factice par interface de `:core:domain`.
- **Ne doit pas** : être utilisé par le code de production.
