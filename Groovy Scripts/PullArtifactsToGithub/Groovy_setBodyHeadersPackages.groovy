import com.sap.gateway.ip.core.customdev.util.Message
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def Message processData(Message message) {
    byte[] packageZip = message.getBody(byte[])
    message.setProperty("packageZipBytes", packageZip)

    String packageName = ""
    String packageId   = ""
    String shortText   = ""
    String version     = "1.0.0"

    ZipInputStream zis1 = new ZipInputStream(new ByteArrayInputStream(packageZip))
    ZipEntry entry1
    while ((entry1 = zis1.getNextEntry()) != null) {
        String n = entry1.getName()

        if (n == "ExportInformation.info") {
            ByteArrayOutputStream b = new ByteArrayOutputStream()
            byte[] buf = new byte[4096]; int l
            while ((l = zis1.read(buf)) > 0) b.write(buf, 0, l)
            b.toString("UTF-8").eachLine { line ->
                if (line.startsWith("Name="))
                    packageName = line.replace("Name=", "").trim()
            }
        }

        if (n == "resources.cnt") {
            ByteArrayOutputStream b = new ByteArrayOutputStream()
            byte[] buf = new byte[4096]; int l
            while ((l = zis1.read(buf)) > 0) b.write(buf, 0, l)
            try {
                byte[] decoded = Base64.getDecoder().decode(b.toByteArray())
                def json = new JsonSlurper().parseText(new String(decoded, "UTF-8"))
                def res = json.resources?.find {
                    it.additionalAttributes?.OriginBundleSymbolicName != null
                }
                if (res) {
                    def attrs = res.additionalAttributes
                    packageId = attrs.OriginBundleSymbolicName?.attributeValues?.getAt(0) ?: ""
                    version   = attrs.OriginBundleVersion?.attributeValues?.getAt(0) ?: "1.0.0"
                    shortText = attrs.Description?.attributeValues?.getAt(0) ?: ""
                }
            } catch(Exception e) { /* ignore */ }
        }

        zis1.closeEntry()
    }
    zis1.close()

    ZipInputStream zis2 = new ZipInputStream(new ByteArrayInputStream(packageZip))
    ZipEntry entry2
    while ((entry2 = zis2.getNextEntry()) != null) {
        String n = entry2.getName()
        if (n.endsWith("_content")) {
            ByteArrayOutputStream b = new ByteArrayOutputStream()
            byte[] buf = new byte[4096]; int l
            while ((l = zis2.read(buf)) > 0) b.write(buf, 0, l)
            byte[] contentBytes = b.toByteArray()

            int previewLen = Math.min(40, contentBytes.length)
            StringBuilder hex = new StringBuilder()
            for (int i = 0; i < previewLen; i++) {
                hex.append(String.format("%02X ", contentBytes[i] & 0xFF))
            }
            message.setProperty("debug_entry", n)
            message.setProperty("debug_size",  contentBytes.length.toString())
            message.setProperty("debug_hex",   hex.toString().trim())

            try {
                ZipInputStream tz = new ZipInputStream(new ByteArrayInputStream(contentBytes))
                ZipEntry te = tz.getNextEntry()
                message.setProperty("debug_directZip", te ? "YES: ${te.getName()}" : "no entries")
                tz.close()
            } catch(Exception e) {
                message.setProperty("debug_directZip", "failed: ${e.getMessage()}")
            }

            try {
                byte[] dec = Base64.getDecoder().decode(contentBytes)
                ZipInputStream tz = new ZipInputStream(new ByteArrayInputStream(dec))
                ZipEntry te = tz.getNextEntry()
                message.setProperty("debug_b64Zip", te ? "YES: ${te.getName()}" : "no entries")
                tz.close()
            } catch(Exception e) {
                message.setProperty("debug_b64Zip", "failed: ${e.getMessage()}")
            }

            break
        }
        zis2.closeEntry()
    }
    zis2.close()

    if (!packageId && packageName) {
        packageId = packageName.replaceAll("[^a-zA-Z0-9_]", "")
    }

    def payload = [
        Id       : packageId,
        Name     : packageName,
        ShortText: shortText,
        Version  : version
    ]

    message.setBody(JsonOutput.toJson(payload))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("Accept", "application/json")
    message.setProperty("packageId", packageId)

    return message
}