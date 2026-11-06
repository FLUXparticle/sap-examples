import spock.lang.Specification
import java.time.LocalDate
import spock.lang.Unroll
class GetMonthGermanSpec extends Specification {
    private final def script = new getMonthGerman()

    @Unroll
    def "formats month #month as #expected"() {
        expect:
        script.getMonat(LocalDate.of(2026, month, 1)) == expected

        where:
        month | expected
        1 | 'Januar'
        3 | 'März'
        10 | 'Oktober'
        12 | 'Dezember'
    }

    def "default date returns current German month"() {
        expect:
        script.getMonat() == script.getMonat(LocalDate.now())
    }
}
