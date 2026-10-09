# :core:domain

Les règles métier de Bercail et les interfaces de ses sources de données (Beeper, média, agenda, Hue, météo, apps, réglages), en Kotlin pur.

- **Expose** : modèles, règles, interfaces des sources.
- **Ne doit pas** : dépendre d'Android (vérifié par `ModuleRulesTest`), ni lire l'heure réelle (horloge injectée).
- **Tests** : couverture minimale de 90 % des lignes (Kover, bloquant).
