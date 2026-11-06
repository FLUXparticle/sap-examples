// This is Groovy Flowstep Version 2.x, running with Groovy runtime 4, Downgrade the script if older behaviour needed.

import com.sap.it.script.v2.api.Message

def Message processData(Message message) {
    def headers = message.getHeaders()

    def required = ['NumGen_MessageId', 'NumGen_ProcessSign', 'NumGen_Receiver', 'NumGen_MaxNumber']
    for (name in required) {
        if ( ! headers.containsKey(name)) {
            throw new IllegalArgumentException("Header "+name+" fehlt, ist Pflicht")
        }
    }
    def name

    name = 'NumGen_Receiver'
    if (headers.get(name).trim().isEmpty()) {
        throw new IllegalArgumentException("Header "+name+" muss angegeben werden, darf nicht leer sein")
    }
    if (headers.get(name).trim().toLowerCase().equals('null')) {
        // 'null' als Option für null, damit kompatibel zur Verwendung ohne Receiver in PI
        message.setHeader(name, null)
    }

    name = 'NumGen_MaxNumber'
    if ( ! message.getHeaders().get(name)?.toString()?.isNumber()) {
        throw new IllegalArgumentException("Header "+name+" muss eine Zahl sein")
    }

    name = 'NumGen_ExpectNumber'
    if ( ! headers.containsKey(name)) {
        message.setHeader(name, message.getProperty(name+'_Default'))
    }
    if ( ! message.getHeaders().get(name)?.toString()?.isNumber()) {
        throw new IllegalArgumentException("Header "+name+" muss eine Zahl sein")
    }

    name = 'NumGen_RetentionNumbers'
    if ( ! headers.containsKey(name)) {
        message.setHeader(name, message.getProperty(name+'_Default'))
    }
    if ( ! message.getHeaders().get(name)?.toString()?.isNumber()) {
        throw new IllegalArgumentException("Header "+name+" muss eine Zahl sein")
    }

    name = 'NumGen_RetentionMessageId'
    if ( ! headers.containsKey(name)) {
        message.setHeader(name, message.getProperty(name+'_Default'))
    }
    if ( ! message.getHeaders().get(name)?.toString()?.isNumber()) {
        throw new IllegalArgumentException("Header "+name+" muss eine Zahl sein")
    }

    return message
}
