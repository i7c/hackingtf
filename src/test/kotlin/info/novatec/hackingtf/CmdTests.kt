package info.novatec.hackingtf

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class CmdTests {

    @Test
    fun `Test that script runs successfully`() {
        bash {
            """
            #!/bin/bash
            exit 0;
        """.trimIndent()
        }
    }

    @Test(expected = RuntimeException::class)
    fun `Test that script fails when expected`() {
        bash {
            """
            #!/bin/bash
            exit 1;
        """.trimIndent()
        }
    }

    @Test
    fun `Test script without shebang`() {
        bash { "exit 0" }
    }

    @Test
    fun `Test running bash script and reading output`() {
        assertThat(
            bashCaptureOutput { "echo -n $(( 37 + 1300 ))" },
            equalTo("1337")
        )
    }

    @Test
    fun `Test json parsing bash output`() {
        val number = System.nanoTime()

        val result = bashCaptureJson {
            """
                echo -n '{"key": $number}'
            """.trimIndent()
        } as Map<String, Any?>

        assertThat(result["key"], equalTo(number as Any))
    }
}