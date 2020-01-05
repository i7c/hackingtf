package info.novatec.hackingtf

import info.novatec.hackingtf.azure.azureIdToNamedResource
import java.io.File

fun setupImportDir(workingDir: File) {
    cd(workingDir) {
        file(File(it, "main.tf")) { """
                    terraform {
                      required_providers {
                        azurerm = "~>1.29"
                      }
                    }

                    provider azurerm {}
                    """.trimIndent() }

        bash { """
              terraform init -no-color
              terraform validate -no-color
              """.trimIndent() }
    }
}

fun stateImport(
    workingDir: File,
    importResources: List<Map<String, Any?>>
) {
    cd(workingDir) {
        importResources.forEach {
            bash { """ terraform import -allow-missing-config "${azureIdToNamedResource(it["id"] as String, it["name"] as String)}" "${it["id"]}" """.trimIndent() }
        }
    }
}

fun getTfPlan(workingDir: File): Map<String, Any?> {
    return cd(workingDir) {
        val tfPlanFile = File(it, "plan")
        bashCaptureJson { """
                          terraform plan -no-color -out "${tfPlanFile.absolutePath}" &> /dev/null
                          terraform show -json "${tfPlanFile.absolutePath}"
                          """.trimIndent() } as Map<String, Any?>
    }
}

fun getTfSchema(workingDir: File): Map<String, Any?> =
    cd(workingDir) { bashCaptureJson { "terraform providers schema -json" } as Map<String, Any?> }
