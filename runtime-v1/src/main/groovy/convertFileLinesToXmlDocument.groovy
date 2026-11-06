import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.XmlUtil
import groovy.xml.MarkupBuilder

def Message processData(Message message) {
    def reader = message.getBody(java.io.Reader)

    def xml = XmlUtil.serialize(toXML(message, reader))

    message.setBody(xml)
    return message
}

String toXML(Message message, java.io.Reader reader) {
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)
    xml.Document {
        Head {
            def headers = message.getHeaders()
            if (headers.SAP_MessageProcessingLogID != null) MessageID(headers.SAP_MessageProcessingLogID)
            if (headers.CamelFileName != null) FileName(headers.CamelFileName)
        }
        Body {
            reader.eachLine { String line -> Line(line) }
        }
    }
    return writer.toString()
}
