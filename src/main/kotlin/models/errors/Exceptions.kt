package models.errors

import models.Value

class BreakException : RuntimeException()
class ContinueException : RuntimeException()
class RetornoException(val value: Value) : RuntimeException()
// achar um use case pra essa exception...
class MainExecutionException(msg: String) : RuntimeException(msg)
class ArquivoException(msg: String) : RuntimeException(msg)
class SemanticError(msg: String?) : RuntimeException(msg)
class MemoryError(msg: String?) : RuntimeException(msg)
