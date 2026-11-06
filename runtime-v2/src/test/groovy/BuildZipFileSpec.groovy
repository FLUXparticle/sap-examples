import spock.lang.Specification
import com.sap.it.script.v2.api.Message
import java.util.zip.ZipInputStream
class BuildZipFileSpec extends Specification {
    private final def script = new buildZipFile()

    def "creates value mapping archive with UTF-8 contents"() {
        given:
        def message = new Message()
        message.setBody('<values>Ä &amp; Ö</values>')
        message.setProperty('Date', '2026.10.7')
        message.setProperty('Timestamp_Meta', '2026-10-07')
        message.setProperty('VM_Name', 'Zuordnung')

        when:
        def result = script.processData(message)
        def entries = [:]
        new ZipInputStream(new ByteArrayInputStream(message.getBody(byte[]))).withCloseable { zip ->
            def entry
            while ((entry = zip.nextEntry) != null) {
                entries[entry.name] = new String(zip.readAllBytes(), 'UTF-8')
            }
        }

        then:
        result.is(message)
        message.getHeaders()['Content-Type'] == 'application/zip'
        entries.keySet() == ['META-INF/', 'META-INF/MANIFEST.MF', '.project', 'metainfo.prop', 'value_mapping.xml'] as Set
        entries['value_mapping.xml'] == '<values>Ä &amp; Ö</values>'
        entries['META-INF/'] == ''
        entries['META-INF/MANIFEST.MF'].contains('Bundle-Version: 2026.10.7')
        entries['.project'].contains('<name> Zuordnung</name>')
        entries['metainfo.prop'].contains('#2026-10-07')
    }
}
