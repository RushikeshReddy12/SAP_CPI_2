import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

def Message putPayloadIntegrationFlow(Message message) {

    def body = message.getBody(String)
    def base64Content = body.replaceAll("\\r|\\n", "")

    def prop = message.getProperties()
    def name = prop.get("ARTIFACT_ID")
    def pid = prop.get("ARTIFACT_PACKAGE_ID")

    def jsonOutput = JsonOutput.toJson([
        Id             : name,
        Name           : name,
        PackageId      : pid,
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
    
    message.setBody(JsonOutput.prettyPrint(jsonOutput))
    message.setHeader("Content-Type", "application/json")
    return message
}