# SAP Cloud Integration: Groovy-Beispiele

Sieben bereinigte Kursbeispiele und ein Beispiel zur Meldungsvalidierung mit Spock-Tests. Zwei getrennte Maven-Module verhindern, dass sich die Groovy-Versionen auf dem Test-Classpath vermischen.

| Modul | Skriptversion | Groovy | Spock | Beispiele |
| --- | --- | --- | --- | --- |
| `runtime-v1` | 1.x | 2.4.21 | 1.3 | `buildMailContent`, `convertFileLinesToXmlDocument`, `getMonthGerman`, `padSpace`, `INL_AK_PruefeSatzlangeFlatFile` |
| `runtime-v2` | 2.x | 4.0.29 | 2.3 | `buildZipFile`, `validateInputParameters`, `validateMeldungen` |

Die Originalimporte der SAP-Message-APIs bleiben erhalten. Die zwei Hilfsskripte ohne explizite Runtime-Angabe (`getMonthGerman`, `padSpace`) sind 1.x zugeordnet. SAP dokumentiert die neue Runtime in [Upgrading Script](https://help.sap.com/docs/integration-suite/isuite-integrations-and-apis/upgrading-groovy-script?locale=en-US).

## Tests ausführen

Voraussetzungen: Maven und JDK 17. Unter macOS, wenn das Standard-JDK neuer ist:

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test
```

Einzelne Runtime oder einzelner Test:

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -pl runtime-v1 test
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -pl runtime-v2 -Dtest=BuildZipFileSpec test
```

Die Skripte liegen unter `runtime-v*/src/main/groovy`, die Tests unter `runtime-v*/src/test/groovy`. GMavenPlus registriert beide Quellverzeichnisse und kompiliert die Skripte mit der Groovy-Version des jeweiligen Moduls. Jeder Test erzeugt die passende kompilierte Skriptklasse direkt, beispielsweise `new buildZipFile()`. Groovy leitet den Klassennamen aus dem Dateinamen ab; die SAP-Einstiegsmethoden bleiben unverändert. Dadurch sind die Referenzen auch für Navigation und Umbenennungen in IntelliJ sichtbar. Testberichte entstehen in den jeweiligen `target/surefire-reports`-Verzeichnissen.

Beide Module verwenden `com.equaliseit:sap-cpi-mocks` als Compile-Abhängigkeit für die SAP-Message-Typen: **1.0.3** für Runtime 1.x (Groovy 2.4.21) und **2.0.0** für Runtime 2.x (Groovy 4.0.29). Die Tests erzeugen die Message-Objekte direkt aus diesen Bibliotheken; eigene SAP-Message-Klassen sind nicht erforderlich. Dabei wird auch die Body-Konvertierung der Mocks genutzt, etwa von String zu Reader.

Nach Änderungen an den POM-Dateien in IntelliJ **Reload All Maven Projects** ausführen und JDK 17 als Project SDK und Maven Runner JRE auswählen. Die Specs lassen sich direkt über Run/Debug starten.

Die Tests prüfen lokale Skriptlogik mit unterschiedlichen Groovy-Versionen, keine vollständige SAP-Laufzeit. Beide Module laufen lokal auf JDK 17; Java-8-Kompatibilität und die übrigen SAP-Dienste werden damit nicht nachgewiesen.

## Meldungen als Byte-Body validieren

`ValidateMeldungenSpec` liest `runtime-v2/src/test/resources/meldungen.txt` unverändert als `byte[]` und setzt diese Bytes als Message-Body. Der Test führt keine Zeichensatz-Konvertierung durch; der Mock stellt den Byte-Body als InputStream bereit. `validateMeldungen` liest den Body als InputStream und übergibt ihn an `zeigeZeilen`. Dort wird er über einen ISO-8859-1-Reader als Zeilenstream verarbeitet. Das Skript filtert im Zeilenstream die `VSDD`-Sätze und prüft in `forEach` per Assertion, dass die vierte Spalte 14 Zeichen enthält. Der Test erwartet einen `AssertionError` für Satz `25041`, dessen vierte Spalte nur `20260213` enthält. Stream und Reader werden im `finally`-Block geschlossen.
