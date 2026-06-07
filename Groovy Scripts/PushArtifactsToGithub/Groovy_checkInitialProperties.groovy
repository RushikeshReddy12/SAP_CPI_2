import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.ITApiFactory
import com.sap.it.api.securestore.SecureStoreService
import com.sap.it.api.securestore.AccessTokenAndUser

def Message processData(Message message) {

    def prop = message.getProperties()

    def githubAccessTokenAlias = prop.get("GITHUB_ACCESS_TOKEN")
    if(githubAccessTokenAlias == null || githubAccessTokenAlias.trim().length() == 0 || githubAccessTokenAlias.trim() == '""')
        throw new Exception("\'Error: Name of the credential created in Security Material to store Github Personal Access Token is missing.\'")
    def githubAccessToken = getGithubToken(githubAccessTokenAlias)
    message.setProperty("GITHUB_ACCESS_TOKEN", githubAccessToken)

    def oauthCredentialAlias = prop.get("OAUTH_CREDENTIAL")
    if (oauthCredentialAlias == null || oauthCredentialAlias.trim().length() == 0 || oauthCredentialAlias.trim() == '""') {
        throw new Exception("Error: Name of the credential created in Security Material for SAP CI ODATA V2 API authentication is missing.")
    }
    
    def host = prop.get("TENANT_HOST_ADDRESS")
    if(host == null || host.trim().length() == 0 || host.trim() == '""'){
        throw new Exception("\'Error: Host URL of Process Integration Runtime Service | API Plan is missing.\'")
    }
    
    def artifactId = prop.get("ARTIFACTS_ID")?.toString()?.trim()
    if (!artifactId || artifactId == '""') {
        throw new Exception("Error: Artifact ID is missing.")
    }
    def artifactType = prop.get("ARTIFACT_TYPE")?.toString()?.trim()
    def artifactsUrl = ""
    if (artifactType == "IFL") {
        artifactsUrl = host + "/api/v1/IntegrationDesigntimeArtifacts(Id='" + artifactId + "',Version='active')/" + '$value'
    } else if (artifactType == "VM") {
        artifactsUrl = host + "/api/v1/ValueMappingDesigntimeArtifacts(Id='" + artifactId + "',Version='active')/" + '$value'
    } else if (artifactType == "MM") {
        artifactsUrl = host + "/api/v1/MessageMappingDesigntimeArtifacts(Id='" + artifactId + "',Version='active')/" + '$value'
    } else if (artifactType == "SC") {
        artifactsUrl = host + "/api/v1/ScriptCollectionDesigntimeArtifacts(Id='" + artifactId + "',Version='active')/" + '$value'
    } else if (artifactType == "DT") {
        artifactsUrl = host + "/api/v1/DataTypeDesigntimeArtifacts(Id='" + artifactId + "',Version='active')/" + '$value'
    } else if (artifactType == "MT") {
        artifactsUrl = host + "/api/v1/MessageTypeDesigntimeArtifacts(Id='" + artifactId + "',Version='active')/" + '$value'
    } else if (artifactType == "SI") {
        artifactsUrl = host + "/api/v1/ServiceInterfaceDesigntimeArtifacts(Id='" + artifactId + "',Version='active')/" + '$value'
    } else {
        throw new Exception("Error: Artifact Type is not supported.")
    }

    message.setProperty("httpArtifactsUrl", artifactsUrl)
    
    def packageId = prop.get("PACKAGE_ID")?.toString()?.trim()
    def packageUrl = host + ":443/api/v1/IntegrationPackages('" + packageId + "')/" + '$value'
    message.setProperty("httpPackagesUrl", packageUrl)

    def githubOwner = prop.get("GITHUB_OWNER")
    if(githubOwner == null || githubOwner.trim().length() == 0 || githubOwner.trim() == '""')
        throw new Exception("\'Error: The account owner of Github Repository is missing.\'")

    def githubRepoName = prop.get("GITHUB_REPO_NAME")
    if(githubRepoName == null || githubRepoName.trim().length() == 0 || githubRepoName.trim() == '""')
        throw new Exception("\'Error: Name of the Github Repository without the .git extension is missing.\'")

    def githubCommitMsg = prop.get("GITHUB_COMMIT_MESSAGE")
    if(githubCommitMsg == null || githubCommitMsg.trim().length() == 0 || githubCommitMsg.trim() == '""')
        throw new Exception("\'Error: Github Commit message is missing\'")


    return message
}

String getGithubToken(String keyAlias)
{
    def secureStorageService =  ITApiFactory.getService(SecureStoreService.class, null)
    try
    {
        def secureParameter = secureStorageService.getUserCredential(keyAlias)
        return secureParameter.getPassword().toString()
    }
    catch(Exception e){
        throw new Exception("\'Error: Secure Parameter for Github Personal Access Token is not available\'")
    }
}