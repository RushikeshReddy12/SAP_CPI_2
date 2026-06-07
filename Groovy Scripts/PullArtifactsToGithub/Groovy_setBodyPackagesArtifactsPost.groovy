import com.sap.gateway.ip.core.customdev.util.Message
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry

def Message processData(Message message) {
    byte[] packageZip = message.getProperty("packageZipBytes") as byte[]
    String packageId  = message.getProperty("packageId")

    def endpointMap = [
        "IFlow"            : "/api/v1/IntegrationDesigntimeArtifacts",
        "ValueMapping"     : "/api/v1/ValueMappingDesigntimeArtifacts",
        "MessageMapping"   : "/api/v1/MessageMappingDesigntimeArtifacts",
        "ScriptCollection" : "/api/v1/ScriptCollectionDesigntimeArtifacts"
    ]

    StringBuilder xml = new StringBuilder()
    xml.append("<?xml version='1.0' encoding='UTF-8'?>\n<artifacts>\n")

    ZipInputStream outerZis = new ZipInputStream(new ByteArrayInputStream(packageZip))
    ZipEntry outerEntry

    while ((outerEntry = outerZis.getNextEntry()) != null) {
        String outerName = outerEntry.getName()

        if (outerName.endsWith("_content")) {
            // Read artifact zip bytes
            ByteArrayOutputStream artifactBaos = new ByteArrayOutputStream()
            byte[] buf = new byte[4096]; int l
            while ((l = outerZis.read(buf)) > 0) artifactBaos.write(buf, 0, l)
            byte[] artifactZipBytes = artifactBaos.toByteArray()

            String artifactId   = ""
            String artifactName = ""
            String artifactType = ""

            ZipInputStream innerZis = new ZipInputStream(new ByteArrayInputStream(artifactZipBytes))
            ZipEntry innerEntry
            while ((innerEntry = innerZis.getNextEntry()) != null) {
                if (innerEntry.getName() == "META-INF/MANIFEST.MF") {
                    ByteArrayOutputStream mfBaos = new ByteArrayOutputStream()
                    byte[] mfBuf = new byte[4096]; int mfL
                    while ((mfL = innerZis.read(mfBuf)) > 0) mfBaos.write(mfBuf, 0, mfL)
                    String manifest = mfBaos.toString("UTF-8")

                    manifest.eachLine { line ->
                        if (line.startsWith("Bundle-SymbolicName:"))
                            artifactId = line.replace("Bundle-SymbolicName:", "").trim()
                        if (line.startsWith("Bundle-Name:"))
                            artifactName = line.replace("Bundle-Name:", "").trim()
                        if (line.startsWith("SAP-BundleType:"))
                            artifactType = line.replace("SAP-BundleType:", "").trim()
                    }
                    if (artifactId.contains(";")) {
                        artifactId = artifactId.split(";")[0].trim()
                    }
                    artifactId = artifactId
                        .trim()
                        .replaceAll("[^a-zA-Z0-9_\\.\\-]", "_")
                        .replaceAll("\\.+\$", "")
                        .replaceAll("^[^a-zA-Z_]", "_")
                    
                    artifactName = artifactName.trim()
                    artifactType = artifactType.trim()
                }
                innerZis.closeEntry()
            }
            innerZis.close()
            
            def supportedTypes = ["IntegrationFlow", "MessageMapping", "ValueMapping", "ScriptCollection"]
            if (artifactId && supportedTypes.contains(artifactType)) {
                // Resolve endpoint — fallback to IntegrationDesigntimeArtifacts if unknown type
                String endpoint = endpointMap.get(artifactType, "/api/v1/IntegrationDesigntimeArtifacts")
                String base64Content = Base64.getEncoder().encodeToString(artifactZipBytes)

                xml.append("  <artifact>\n")
                xml.append("    <id>${artifactId}</id>\n")
                xml.append("    <name>${artifactName ?: artifactId}</name>\n")
                xml.append("    <type>${artifactType}</type>\n")
                xml.append("    <packageId>${packageId}</packageId>\n")
                xml.append("    <endpoint>${endpoint}</endpoint>\n")
                xml.append("    <base64Content>${base64Content}</base64Content>\n")
                xml.append("  </artifact>\n")
                
                message.setProperty("tempArtifactType", artifactType)
            }else if (artifactId) {
                message.setProperty("skipped_${artifactId}", artifactType)
            }
        }
        outerZis.closeEntry()
    }
    outerZis.close()

    xml.append("</artifacts>")

    message.setBody(xml.toString())
    message.setHeader("Content-Type", "application/xml")

    return message
}