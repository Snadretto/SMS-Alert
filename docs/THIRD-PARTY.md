# Abhängigkeiten und eigene Assets

Die App bindet keine Netzwerk-, Analyse-, Werbe-, Push- oder Remote-Crash-SDKs ein.
Die Release-Konfiguration verwendet folgende Bibliotheksfamilien:

| Bestandteil | Zweck | Lizenz / Hinweis |
| --- | --- | --- |
| Flutter / Dart / flutter_localizations | Oberfläche, Engine und deutsche Systemtexte | BSD und die jeweils gebündelten Drittlizenzen; Flutter erzeugt das lokale Lizenzregister aus den SDK-/Paket-Lizenzdateien |
| Material Icons / Roboto aus Flutter | Lokale Symbole und Schrift | Apache-2.0; lokale Flutter-Lizenzanzeige |
| Google libphonenumber 9.0.42 | Lokale Rufnummernnormalisierung; keine Nummernabfrage im Internet | Apache-2.0, The Libphonenumber Authors |
| Kotlin-Standardbibliothek | Kotlin-Laufzeit | Apache-2.0, JetBrains / Kotlin contributors |
| AndroidX (transitiv über Flutter Android Embedding) | Activity, Lifecycle, Window, Core, Startup, ProfileInstaller usw. | Apache-2.0, Android Open Source Project / AndroidX contributors |
| JUnit, AndroidX Test, flutter_test, flutter_lints | Nur Build-/Testwerkzeuge | Nicht als Test-Framework in der Release-App ausgeführt; jeweilige Open-Source-Lizenzen |

Die genaue Paketauflösung ergibt sich aus `app/pubspec.lock` und dem Gradle-Bericht
`./gradlew :app:dependencies --configuration releaseRuntimeClasspath`.
Flutter-Engine und Android-Embedding sind an den Flutter-SDK-Stand gebunden.

Im lokalen Lizenzbildschirm werden Flutter-Lizenzen sowie ein zusätzlicher
Apache-Lizenz-/Urheberhinweis für die nativen Bibliotheken geladen. Root-Lizenz
und NOTICE von SMS-Alert werden ebenfalls gebündelt. Die Projektlizenz ersetzt
keine Urheberhinweise von Bibliotheken.

Die beiden PCM-Alarmtöne sind selbst erzeugte Assets. Ihr Quellgenerator liegt
in `tools/generate_tones.py`. App-/Benachrichtigungssymbole sind lokale
VectorDrawables beziehungsweise Material Icons. Keine heruntergeladenen
Alarmaufnahmen, Online-Schriften oder Remote-Assets.

Quellen: [libphonenumber](https://github.com/google/libphonenumber),
[Kotlin](https://github.com/JetBrains/kotlin),
[AndroidX](https://android.googlesource.com/platform/frameworks/support/),
[Flutter](https://github.com/flutter/flutter).
