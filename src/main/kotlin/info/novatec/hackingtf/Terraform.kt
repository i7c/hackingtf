package info.novatec.hackingtf

import java.io.File

fun tfInitNew(workingDir: File, provider: String) {
    cd(workingDir) {
        file(File(it, "main.tf")) { "provider $provider {}" }
        bash { "terraform init -no-color && terraform validate -no-color" }
    }
}

fun tfImportResource(tfResource: String, id: String) {
    bash { """terraform import -allow-missing-config "$tfResource" "$id"""" }
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
