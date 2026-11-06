import spock.lang.Specification
import com.sap.gateway.ip.core.customdev.util.Message
class PruefeSatzlaengeSpec extends Specification {
    private final def script = new INL_AK_PruefeSatzlangeFlatFile()

    def "accepts records with exactly 366 characters"() {
        given:
        def message = new Message()
        message.setBody(('x' * 366) + '\n' + ('y' * 366))

        expect:
        script.processData(message).is(message)
    }
    def "reports length and number of invalid record"() {
        when:
        script.pruefeSatzlaenge(new StringReader(('x' * 366) + '\nabc'))

        then:
        def error = thrown(IllegalArgumentException)
        error.message == 'Datensatzlaenge Fehlerhaft. Laenge muss 366 sein ist aber : 3 in Zeile :  2'
    }
}
