# SAP Cloud Integration: Groovy-Beispiele

Groovy-Beispiele aus dem Kurs sowie Beispiele zur Meldungsvalidierung und CMS-Extraktion mit Spock-Tests. Zwei getrennte Maven-Module verhindern, dass sich die Groovy-Versionen auf dem Test-Classpath vermischen.

| Modul | Skriptversion | Groovy | Spock | Beispiele |
| --- | --- | --- | --- | --- |
| `runtime-v1` | 1.x | 2.4.21 | 1.3 | `buildMailContent`, `convertFileLinesToXmlDocument`, `getMonthGerman`, `padSpace`, `INL_AK_PruefeSatzlangeFlatFile` |
| `runtime-v2` | 2.x | 4.0.29 | 2.3 | `buildZipFile`, `validateInputParameters`, `validateMeldungen`, `extractCmsContent` |

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

## CMS-Nutzdaten ohne zusätzliche Bibliothek extrahieren

`extractCmsContent.groovy` ist ein eingeschränkter Prototyp für binäres, unverschlüsseltes CMS `SignedData` mit angehängtem `id-data`-Inhalt. Er liest ASN.1-Tags und Längen, navigiert zum eingebetteten OCTET STRING und kopiert die Nutzdaten bytegenau. Auch konstruierte OCTET STRINGs und BER-Strukturen mit unbestimmten Längen und End-of-Contents-Markern werden verarbeitet. Zertifikate und Signaturdaten werden übersprungen, nicht geprüft.

Unterstützt werden feste ASN.1-Längen und unbestimmte Längen für konstruierte Werte (DER/BER). Nicht unterstützt: Detached-Signaturen, Verschlüsselung, PEM/Base64/MIME-Verpackung sowie andere PKCS#7-Inhaltstypen. Verschachtelung und Längengrenzen werden geprüft. Das ist kein vollständiger CMS-Validator. Eine erfolgreiche Extraktion sagt nichts über Gültigkeit oder Vertrauenswürdigkeit der Signatur aus.

- `processData(message)` liest einen InputStream und setzt den extrahierten Inhalt als `byte[]`-Body. Diese SAP-Einstiegsmethode puffert die gesamte Ausgabe im Speicher.
- `extractContent(input, output)` kopiert dagegen mit einem 8-KiB-Puffer und eignet sich für große Nutzdaten, wenn auch Quelle und Ziel gestreamt werden. Der Aufrufer schließt beide Streams. Bei einem Fehler muss bereits geschriebene Ausgabe verworfen werden.

`ExtractCmsContentSpec` prüft eine echte OpenSSL-Signatur, blockweise Binärdaten, fehlende Nutzdaten, abgeschnittene Daten, falsche Längen und nicht unterstützte Formate. `src/test/resources/cms/attached.der` enthält ein selbstsigniertes Testzertifikat und die Bytes von `payload.xml`; der private Testschlüssel wird nicht im Projekt gespeichert. Zusätzlich erzeugt Bouncy Castle (`bcpkix-jdk18on:1.83`, ausschließlich Test-Scope) frische RSA-2048-Schlüssel und ein kurzlebiges selbstsigniertes Zertifikat im Speicher. Damit signieren die Tests XML und 100.003 Byte Binärdaten mit SHA256withRSA und prüfen die erzeugte Signatur mit Bouncy Castle. Getestet werden sowohl feste Längen als auch die BER-Streaming-Ausgabe mit 1-KiB-CMS-Blöcken, einschließlich Nullbytes an Block- und Kopierpuffergrenzen. Die extrahierten Bytes müssen exakt der Eingabe entsprechen. Es wird weder ein Keystore benötigt noch ein globaler Security-Provider registriert. Die kleinen Fehlerfall-Fixtures sind rein strukturelle Container ohne echte Signatur.
