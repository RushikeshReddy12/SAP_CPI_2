import com.sap.gateway.ip.core.customdev.util.Message;

def Message processData(Message message) {

    def prop = message.getProperties()
    def token = prop.get("GITHUB_ACCESS_TOKEN").toString()
    def gitOwner = prop.get('GITHUB_OWNER').toString()
    def gitRepo = prop.get('GITHUB_REPO_NAME').toString()
    def artifactId = prop.get('ARTIFACTS_ID').toString()
    def authorization = "Bearer $token"

    message.setHeader("Content-Type", "application/json")
    message.setHeader("Accept", "application/vnd.github+json")
    message.setHeader("Authorization", authorization)

    def body = message.getBody(String)
    
    def httpArtifactsUrl = "https://api.github.com/repos/"+gitOwner+"/"+gitRepo+"/contents/IntegrationArtifacts/Zip/"+artifactId+".zip"
    message.setProperty("httpArtifactsUrl", httpArtifactsUrl)
    
    return message;
}