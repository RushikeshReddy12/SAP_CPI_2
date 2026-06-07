import com.sap.gateway.ip.core.customdev.util.Message
import javax.xml.parsers.DocumentBuilderFactory
import groovy.json.JsonOutput

def Message processData(Message message) {
    String xmlBody = message.getBody(String)

    def factory = DocumentBuilderFactory.newInstance()
    def builder = factory.newDocumentBuilder()
    def doc = builder.parse(new ByteArrayInputStream(xmlBody.getBytes("UTF-8")))

    String artifactId    = doc.getElementsByTagName("id").item(0)?.getTextContent()
    String artifactName  = doc.getElementsByTagName("name").item(0)?.getTextContent()
    String artifactType  = doc.getElementsByTagName("type").item(0)?.getTextContent()
    String packageId     = doc.getElementsByTagName("packageId").item(0)?.getTextContent()
    String base64Content = doc.getElementsByTagName("base64Content").item(0)?.getTextContent()
    String endpoint      = doc.getElementsByTagName("endpoint").item(0)?.getTextContent()
    
    message.setProperty("post_artifactId",   artifactId)
    message.setProperty("post_artifactName", artifactName)
    message.setProperty("post_artifactType", artifactType)
    message.setProperty("post_endpoint",     endpoint)
    message.setProperty("post_packageId",    packageId)

    def payload = [
        Name           : artifactName,
        Id             : artifactId,
        PackageId      : packageId,
        ArtifactContent: base64Content
    ]

    message.setBody(JsonOutput.toJson(payload))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("Accept", "application/json")

    message.setProperty("artifactEndpoint", endpoint)

    return message
}