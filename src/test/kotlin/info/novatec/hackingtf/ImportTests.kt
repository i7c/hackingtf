package info.novatec.hackingtf

import org.junit.Test
import java.io.File

class ImportTests {

    @Test
    fun `Terraform Import Test`() {

        val importRgs =
            bashCaptureJson { """az group list | jq '[ .[].name | select (test("^cwe-importt?est")) ]'""" } as List<String>
        val importResources =
            importRgs.flatMap {
                bashCaptureJson { """ az resource list -g "$it" """ } as List<Map<String, Any?>>
            }

        val workingDir = createTempDir()

        setupImportDir(workingDir)
        val tfSchema = getTfSchema(workingDir)

        bash { """
            
        """.trimIndent() }
//
//            val tfPlanFile = File(it, "plan")
//            val tfPlan = bashCaptureJson { """
//                terraform plan -no-color -out "${tfPlanFile.absolutePath}" &> /dev/null
//                terraform show -json "${tfPlanFile.absolutePath}"
//            """.trimIndent() } as Map<String, Any?>

        println(workingDir.absolutePath)
        workingDir.deleteRecursively()
    }

    private fun setupImportDir(workingDir: File) {
        cd(workingDir) {
            file(File(it, "main.tf")) {
                """
                    terraform {
                      required_providers {
                        azurerm = "~>1.29"
                      }
                    }
                    
                    provider azurerm {}
                    """.trimIndent()
            }

            bash {
                """
                    terraform init -no-color
                    terraform validate -no-color
                """.trimIndent()
            }
        }
    }

    private fun getTfSchema(workingDir: File): Map<String, Any?> =
        cd(workingDir) {
            bashCaptureJson { "terraform providers schema -json" } as Map<String, Any?>
        }
}