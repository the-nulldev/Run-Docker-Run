import org.hyperskill.hstest.dynamic.DynamicTest
import org.hyperskill.hstest.stage.StageTest
import org.hyperskill.hstest.testcase.CheckResult
import org.hyperskill.hstest.testing.TestedProgram
import java.io.BufferedReader
import java.io.InputStreamReader

class DockerTest : StageTest<String>() {

    private val requiredImageTag = "hyper-web-app:latest"

    /**
     * Test 1: Check if the custom image `hyper-web-app:latest` exists in the system
     */
    @DynamicTest
    fun test1_checkCustomImageExists(): CheckResult {
        // Run the student's program to determine the Dockerfile path
        val program = TestedProgram()
        val output = program.start().trim()

        // Check if the required image exists in the Docker system
        return if (isImagePresent(requiredImageTag)) {
            CheckResult.correct()
        } else {
            CheckResult.wrong("The custom Docker image '$requiredImageTag' was not found in the system. " +
                    "Make sure to build the image using the correct tag!")
        }
    }

    /**
     * Checks if the given Docker image exists in the local Docker system.
     */
    private fun isImagePresent(imageTag: String): Boolean {
        // Detect the OS to use the appropriate shell command
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val shell = if (isWindows) "cmd.exe" else "/bin/sh"
        val argument = if (isWindows) "/c" else "-c"

        // Run the `docker images` command to list all images
        val processBuilder = ProcessBuilder(shell, argument, "docker images --format \"{{.Repository}}:{{.Tag}}\"")
        val process = processBuilder.start()

        // Capture both stdout and stderr
        val output = BufferedReader(InputStreamReader(process.inputStream)).readText().trim()

        // Wait for the process to complete
        val exitCode = process.waitFor()

        // Check if the process executed successfully
        if (exitCode != 0) {
            return false
        }

        // Check if the image is present in the output
        return output.lines().any { it.trim() == imageTag }
    }
}
