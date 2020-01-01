package info.novatec.hackingtf

import java.io.File
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

    private fun getTfPlan(workingDir: File): Map<String, Any?> {
        return cd(workingDir) {
            val tfPlanFile = File(it, "plan")
            bashCaptureJson {
                """
                    terraform plan -no-color -out "${tfPlanFile.absolutePath}" &> /dev/null
                    terraform show -json "${tfPlanFile.absolutePath}"
                """.trimIndent()
            } as Map<String, Any?>
        }
    }

    private fun stateImport(
        workingDir: File,
        importResources: List<Map<String, Any?>>
    ) {
        cd(workingDir) {
            importResources.forEach {
                bash {
                    """
                        terraform import -allow-missing-config "${mapToTfType(
                        it["id"] as String,
                        it["name"] as String
                    )}" "${it["id"]}"
                    """.trimIndent()
                }
            }
        }
    }

    private fun mapToTfType(id: String, name: String): String =
        when {
            Regex("^/subscriptions/.*/resourceGroups/.*/providers/Microsoft.DBforMySQL/servers/.*$").matches(id) -> "azurerm_mysql_server.$name"
            Regex("^/subscriptions/.*/resourceGroups/.*/providers/Microsoft.ContainerService/managedClusters/.*$").matches(id) -> "azurerm_kubernetes_cluster.$name"
            Regex("/subscriptions/.*/resourceGroups/.*/providers/Microsoft.Network/virtualNetworks/.*$").matches(id) -> "azurerm_virtual_network.$name"
            Regex("/subscriptions/.*/resourceGroups/.*$").matches(id) -> "azurerm_resource_group.$name"
            else -> throw IllegalArgumentException("Unknown resource type $id")
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
