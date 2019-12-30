package info.novatec.hackingtf

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

private fun runBashScript(script: String, dir: File = File(System.getProperty("user.dir"))) =
    ProcessBuilder("bash", "-")
        .directory(dir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .apply {
            OutputStreamWriter(outputStream, Charsets.UTF_8).use {
                it.write(script)
            }
            waitFor()
        }

fun runBashScriptCaptureOutput(script: String, dir: File = File(System.getProperty("user.dir"))): String {
    try {
        ProcessBuilder("bash", "-")
            .directory(dir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
            .apply {
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { it.write(script) }
                val stdout = inputStream.bufferedReader().use { it.readText() }
                val stderr = errorStream.bufferedReader().use { it.readText() }
                waitFor()
                return if (exitValue() == 0) stdout
                else throw RuntimeException(stderr)
            }
    } catch (e: IOException) {
        throw RuntimeException(e)
    }
}

fun bash(script: () -> String) = runBashScript(script())
    .exitValue()
    .let { if (it != 0) throw RuntimeException("Failed with exit code $it") }

fun bashCaptureOutput(script: () -> String): String = runBashScriptCaptureOutput(script())

fun bashCaptureJson(script: () -> String): Any = runBashScriptCaptureOutput(script())
    .let { jacksonObjectMapper().readValue(it) }

inline fun <reified T> cd(dir: File, contextualFunction: (dir: File) -> T): T {
    val backup = System.getProperty("user.dir")
    return try {
        System.setProperty("user.dir", dir.absolutePath)
        contextualFunction(dir)
    } finally {
        System.setProperty("user.dir", backup)
    }
}

fun file(file: File, content: () -> String): File {
    OutputStreamWriter(FileOutputStream(file)).use {
        it.write(content())
    }
    return file
}
