// Mapping-Hilfsfunktionen für Runtime 1.x.

String padLeftString(String string, int toLength, String fillChar) {
    return string.padLeft(toLength, fillChar)
}

String padRightString(String string, int toLength, String fillChar) {
    return string.padRight(toLength, fillChar)
}

String padRightStringSpace(String string, int toLength) {
    return string.padRight(toLength, ' ')
}

String padLeftStringSpace(String string, int toLength) {
    return string.padLeft(toLength, ' ')
}

String padRightZero(String string, int toLength) {
    return string.padRight(toLength, '0')
}

String padLeftZero(String string, int toLength) {
    return string.padLeft(toLength, '0')
}

String versionBestimmen() {
    return "Groovy: ${GroovySystem.getVersion()}\r\nJava: ${System.getProperty('java.version')}\r\n"
}
