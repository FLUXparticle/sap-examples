import com.sap.it.script.v2.api.Message
import spock.lang.Specification

class ValidateMeldungenSpec extends Specification {
    private final def script = new validateMeldungen()

    def "rejects VSDD record with a timestamp shorter than 14 characters"() {
        given:
        def resource = getClass().getResource('/meldungen.txt')
        assert resource != null
        def message = new Message()
        message.setBody(resource.bytes)

        when:
        script.processData(message)

        then:
        def error = thrown(AssertionError)
        error.message.contains("VSDD 25041: Spalte 4 muss 14 Zeichen enthalten, ist aber '20260213'")
    }
}
