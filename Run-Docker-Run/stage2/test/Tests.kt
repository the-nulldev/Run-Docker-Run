import org.hyperskill.hstest.dynamic.DynamicTest
import org.hyperskill.hstest.stage.StageTest
import org.hyperskill.hstest.testcase.CheckResult
import org.hyperskill.hstest.testing.TestedProgram
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class DockerTest : StageTest<String>() {

    private val requiredInstructions = listOf("FROM", "WORKDIR", "EXPOSE", "RUN", "ENTRYPOINT")
    private val allowedBaseImages = listOf("eclipse-temurin", "amazoncorretto", "openjdk")

    /**
     * Test 1: Check if the Dockerfile exists and contains valid content
     */
    @DynamicTest
    fun test1_checkDockerfileContent(): CheckResult {
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
        val dockerfileLines = dockerfileContent.lines().filter { it.isNotBlank() }

        for (instruction in requiredInstructions) {
            if (!dockerfileLines.any { it.startsWith(instruction.toLowerCase()) }) {
                return when (instruction) {
                    "WORKDIR" -> CheckResult.wrong("The Dockerfile does not have an instruction to set the working directory!")
                    "RUN" -> CheckResult.wrong("The Dockerfile should include RUN commands for to install dependencies, set permissions, and build the app using Gradle!")
                    "EXPOSE" -> CheckResult.wrong("The Dockerfile should expose port 8080 for the Spring Boot application!")
                    "ENTRYPOINT" -> CheckResult.wrong("The Dockerfile should include an entrypoint to run the Spring Boot application!")
                    else -> CheckResult.wrong("The Dockerfile is missing the required instruction: $instruction")
                }
            }
        }

        // Check if either COPY or ADD is present in the Dockerfile
        if (!dockerfileContent.contains("copy") && !dockerfileContent.contains("add")) {
            return CheckResult.wrong("The Dockerfile must include instructions to transfer files into the image!")
        }

        val copyCount = dockerfileContent.lines().count { it.trim().startsWith("copy") || it.trim().startsWith("add") }
        if (copyCount < 3) {
            return CheckResult.wrong("The Dockerfile should include at least 3 instructions to transfer files into the image since the files are copied to different target directories!")
        }

        val runCount = dockerfileContent.lines().count { it.trim().startsWith("run") }
        if (runCount < 2) {
            return CheckResult.wrong("The Dockerfile should include at least 2 RUN commands to set permissions and build the app using Gradle!")
        }
        return CheckResult.correct()
    }

    /**
     * Test 2: Check if valid JVM-based base images are used
     */
    @DynamicTest
    fun test2_checkBaseImages(): CheckResult {
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

        // Check if at least one valid JVM-based base image is used
        val invalidImages = baseImages.filterNot { baseImage ->
            allowedBaseImages.any { baseImage.startsWith(it) }
        }

        return if (invalidImages.isEmpty()) {
            CheckResult.correct()
        } else {
            CheckResult.wrong(
                "The Dockerfile uses invalid base image(s): ${invalidImages.joinToString(", ")}. " +
                        "Please use amy of these base images: `eclipse-temurin`, `amazoncorretto`, or `openjdk`."
            )
        }
    }

    /**
     * Test 3: Check if the Dockerfile uses multi-stage builds
     */
    @DynamicTest
    fun test3_checkMultiStageBuild(): CheckResult {
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

        // Count the number of `FROM` instructions
        val fromCount = dockerfileContent.lines().count { it.trim().startsWith("from ") }

        // Ensure there are at least two `FROM` instructions for multi-stage builds
        return if (fromCount >= 2) {
            CheckResult.correct()
        } else {
            CheckResult.wrong(
                "The Dockerfile should use multi-stage builds!"
            )
        }
    }

    /**
     * Test 4: Verify the exposed port in the Dockerfile
     */
    @DynamicTest
    fun test4_checkExposedPort(): CheckResult {
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

        // Check if port 8080 is exposed
        val isPortExposed = dockerfileContent.lines().any { it.trim() == "expose 8080" }
        return if (isPortExposed) {
            CheckResult.correct()
        } else {
            CheckResult.wrong("The Dockerfile should expose port 8080 for the Spring Boot application!")
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
