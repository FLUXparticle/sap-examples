import spock.lang.Specification

class PadSpaceSpec extends Specification {
    private final def script = new padSpace()

    def "padLeftString pads on the left with the given character"() {
        expect:
        script.padLeftString('ab', 5, '-') == '---ab'
    }

    def "padRightString pads on the right with the given character"() {
        expect:
        script.padRightString('ab', 5, '-') == 'ab---'
    }

    def "padLeftStringSpace pads on the left with spaces"() {
        expect:
        script.padLeftStringSpace('ab', 4) == '  ab'
    }

    def "padRightStringSpace pads on the right with spaces"() {
        expect:
        script.padRightStringSpace('ab', 4) == 'ab  '
    }

    def "padLeftZero pads on the left with zeros"() {
        expect:
        script.padLeftZero('12', 4) == '0012'
    }

    def "padRightZero pads on the right with zeros"() {
        expect:
        script.padRightZero('12', 4) == '1200'
    }

    def "padLeftZero preserves strings longer than the requested length"() {
        expect:
        script.padLeftZero('12345', 3) == '12345'
    }
}
