import spock.lang.Specification
import com.sap.it.script.v2.api.Message
import spock.lang.Unroll
class ValidateInputParametersSpec extends Specification {
    private final def script = new validateInputParameters()

    static Message validMessage() {
        def message = new Message()
        message.setHeaders([NumGen_MessageId: 'id', NumGen_ProcessSign: 'P', NumGen_Receiver: 'receiver', NumGen_MaxNumber: '100'])
        ['NumGen_ExpectNumber', 'NumGen_RetentionNumbers', 'NumGen_RetentionMessageId'].each {
            message.setProperty(it + '_Default', '10')
        }
        return message
    }

    def "sets numeric defaults and normalizes null receiver"() {
        given:
        def message = validMessage()
        message.setHeader('NumGen_Receiver', ' NULL ')

        when:
        def result = script.processData(message)

        then:
        result.is(message)
        message.getHeaders().NumGen_Receiver == null
        message.getHeaders().NumGen_ExpectNumber == '10'
        message.getHeaders().NumGen_RetentionNumbers == '10'
        message.getHeaders().NumGen_RetentionMessageId == '10'
    }

    def "preserves explicit values"() {
        given:
        def message = validMessage()
        message.setHeader('NumGen_ExpectNumber', '7')

        when:
        script.processData(message)

        then:
        message.getHeaders().NumGen_ExpectNumber == '7'
        message.getHeaders().NumGen_Receiver == 'receiver'
    }

    @Unroll
    def "rejects missing required header #header"() {
        given:
        def message = validMessage()
        def headers = new HashMap(message.getHeaders())
        headers.remove(header)
        message.setHeaders(headers)

        when:
        script.processData(message)

        then:
        thrown(IllegalArgumentException)

        where:
        header << ['NumGen_MessageId', 'NumGen_ProcessSign', 'NumGen_Receiver', 'NumGen_MaxNumber']
    }

    @Unroll
    def "rejects invalid #header value #value"() {
        given:
        def message = validMessage()
        message.setHeader(header, value)

        when:
        script.processData(message)

        then:
        thrown(IllegalArgumentException)

        where:
        header | value
        'NumGen_Receiver' | '  '
        'NumGen_MaxNumber' | 'abc'
        'NumGen_ExpectNumber' | 'abc'
        'NumGen_RetentionNumbers' | 'abc'
        'NumGen_RetentionMessageId' | 'abc'
    }
}
