import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

def Message postPayloadIntegrationFlow(Message message) {

    def body = message.getBody(String)
    def base64Content = body.replaceAll("\\r|\\n", "")

    def prop = message.getProperties()
    def id = prop.get("ARTIFACT_ID")
    def packageId = prop.get("ARTIFACT_PACKAGE_ID")

    def jsonOutput = JsonOutput.toJson([
             Name: id,
             Id: id,
             PackageId: packageId,
             ArtifactContent: base64Content
    ])
    
    def artifactType = prop.get("ARTIFACT_TYPE")
    def host = prop.get("TENANT_HOST_ADDRESS")?.toString().trim()
    def httpUrl = ""
    if(artifactType == "IFL"){
        httpUrl = host + "/api/v1/IntegrationDesigntimeArtifacts"
    }else if(artifactType == "MM"){
        httpUrl = host + "/api/v1/MessageMappingDesigntimeArtifacts"
    }else if(artifactType == "VM"){
        httpUrl = host + "/api/v1/ValueMappingDesigntimeArtifacts"
    }else if(artifactType == "SC"){
        httpUrl = host + "/api/v1/ScriptCollectionDesigntimeArtifacts"
    }
    message.setProperty("httpUrl", httpUrl)
    
    println JsonOutput.prettyPrint(jsonOutput)
    message.setBody(JsonOutput.prettyPrint(jsonOutput))
    message.setHeader("Content-Type", "application/json")
    return message
}