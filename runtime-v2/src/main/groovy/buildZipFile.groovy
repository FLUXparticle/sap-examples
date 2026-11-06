// This Groovy Flowstep Version 2, running with Groovy 4, Downgrade the script if older behaviour needed.

import com.sap.it.script.v2.api.Message
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.ByteArrayOutputStream

def Message processData(Message message) {
    // Inhalte für die ZIP-Datei definieren
    def date = message.getProperty("Date")
    def timestamp_meta = message.getProperty("Timestamp_Meta")
    def vm_name = message.getProperty("VM_Name")
    
    def structure = [
        "META-INF/": "",
        "META-INF/MANIFEST.MF": """Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: ${vm_name}
Bundle-SymbolicName: ${vm_name}
Bundle-Version: ${date}
SAP-BundleType: ValueMapping
SAP-NodeType: IFLMAP
Import-Package: com.sap.xi.mapping.camel.valmap
Bundle-Activator: com.sap.xi.mapping.camel.valmap.ValueMappingActivator""",

        ".project": """<?xml version="1.0" encoding="UTF-8"?><projectDescription>
   <name> ${vm_name}</name>
   <comment/>
   <projects/>
   <buildSpec>
      <buildCommand>
         <name>org.eclipse.jdt.core.javabuilder</name>
         <arguments/>
      </buildCommand>
   </buildSpec>
   <natures>
      <nature>org.eclipse.jdt.core.javanature</nature>
      <nature>com.sap.ide.ifl.project.support.project.nature</nature>
      <nature>com.sap.ide.ifl.valmap</nature>
   </natures>
</projectDescription>
""",

        "metainfo.prop": """#Store metainfo properties
#${timestamp_meta}
description=
source=
target=
""",

        "value_mapping.xml": message.getBody(java.lang.String) // Inhalt aus der Nachricht
    ]

    // ByteArrayOutputStream für die ZIP-Datei erstellen
    def byteArrayOutputStream = new ByteArrayOutputStream()
    def zipOutputStream = new ZipOutputStream(byteArrayOutputStream)

    // Struktur in die ZIP-Datei schreiben
    structure.each { path, content ->
        def entry = new ZipEntry(path)
        zipOutputStream.putNextEntry(entry)

        // Falls der Eintrag eine Datei ist, den Inhalt schreiben
        if (content) {
            zipOutputStream.write(content.getBytes('UTF-8'))
        }

        zipOutputStream.closeEntry()
    }

    zipOutputStream.close()

    // ZIP-Datei in die Nachricht setzen
    message.setBody(byteArrayOutputStream.toByteArray())
    message.setHeader("Content-Type", "application/zip")
    return message
}
