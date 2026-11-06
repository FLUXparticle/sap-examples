import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    def props = message.getProperties()
    def header = message.getHeaders()

    def Dateiname = header.get("CamelFileName")
    def MessageID = header.get("SAP_MessageProcessingLogID")
    def Elterngeldstelle = props.get("Elterngeldstelle")
    def ErreichbarkeitES = props.get("ErreichbarkeitES")
    def Prüfinstanz = props.get("Prüfinstanz")
    def ErreichbarkeitPI = props.get("ErreichbarkeitPI")
    def Erstellungsdatum = props.get("Erstellungsdatum")
    def Rückweisungscode = props.get("Rückweisungscode")
    def Beschreibung = props.get("Beschreibung")
    def NachrichtenID = props.get("NachrichtenID")
    def Sendezeitpunkt = props.get("Sendezeitpunkt")
    def ErgänzendeHinweise = props.get("ErgänzendeHinweise")

    final String NL = "\r\n"
    def content = new StringBuilder()

    content.append("Sehr geehrte Damen und Herren,").append(NL).append(NL)

    content.append("Dateiname: ${Dateiname}").append(NL).append(NL)
    content.append("MessageID: ${MessageID}").append(NL)
    content.append("Elterngeldstelle: ${Elterngeldstelle}").append(NL)

    if (ErreichbarkeitES) {
        content.append("Erreichbarkeit Elterngeldstelle: ${ErreichbarkeitES}").append(NL)
    }
    if (Prüfinstanz) {
        content.append("Prüfinstanz: ${Prüfinstanz}").append(NL)
    }
    if (ErreichbarkeitPI) {
        content.append("Erreichbarkeit Prüfinstanz: ${ErreichbarkeitPI}").append(NL)
    }

    content.append("Erstellungsdatum: ${Erstellungsdatum}").append(NL).append(NL)

    content.append("Fehlercode: ${Rückweisungscode}").append(NL)

    if (Beschreibung) {
        content.append("${Beschreibung}").append(NL)
    }

    content.append(NL)

    if (NachrichtenID) {
        content.append("${NachrichtenID}").append(NL)
    }
    if (Sendezeitpunkt) {
        content.append("${Sendezeitpunkt}").append(NL)
    }
    if (ErgänzendeHinweise) {
        content.append("${ErgänzendeHinweise}").append(NL)
    }

    content.append(NL)
    content.append("Mit freundlichen Grüßen").append(NL)
    content.append(NL)
    content.append("Sozialversicherung für Landwirtschaft, Forsten und Gartenbau").append(NL)
    content.append("- Bereich Informationstechnik -").append(NL)
    content.append("Weißensteinstraße 70-72").append(NL)
    content.append("34131 Kassel").append(NL)
    content.append("Tel.: 0561 785-0").append(NL)
    content.append("Internet: www.SVLFG.de")


    message.setBody(content.toString())

    return message
}
