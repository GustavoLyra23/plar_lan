package processors

import models.Ambiente


fun retornoFuncaoInvalido(retorno: String?, global: Ambiente): Boolean {
    if (retorno == null) return false
    return retorno !in listOf(
        "Inteiro", "Real", "Texto", "Logico", "Nulo", "Lista", "Mapa"
    ) && (!global.classExists(retorno) && !global.interfaceExists(retorno))
}

