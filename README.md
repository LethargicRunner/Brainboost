# Brainboost

Générateur de sons binauraux (Kotlin, Jetpack Compose). Casque stéréo obligatoire.

Reprise du projet **Binaural** (app archivée, code uniquement en local) : reconstruit de zéro avec les mêmes fonctions.

- Presets : Delta, Theta, Alpha, Focus 10/12/15 (approximations, non officielles)
- Battement (0,5–40 Hz), porteuse (20–1000 Hz) et durée (0 = illimitée) réglables
- Lecture écran éteint (service de premier plan, notification avec arrêt)
- Zéro dépendance : sinus générés en PCM 16 bits à la volée (BinauralGenerator)
- APK compilé par GitHub Actions : onglet Actions > dernier run > Artifacts
