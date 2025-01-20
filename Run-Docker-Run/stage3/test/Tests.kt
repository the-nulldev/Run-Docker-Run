import org.hyperskill.hstest.dynamic.DynamicTest
import org.hyperskill.hstest.stage.StageTest
import org.hyperskill.hstest.testcase.CheckResult
import org.hyperskill.hstest.testing.TestedProgram
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class DockerTest : StageTest<String>() {

    private val requiredImageTag = "hyper-web-app:latest"

    /**
     * Test 1: Check if the custom image `hyper-web-app:latest` exists in the system
     */

    @DynamicTest
    fun test3_checkCustomImageExists(): CheckResult {
        // Step 1: Get the Dockerfile path from the student's program
        val program = TestedProgram()
        val dockerfilePath = program.start().trim()

        // Check if the Dockerfile path is valid
        val dockerfile = File(dockerfilePath)
        if (!dockerfile.exists() || !dockerfile.isFile) {
            return CheckResult.wrong("The provided Dockerfile path '$dockerfilePath' is invalid or the file does not exist.")
        }

        // Extract base images from the Dockerfile
        val customImage = "hyper-web-app:latest"

        // Check if the custom images used in the Dockerfile exist in the local system
        val isImagePresent = {
            val process = ProcessBuilder("docker", "images", "--format", "{{.Repository}}:{{.Tag}}")
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val images = reader.readLines()

            images.any { it.startsWith(customImage) }
        }

        return if (isImagePresent.invoke()) {
            CheckResult.correct()
        } else {
            CheckResult.wrong(
                "The custom Docker image '$requiredImageTag' was not found in the system. " +
                        "Make sure to build the image using the correct tag!"
            )
        }
    }
}
