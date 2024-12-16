import org.hyperskill.hstest.dynamic.DynamicTest
import org.hyperskill.hstest.stage.StageTest
import org.hyperskill.hstest.testcase.CheckResult
import java.io.BufferedReader
import java.io.InputStreamReader

class DockerCleanupTest : StageTest<String>() {

    private val ancestorImage = "hyper-web-app"
    private val ancestorTag = "hyper-web-app:latest"

    /**
     * Test 1: Ensure the container is stopped
     */
    @DynamicTest
    fun test1_checkContainerStopped(): CheckResult {
        val containers = getAllContainers()

        // Find the container using the ancestor image
        val container = containers.find { it["Image"] == ancestorImage }

        if (container == null) {
            return CheckResult.correct()
        }

        // Check if the container is still running
        val isRunning = container["State"]?.contains("running", ignoreCase = true) == true
        if (isRunning) {
            return CheckResult.wrong("The container created from the '$ancestorImage' image should be stopped!")
        }

        return CheckResult.correct()
    }

    /**
     * Test 2: Ensure the container is deleted
     */
    @DynamicTest
    fun test2_checkContainerDeleted(): CheckResult {
        val containers = getAllContainers()

        val containerExists = containers.any { it["Image"] == ancestorImage }
        if (containerExists) {
            return CheckResult.wrong("The container created from the '$ancestorImage' image should be deleted!")
        }

        return CheckResult.correct()
    }

    /**
     * Test 3: Ensure the image is removed
     */
    @DynamicTest
    fun test3_checkImageDeleted(): CheckResult {
        val images = getAllImages()

        // Check if the ancestor image still exists
        val imageExists = images.any { it["Repository"] == ancestorImage && it["Tag"] == "latest" }
        if (imageExists) {
            return CheckResult.wrong("The image '$ancestorTag' should be deleted from the system!")
        }

        return CheckResult.correct()
    }

    /**
     * Helper function to get a list of all containers
     */
    private fun getAllContainers(): List<Map<String, String>> {
        return executeDockerCommand("docker ps -a --format \"{{json .}}\"")
    }

    /**
     * Helper function to get a list of all images
     */
    private fun getAllImages(): List<Map<String, String>> {
        return executeDockerCommand("docker images --format \"{{json .}}\"")
    }

    /**
     * Helper function to execute a Docker command and parse JSON output
     */
    private fun executeDockerCommand(command: String): List<Map<String, String>> {
        try {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            val shell = if (isWindows) "cmd.exe" else "/bin/sh"
            val argument = if (isWindows) "/c" else "-c"

            // Execute the command
            val processBuilder = ProcessBuilder(shell, argument, command)
            val process = processBuilder.start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readLines()
            process.waitFor()

            if (output.isEmpty()) {
                return emptyList()
            }

            // Parse the JSON output into a list of maps
            return output.map { line ->
                line.trim().removeSurrounding("{", "}").split(",").associate {
                    val keyValue = it.split(":", limit = 2)
                    if (keyValue.size == 2) {
                        keyValue[0].trim().removeSurrounding("\"") to keyValue[1].trim().removeSurrounding("\"")
                    } else {
                        "" to ""
                    }
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Error while executing Docker command: ${e.message}", e)
        }
    }
}
