package info.novatec.hackingtf

import org.junit.Test

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

        println(workingDir.absolutePath)
        workingDir.deleteRecursively()
    }
}
