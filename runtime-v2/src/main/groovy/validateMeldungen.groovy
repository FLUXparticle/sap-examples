import com.sap.it.script.v2.api.Message
import java.nio.charset.StandardCharsets
import java.util.stream.Stream

Message processData(Message message) {
    InputStream input = message.getBody(InputStream)
    zeigeZeilen(input)
    return message
}

void zeigeZeilen(InputStream input) {
    BufferedReader reader = input.newReader(StandardCharsets.ISO_8859_1.name())
    Stream<String> zeilen = reader.lines()
    try {
        zeilen.forEach { zeile ->
            println zeile
            if (zeile.startsWith('VSDD;')) {
                def felder = zeile.split(';', -1)
                assert felder[3].length() == 14 :
                    "VSDD ${felder[4]}: Spalte 4 muss 14 Zeichen enthalten, ist aber '${felder[3]}'"
            }
        }
    } finally {
        zeilen.close()
        reader.close()
    }
}
