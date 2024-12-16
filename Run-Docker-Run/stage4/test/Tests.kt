import org.hyperskill.hstest.dynamic.DynamicTest
import org.hyperskill.hstest.stage.StageTest
import org.hyperskill.hstest.testcase.CheckResult
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class DockerTest : StageTest<String>() {

    private val ancestorImage = "hyper-web-app"
    private val testUrl = "http://localhost:8080"
    private val expectedContent = "Welcome to <span class=\"highlight\">Run Docker Run</span> 🚀"

    /**
     * Test 1: Verify the container is running and using the correct image
     */
    @DynamicTest
    fun test1_checkContainerState(): CheckResult {
        // Get the list of running containers
        val runningContainers = getRunningContainers()

        if (runningContainers.isEmpty()) {
            return CheckResult.wrong("No running containers were found. Ensure that your container is running.")
        }

        // Find the container using the specified image
        val container = runningContainers.find { it["Image"] == ancestorImage }

        if (container == null) {
            return CheckResult.wrong("Couldn't find a running container created from the '$ancestorImage' image!")
        }

        // Check if port 8080 is exposed and mapped
        val ports = container["Ports"]?.replace("\\u003e", ">") ?: ""

        // Check if the port mapping exists
        if (!ports.contains("8080->8080")) {
            return CheckResult.wrong("The container should map port 8080 on the container to port 8080 on the host! Found: $ports")
        }
        return CheckResult.correct()
    }

    /**
     * Test 2: Verify the web server is running and responding correctly
     */
    @DynamicTest
    fun test2_checkWebServerResponse(): CheckResult {
        return try {
            // Make an HTTP GET request to the test URL
            val connection = URL(testUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            // Check the HTTP response code
            if (connection.responseCode != 200) {
                return CheckResult.wrong("The server returned a non-200 HTTP status code: ${connection.responseCode}")
            }

            // Check the response content
            val responseContent = connection.inputStream.bufferedReader().readText()
            if (!responseContent.contains(expectedContent)) {
                return CheckResult.wrong("The server's response does not contain the expected content!")
            }

            CheckResult.correct()
        } catch (e: Exception) {
            CheckResult.wrong("Failed to connect to the server at $testUrl. " +
                    "Make sure the container is running and the application is accessible!")
        }
    }

    /**
     * Helper function to get a list of running containers
     */
    private fun getRunningContainers(): List<Map<String, String>> {
        try {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            val shell = if (isWindows) "cmd.exe" else "/bin/sh"
            val argument = if (isWindows) "/c" else "-c"

            // Run the `docker ps` command to list running containers
            val processBuilder = ProcessBuilder(shell, argument, "docker ps --format \"{{json .}}\"")
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
            throw RuntimeException("Error while fetching running containers: ${e.message}", e)
        }
    }
}
