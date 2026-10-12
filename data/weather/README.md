# :data:weather

La météo du lieu où se trouve le téléphone, derrière les interfaces `ForecastService`, `PlaceSource` et `ForecastJournal` du domaine. Les règles (une requête par heure au plus, seulement accueil affiché, prévision gardée six heures, phrases) sont dans `WeatherWatch` et `weatherAt`, dans `:core:domain`.

- **Expose** : `OpenMeteo` (les prévisions heure par heure sur deux jours, lues sur l'API publique d'Open-Meteo, sans compte ni clé ; une requête manquée dit pourquoi à qui l'écoute, sans jamais dire où est le téléphone), `OpenMeteoCities` (la recherche d'une ville par son nom, seulement quand l'utilisateur la demande), `PhonePosition` (la position approximative du téléphone) et `StoredForecast` (la dernière prévision et son heure, dans un fichier).
- **Vie privée** : seule l'autorisation de position *approximative* est déclarée, jamais la position précise. La position n'est envoyée à Open-Meteo qu'arrondie au centième de degré (environ un kilomètre), sans rien qui nomme le téléphone ou son utilisateur. Rien n'est lu avant l'accord de l'utilisateur.
- **Batterie** : ce module ne décide de rien et ne programme rien ; il ne travaille que lorsqu'on l'appelle. La position vient d'abord de ce qu'Android sait déjà (gratuit) ; au-delà de six heures, une seule question au téléphone, par le réseau et jamais par le GPS, sans attendre plus de dix secondes.
- **Tests** : `OpenMeteoUiTest` et `OpenMeteoCitiesUiTest` (Robolectric, avec un serveur local qui rejoue les réponses enregistrées d'Open-Meteo), `PhonePositionUiTest` (Robolectric), `StoredForecastTest`.
