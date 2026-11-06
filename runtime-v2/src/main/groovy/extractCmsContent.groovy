import com.sap.it.script.v2.api.Message

// Prototype: binary CMS SignedData, attached id-data, definite and indefinite ASN.1 lengths.
// Extraction does NOT verify signatures, certificates or trust.
Message processData(Message message) {
    def output = new ByteArrayOutputStream()
    message.getBody(InputStream).withCloseable { input ->
        extractContent(input, output)
    }
    message.setBody(output.toByteArray())
    return message
}

// Streams the payload to the caller's output; neither stream is closed here.
// Output must be discarded if parsing fails, as it can already contain bytes.
void extractContent(InputStream input, OutputStream output) {
    def parser = new CmsPayloadReader(input)
    parser.extract(output)
}

class CmsPayloadReader {
    private static final byte[] SIGNED_DATA = [0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 1, 7, 2] as byte[]
    private static final byte[] DATA = [0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 1, 7, 1] as byte[]
    private final PushbackInputStream input
    private final byte[] buffer = new byte[8192]
    // -1 denotes an indefinite constructed value, closed by an EOC marker.
    private final List<Long> ends = []
    private long position

    CmsPayloadReader(InputStream input) {
        this.input = new PushbackInputStream(input, 2)
    }

    void extract(OutputStream output) {
        begin(0x30)
        oid(SIGNED_DATA)
        begin(0xa0)
        begin(0x30)
        skipElement(0x02) // version
        skipElement(0x31) // digestAlgorithms
        begin(0x30)
        oid(DATA)
        if (!hasNext()) fail('Detached CMS contains no payload')
        begin(0xa0)
        octets(output)
        finish()
        finish()

        // Optional certificates [0], then optional CRLs [1], then signerInfos.
        if (peekTag() == 0xa0) skipElement(0xa0)
        if (peekTag() == 0xa1) skipElement(0xa1)
        skipElement(0x31)
        finish()
        finish()
        finish()
        if (input.read() != -1) fail('Unexpected bytes after CMS container')
    }

    private void octets(OutputStream output) {
        int tag = peekTag()
        if (tag != 0x04 && tag != 0x24) fail('Expected OCTET STRING')
        begin(tag)
        if (tag == 0x04) {
            copyTo(ends.last(), output)
        } else {
            while (hasNext()) octets(output)
        }
        finish()
    }

    private void oid(byte[] expected) {
        begin(0x06)
        if (ends.last() - position != expected.length) fail('Unsupported CMS content type')
        for (byte value : expected) {
            if (readByte() != (value & 0xff)) fail('Unsupported CMS content type')
        }
        finish()
    }

    private void begin(int expected) {
        if (ends.size() >= 48) fail('ASN.1 nesting too deep')
        if (readByte() != expected) fail('Unexpected ASN.1 tag')
        int first = readByte()
        if (first == 0x80) {
            if ((expected & 0x20) == 0) fail('Indefinite length requires a constructed value')
            ends.add(-1L)
            return
        }
        long length = first
        if ((first & 0x80) != 0) {
            int count = first & 0x7f
            if (count > 8) fail('Invalid ASN.1 length')
            length = 0
            for (int i = 0; i < count; i++) {
                int next = readByte()
                if (length > (Long.MAX_VALUE - next) / 256L) fail('ASN.1 length overflow')
                length = length * 256L + next
            }
        }
        if (length > limit() - position) fail('ASN.1 element exceeds its parent')
        ends.add(position + length)
    }

    private boolean hasNext() {
        long end = ends.last()
        if (end >= 0) return position < end
        int first = readByte()
        if (first != 0) {
            unread(first)
            return true
        }
        int second = readByte()
        if (second != 0) fail('Invalid end-of-contents marker')
        unread(second)
        unread(first)
        return false
    }

    private int peekTag() {
        if (!hasNext()) fail('Missing ASN.1 element')
        int tag = readByte()
        unread(tag)
        if ((tag & 0x1f) == 0x1f || tag == 0) fail('Unsupported ASN.1 tag')
        return tag
    }

    private void finish() {
        long end = ends.last()
        if (end < 0) {
            if (readByte() != 0 || readByte() != 0) fail('Missing end-of-contents marker')
        } else if (position != end) {
            fail('Unexpected ASN.1 structure')
        }
        ends.remove(ends.size() - 1)
    }

    private void skipElement(int tag) {
        begin(tag)
        if (ends.last() >= 0) {
            copyTo(ends.last(), null)
        } else {
            while (hasNext()) skipElement(peekTag())
        }
        finish()
    }

    private long limit() {
        // An indefinite child still has to fit within any definite ancestor.
        long result = Long.MAX_VALUE
        for (long end : ends) {
            if (end >= 0) result = Math.min(result, end)
        }
        return result
    }

    private int readByte() {
        if (position >= limit()) fail('ASN.1 element exceeds its parent')
        int value = input.read()
        if (value == -1) throw new EOFException('Truncated CMS input')
        position++
        return value
    }

    private void unread(int value) {
        input.unread(value)
        position--
    }

    private void copyTo(long end, OutputStream output) {
        while (position < end) {
            int count = input.read(buffer, 0, (int) Math.min(buffer.length as long, end - position))
            if (count == -1) throw new EOFException('Truncated CMS input')
            if (count == 0) {
                int value = readByte()
                if (output != null) output.write(value)
            } else {
                position += count
                if (output != null) output.write(buffer, 0, count)
            }
        }
    }

    private static void fail(String message) {
        throw new IllegalArgumentException(message)
    }
}
