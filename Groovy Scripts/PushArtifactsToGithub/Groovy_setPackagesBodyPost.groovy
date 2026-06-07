import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def Message processData(Message message) {

    def body = message.getBody(String)
    def props = message.getProperties()
    def packageIds = props.get("PACKAGE_IDS")

    if (packageIds != null && packageIds.toString().trim().length() != 0) {

        def requiredIds = packageIds.toString()
            .split(',')
            .collect { it.trim() }
            .findAll { it }
        def json = new JsonSlurper().parseText(body)

        def filteredResults = json.d.results.findAll { pkg ->
            requiredIds.contains(pkg.Id)
        }

        def output = [
            d: [
                results: filteredResults
            ]
        ]
        message.setProperty("AllPackages", JsonOutput.prettyPrint(JsonOutput.toJson(output)))
    }

    return message
}