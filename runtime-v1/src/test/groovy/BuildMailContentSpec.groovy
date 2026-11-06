import spock.lang.Specification
import com.sap.gateway.ip.core.customdev.util.Message
class BuildMailContentSpec extends Specification {
    private final def script = new buildMailContent()

    def "builds mail and includes optional details only when present"() {
        given:
        def message = new Message()
        message.setHeaders([CamelFileName: 'daten.txt', SAP_MessageProcessingLogID: 'id-123'])
        message.setProperties([Elterngeldstelle: 'Berlin', Erstellungsdatum: '2026-10-07', Rückweisungscode: 'E01', Beschreibung: 'Ungültiger Satz', ErreichbarkeitES: 'kontakt@example.org'])

        when:
        def result = script.processData(message)
        def body = message.getBody(String)

        then:
        result.is(message)
        body.startsWith('Sehr geehrte Damen und Herren,\r\n\r\n')
        body.contains('Dateiname: daten.txt\r\n')
        body.contains('MessageID: id-123\r\n')
        body.contains('Erreichbarkeit Elterngeldstelle: kontakt@example.org\r\n')
        body.contains('Fehlercode: E01\r\nUngültiger Satz\r\n')
        !body.contains('Prüfinstanz:')
        body.endsWith('Internet: www.SVLFG.de')
    }
}
