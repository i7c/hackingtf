package info.novatec.hackingtf

import info.novatec.hackingtf.azure.azureIdToNamedResource
import info.novatec.hackingtf.terraform.GenericConfigurationBlock
import java.io.File
import org.junit.Test

class ImportTests {

    @Test
    fun `Terraform Import Test`() {

        val importRgs =
            bashCaptureJson {
                """
                az group list \
                    | jq '[ .[] | select (.name | test("^cwe-importt?est")) ]'
            """
            } as List<Map<String, Any?>>
        val importResources = importRgs.flatMap {
            bashCaptureJson { """ az resource list -g "${it["name"]}" """ } as List<Map<String, Any?>>
        }.plus(importRgs)

        val workingDir = createTempDir()

        tfInitNew(workingDir, "azurerm")
        val resourceSchemas = getTfSchema(workingDir).jsonPath<Map<String, Any?>>("$.provider_schemas.azurerm.resource_schemas")

        cd(workingDir) {
            importResources.forEach {
                tfImportResource(azureIdToNamedResource(it["id"] as String, it["name"] as String), it["id"] as String)
            }
        }

        val resourceChanges = getTfPlan(workingDir).jsonPath<List<Map<String, Any?>>>("$.resource_changes")

        file(File(workingDir, "generated.tf")) {
            resourceChanges.joinToString(separator = "\n\n") {
                GenericConfigurationBlock(
                    it["name"] as String,
                    it.jsonPath("$.change.before"),
                    resourceSchemas[it["type"]]!!.jsonPath<Map<String, Any?>>("$.block"),
                    it["type"] as String
                ).resourceCode()
            }
        }

        cd(workingDir) { bash { """
            set -e
            terraform fmt
            terraform validate
        """.trimIndent() } }

        println(workingDir.absolutePath)
        workingDir.deleteRecursively()
    }
}
