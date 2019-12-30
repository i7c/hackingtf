package info.novatec.hackingtf

import org.junit.Test

class ImportTests {

    @Test
    fun `Terraform Import Test`() {

        val importRgs =
            bashCaptureJson { """az group list | jq '[ .[].name | select (test("^cwe-importt?est")) ]'""" } as List<String>
        val importResources =
            importRgs.flatMap {
                bashCaptureJson { """az resource list -g "$it" | jq "[ .[].id ]" """ } as List<String>
            }


        println("ok")
    }
}