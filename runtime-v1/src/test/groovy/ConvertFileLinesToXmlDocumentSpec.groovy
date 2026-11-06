import spock.lang.Specification
import com.sap.gateway.ip.core.customdev.util.Message
class ConvertFileLinesToXmlDocumentSpec extends Specification {
    private final def script = new convertFileLinesToXmlDocument()

    def "converts lines and escapes XML in body and headers"() {
        given:
        def message = new Message()
        message.setBody('A & B\n<test>')
        message.setHeaders([CamelFileName: 'a&b.txt', SAP_MessageProcessingLogID: '123'])

        when:
        def result = script.processData(message)
        def xml = new XmlSlurper().parseText(message.getBody(String))

        then:
        result.is(message)
        xml.Head.FileName.text() == 'a&b.txt'
        xml.Head.MessageID.text() == '123'
        xml.Body.Line.collect { it.text() } == ['A & B', '<test>']
    }
    def "supports empty input without optional headers"() {
        given:
        def message = new Message()
        message.setBody('')

        when:
        script.processData(message)

        then:
        new XmlSlurper().parseText(message.getBody(String)).Body.Line.size() == 0
        new XmlSlurper().parseText(message.getBody(String)).Head.children().size() == 0
    }
}
