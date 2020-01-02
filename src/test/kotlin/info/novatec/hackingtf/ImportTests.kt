package info.novatec.hackingtf

import info.novatec.hackingtf.azure.GenericAzureResource
import org.junit.Test
import java.io.File

class ImportTests {

    @Test
    fun `Terraform Import Test`() {

        val importRgs =
            bashCaptureJson { """az group list | jq '[ .[] | select (.name | test("^cwe-importt?est")) ]'""" } as List<Map<String, Any?>>
        val importResources =
            importRgs.flatMap {
                bashCaptureJson { """ az resource list -g "${it["name"]}" """ } as List<Map<String, Any?>>
            }.plus(importRgs)

        val workingDir = createTempDir()

        setupImportDir(workingDir)
        val tfSchema = getTfSchema(workingDir)
        stateImport(workingDir, importResources)
        val tfPlan = getTfPlan(workingDir)

        val resourceSchemas = tfSchema.jsonPath<Map<String, Any?>>("$.provider_schemas.azurerm.resource_schemas")
        val resourceChanges = tfPlan.jsonPath<List<Map<String, Any?>>>("$.resource_changes")

        file(File(workingDir, "generated.tf")) {
            resourceChanges.joinToString(separator = "\n\n") {
                GenericAzureResource(it, resourceSchemas[it["type"]] as Map<String, Any?>).resourceCode()
            }
        }

        cd(workingDir) { bash { """
            set -e
            terraform fmt
            terraform validate
        """.trimIndent()}}

        println(workingDir.absolutePath)
        workingDir.deleteRecursively()
    }
}
