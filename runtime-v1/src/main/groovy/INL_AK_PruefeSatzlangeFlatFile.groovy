import com.sap.gateway.ip.core.customdev.util.Message

Message processData(Message message) {
    pruefeSatzlaenge(message.getBody(java.io.Reader))
    return message
}

void pruefeSatzlaenge(java.io.Reader reader) {
    int expectedLength = 366
    reader.eachLine { String line, int lineNumber ->
        if (line.length() != expectedLength) {
            throw new IllegalArgumentException("Datensatzlaenge Fehlerhaft. Laenge muss 366 sein ist aber : " + line.length() + " in Zeile :  " + lineNumber)
        }
    }
}
