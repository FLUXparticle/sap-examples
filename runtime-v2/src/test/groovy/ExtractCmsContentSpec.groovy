import org.bouncycastle.asn1.BEROctetString
import org.bouncycastle.asn1.cms.SignedData
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.CMSSignedDataStreamGenerator
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder

import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator

import com.sap.it.script.v2.api.Message
import spock.lang.Specification

class ExtractCmsContentSpec extends Specification {
    private final def script = new extractCmsContent()

    def "extracts original XML bytes from an OpenSSL attached signature"() {
        given:
        def message = new Message()
        message.setBody(getClass().getResource('/cms/attached.der').bytes)

        when:
        def result = script.processData(message)

        then:
        result.is(message)
        message.getBody(byte[]) == getClass().getResource('/cms/payload.xml').bytes
    }

    def "joins definite-length OCTET STRING chunks without interpreting XML"() {
        given:
        byte[] payload = (0..255).collect { it as byte } as byte[]
        def content = tlv(0x24, concat(tlv(4, payload[0..99] as byte[]), tlv(4, payload[100..255] as byte[])))
        def output = new ByteArrayOutputStream()

        when:
        script.extractContent(new ByteArrayInputStream(cms(content)), output)

        then:
        output.toByteArray() == payload
    }

    def "rejects detached content"() {
        when:
        script.extractContent(new ByteArrayInputStream(cms(null)), new ByteArrayOutputStream())

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('Detached')
    }

    def "extracts a freshly signed definite-length CMS message"() {
        given:
        byte[] payload = '<Meldung>Grüße &amp; Daten</Meldung>'.getBytes(StandardCharsets.UTF_8)
        byte[] signed = sign(payload, false)
        def message = new Message()
        message.setBody(signed)

        when:
        script.processData(message)

        then:
        message.getBody(byte[]) == payload
    }

    def "extracts signed binary data exceeding the 8 KiB copy buffer"() {
        given:
        byte[] payload = largePayload()
        byte[] signed = sign(payload, false)
        def output = new ByteArrayOutputStream()

        when:
        script.extractContent(new ByteArrayInputStream(signed), output)

        then:
        output.toByteArray() == payload
    }

    def "extracts a streaming signature across actual CMS content blocks"() {
        given:
        byte[] payload = largePayload()
        byte[] signed = sign(payload, true)
        def content = SignedData.getInstance(new CMSSignedData(signed).toASN1Structure().content)
            .encapContentInfo.content
        assert content instanceof BEROctetString
        // Constructed BER plus more than a single chunk's header overhead.
        assert content.getEncoded('BER').length > payload.length + 32
        def output = new ByteArrayOutputStream()

        when:
        script.extractContent(new ByteArrayInputStream(signed), output)

        then:
        output.toByteArray() == payload
    }

    def "rejects indefinite primitive OCTET STRING"() {
        when:
        script.extractContent(new ByteArrayInputStream(cms([4, 0x80, 0, 0] as byte[])), new ByteArrayOutputStream())

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('Indefinite length requires a constructed value')
    }

    def "rejects missing BER end marker"() {
        given:
        byte[] signed = sign(largePayload(), true)

        when:
        script.extractContent(new ByteArrayInputStream(Arrays.copyOf(signed, signed.length - 2)), new ByteArrayOutputStream())

        then:
        thrown(EOFException)
    }

    def "rejects truncated payload"() {
        given:
        byte[] encoded = cms(tlv(4, 'example'.bytes))

        when:
        script.extractContent(new ByteArrayInputStream(Arrays.copyOf(encoded, encoded.length - 5)), new ByteArrayOutputStream())

        then:
        thrown(EOFException)
    }

    def "rejects content extending beyond its enclosing element"() {
        when:
        script.extractContent(new ByteArrayInputStream(cms([4, 100, 1] as byte[])), new ByteArrayOutputStream())

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('exceeds its parent')
    }

    def "rejects encrypted content type"() {
        given:
        byte[] oid = [0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 1, 7, 3] as byte[]

        when:
        script.extractContent(new ByteArrayInputStream(tlv(0x30, tlv(6, oid))), new ByteArrayOutputStream())

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('Unsupported CMS content type')
    }

    private static byte[] largePayload() {
        // Includes zero/EOC-like bytes, arbitrary binary values and a partial final block.
        byte[] payload = new byte[100_003]
        new Random(42).nextBytes(payload)
        payload[1023] = 0
        payload[1024] = 0
        payload[8191] = 0
        payload[8192] = 0
        payload
    }

    private static byte[] sign(byte[] payload, boolean streaming) {
        // Entirely local, ephemeral test identity. No keystore or global provider registration.
        def provider = new BouncyCastleProvider()
        def keys = KeyPairGenerator.getInstance('RSA')
        keys.initialize(2048)
        def pair = keys.generateKeyPair()
        def name = new X500Name('CN=Ephemeral CMS Test')
        def now = new Date()
        def contentSigner = new JcaContentSignerBuilder('SHA256withRSA').setProvider(provider)
        def certificate = new JcaX509v3CertificateBuilder(name, BigInteger.ONE,
            new Date(now.time - 60_000), new Date(now.time + 3_600_000), name, pair.public)
            .build(contentSigner.build(pair.private))
        def signer = new JcaSignerInfoGeneratorBuilder(
            new JcaDigestCalculatorProviderBuilder().setProvider(provider).build())
            .build(contentSigner.build(pair.private), certificate)
        def generator = streaming ? new CMSSignedDataStreamGenerator() : new CMSSignedDataGenerator()
        generator.addSignerInfoGenerator(signer)
        generator.addCertificate(certificate)
        byte[] encoded
        if (streaming) {
            generator.setBufferSize(1024)
            def output = new ByteArrayOutputStream()
            generator.open(output, true).withCloseable { it.write(payload) }
            encoded = output.toByteArray()
        } else {
            generator.setDefiniteLengthEncoding(true)
            encoded = generator.generate(new CMSProcessableByteArray(payload), true).encoded
        }
        // Ensure fixtures really carry a valid signature, independently of our extractor.
        def parsed = new CMSSignedData(encoded)
        assert parsed.signerInfos.signers.size() == 1
        assert parsed.signerInfos.signers.first().verify(
            new JcaSimpleSignerInfoVerifierBuilder().setProvider(provider).build(certificate))
        return encoded
    }

    // Structural fixtures only: empty signerInfos, no cryptographic signature.
    private static byte[] cms(byte[] content) {
        byte[] baseOid = [0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 1, 7] as byte[]
        byte[] contentInfo = tlv(0x30, concat(tlv(6, concat(baseOid, [1] as byte[])),
            content == null ? new byte[0] : tlv(0xa0, content)))
        byte[] signedData = tlv(0x30, concat(tlv(2, [1] as byte[]), tlv(0x31, new byte[0]),
            contentInfo, tlv(0x31, new byte[0])))
        tlv(0x30, concat(tlv(6, concat(baseOid, [2] as byte[])), tlv(0xa0, signedData)))
    }

    private static byte[] tlv(int tag, byte[] value) {
        def output = new ByteArrayOutputStream()
        output.write(tag)
        if (value.length < 128) {
            output.write(value.length)
        } else {
            output.write(0x82)
            output.write(value.length >> 8)
            output.write(value.length & 0xff)
        }
        output.write(value)
        output.toByteArray()
    }

    private static byte[] concat(byte[]... parts) {
        def output = new ByteArrayOutputStream()
        parts.each { output.write(it) }
        output.toByteArray()
    }
}
