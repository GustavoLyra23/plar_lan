package core.gateways

import core.io.runCLICommand
import kotlinx.coroutines.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.exists

class GithubGateway {
    private val log: Logger = LoggerFactory.getLogger("GithubGateway")

    private constructor()

    //singleton
    companion object {
        val instance: GithubGateway by lazy {
            GithubGateway()
        }
    }

    suspend fun getLibrary(libUrl: String, destDir: Path) {
        log.info("Buscando biblioteca: $libUrl")
        delay(5000) // only for visual test... remove this later...
        val finalFolderName = libUrl.substringAfterLast("/").removeSuffix(".git")
        val targetDir = destDir.resolve(finalFolderName)
        if (targetDir.exists()) {
            runCLICommand(
                listOf("git", "fetch", "--all", "--prune"),
                workingDir = targetDir
            )
        } else {
            runCLICommand(
                listOf(
                    "git",
                    "clone",
                    libUrl,
                    targetDir.toString()
                ), workingDir = destDir
            )
        }
    }
}
