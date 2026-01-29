package processors

object FileIOProcessor {

    fun readFile(path: String): String {
        return try {
            java.io.File(path).readText()
        } catch (e: Exception) {
            throw RuntimeException("Erro ao ler o arquivo: $path", e)
        }
    }

    fun writeFile(path: String, data: String, append: Boolean = false) {
        try {
            val file = java.io.File(path)
            if (append) {
                file.appendText(data)
            } else {
                file.writeText(data)
            }
        } catch (e: Exception) {
            throw RuntimeException("Erro ao escrever no arquivo: $path", e)
        }
    }
}
