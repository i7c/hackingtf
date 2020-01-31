package info.novatec.hackingtf

import info.novatec.hackingtf.azure.azureIdToNamedResource
import info.novatec.hackingtf.azure.resourceChangesJsonPath
import info.novatec.hackingtf.azure.schemaJsonPath
import info.novatec.hackingtf.terraform.GenericConfigurationBlock
import org.junit.Test
import java.io.File

class ImportTests {

    val workingDir = File("/tmp/stateimport").apply { mkdirs() }

    @Test
    fun `Import resources into statefile`() {
        val importRgs =
            bashCaptureJson {
                """az group list | jq '[ .[] | select (.name | test("^cwe-importt?est")) ]'"""
            } as List<Map<String, Any?>>

        val importResources = importRgs.flatMap {
            bashCaptureJson { """az resource list -g "${it["name"]}"""" } as List<Map<String, Any?>>
        }.plus(importRgs)

        tfInitNew(workingDir, "azurerm")
        cd(workingDir) {
            importResources.forEach {
                tfImportResource(azureIdToNamedResource(it["id"] as String, it["name"] as String), it["id"] as String)
            }
        }
    }

    @Test
    fun `Generate code`() {
        val resourceSchemas = getTfSchema(workingDir).jsonPath<Map<String, Any?>>(schemaJsonPath)
        val resourceChanges = getTfPlan(workingDir).jsonPath<List<Map<String, Any?>>>(resourceChangesJsonPath)

        file(File(workingDir, "generated.tf")) {
            resourceChanges.joinToString(separator = "") {
                GenericConfigurationBlock(
                    it["name"] as String,
                    it.jsonPath("$.change.before"),
                    resourceSchemas[it["type"]]!!.jsonPath<Map<String, Any?>>("$.block"),
                    it["type"] as String
                ).resourceCode()
            }
        }

        cd(workingDir) { bash { "terraform fmt" } }
    }
}
