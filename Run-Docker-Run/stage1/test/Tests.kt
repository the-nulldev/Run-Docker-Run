import org.hyperskill.hstest.dynamic.DynamicTest
import org.hyperskill.hstest.stage.StageTest
import org.hyperskill.hstest.testcase.CheckResult
import org.hyperskill.hstest.testing.TestedProgram
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class DockerTest : StageTest<String>() {

    private val requiredInstructions = listOf("FROM")
    private val allowedBaseImages = listOf("eclipse-temurin", "amazoncorretto", "openjdk")

    /**
     * Test 1: Check if valid JVM-based base images are used
     */
    @DynamicTest
    fun test1_checkBaseImages(): CheckResult {
        // Step 1: Get the Dockerfile path
        val program = TestedProgram()
        val dockerfilePath = program.start().trim()

        // Check if the Dockerfile path is valid
        val dockerfile = File(dockerfilePath)
        if (!dockerfile.exists() || !dockerfile.isFile) {
            return CheckResult.wrong("The provided Dockerfile path '$dockerfilePath' is invalid or the file does not exist.")
        }

        // Read the Dockerfile content
        val dockerfileContent = dockerfile.readText().lowercase()

        // Extract base images from the Dockerfile
        val baseImages = extractBaseImages(dockerfileContent)

        // Check if at least one valid JVM-based base image is used
        val invalidImages = baseImages.filterNot { baseImage ->
            allowedBaseImages.any { baseImage.startsWith(it) }
        }

        return if (invalidImages.isEmpty()) {
            CheckResult.correct()
        } else {
            CheckResult.wrong(
                "The Dockerfile uses invalid base image(s): ${invalidImages.joinToString(", ")}. " +
                        "Please use a valid JVM-based base image such as `eclipse-temurin`, `amazoncorretto`, or `openjdk`."
            )
        }
    }

    /**
     * Test 2: Check if the Dockerfile uses multi-stage builds
     */
    @DynamicTest
    fun test2_checkMultiStageBuild(): CheckResult {
        // Step 1: Get the Dockerfile path from the student's program
        val program = TestedProgram()
        val dockerfilePath = program.start().trim()

        // Check if the Dockerfile path is valid
        val dockerfile = File(dockerfilePath)
        if (!dockerfile.exists() || !dockerfile.isFile) {
            return CheckResult.wrong("The provided Dockerfile path '$dockerfilePath' is invalid or the file does not exist.")
        }

        // check if .dockerignore file exists
        val dockerignore = File(dockerfile.parent, ".dockerignore")
        if (!dockerignore.exists() || !dockerignore.isFile) {
            return CheckResult.wrong("The .dockerignore file is missing in the project directory.")
        }

        // Read the Dockerfile content
        val dockerfileContent = dockerfile.readText().lowercase()

        // Count the number of `FROM` instructions
        val fromCount = dockerfileContent.lines().count { it.trim().startsWith("from ") }

        // Ensure there are at least two `FROM` instructions for multi-stage builds
        // the first `FROM` instruction is for the base image and the second `FROM` instruction is for the final image
        // the first `FROM` instruction should be the build stage and the second `FROM` instruction should be the run stage
        // check that it has a "AS" keyword to name the build stage
        return if (fromCount >= 2 && dockerfileContent.contains(" as ")) {
            CheckResult.correct()
        } else {
            CheckResult.wrong(
                "The Dockerfile should use multi-stage builds with at least two `FROM` instructions. " +
                        "Please ensure that the Dockerfile has a build stage and a run stage and uses the appropriate keywords to name them."
            )
        }

    }

    /**
     * Test 3: Check if the JVM images used in the Dockerfile exist in the local system by listing images
     */

    @DynamicTest
    fun test3_checkJVMImagesExist(): CheckResult {
        // Step 1: Get the Dockerfile path from the student's program
        val program = TestedProgram()
        val dockerfilePath = program.start().trim()

        // Check if the Dockerfile path is valid
        val dockerfile = File(dockerfilePath)
        if (!dockerfile.exists() || !dockerfile.isFile) {
            return CheckResult.wrong("The provided Dockerfile path '$dockerfilePath' is invalid or the file does not exist.")
        }

        // Read the Dockerfile content
        val dockerfileContent = dockerfile.readText().lowercase()

        // Extract base images from the Dockerfile
        val baseImages = extractBaseImages(dockerfileContent)

        // Check if the JVM images used in the Dockerfile exist in the local system
        val missingImages = baseImages.filterNot { baseImage ->
            val process = ProcessBuilder("docker", "images", "--format", "{{.Repository}}:{{.Tag}}")
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val images = reader.readLines()

            images.any { it.startsWith(baseImage) }
        }

        return if (missingImages.isEmpty()) {
            CheckResult.correct()
        } else {
            CheckResult.wrong(
                "The following base image(s) are missing in the local system: ${missingImages.joinToString(", ")}. " +
                        "Please ensure that the required base images are available in the local system."
            )
        }
    }


    /**
     * Extracts the base images (e.g., `FROM <base-image>`) from the Dockerfile content.
     */
    private fun extractBaseImages(dockerfileContent: String): List<String> {
        val regex = Regex("""from\s+([a-zA-Z0-9\-_/]+:[a-zA-Z0-9\-_.]+)""") // Matches 'FROM <image>:<tag>'
        return regex.findAll(dockerfileContent)
            .map { it.groupValues[1] } // Extract the full image name with the tag
            .toList()
    }
}
