package models

import models.errors.MemoryError
import org.gustavolyra.portugolpp.PortugolPPParser

class Environment(val enclosing: Environment? = null) {
    private val values = mutableMapOf<String, Value>()

    private val classes = mutableMapOf<String, PortugolPPParser.DeclaracaoClasseContext?>()

    private val interfaces = mutableMapOf<String, PortugolPPParser.DeclaracaoInterfaceContext>()

    //referente a instancia que estamos...
    var thisObject: Value.Object? = null

    fun setInterface(nome: String, declaracao: PortugolPPParser.DeclaracaoInterfaceContext) {
        interfaces[nome] = declaracao
    }

    fun getInterface(nome: String): PortugolPPParser.DeclaracaoInterfaceContext? {
        return interfaces[nome] ?: enclosing?.getInterface(nome)
    }

    fun interfaceExists(nome: String): Boolean {
        return this.interfaces.containsKey(nome)
    }

    fun getInterfaces(classeContext: PortugolPPParser.DeclaracaoClasseContext): List<String> {
        val result = mutableListOf<String>()
        var foundImplements = false
        for (i in 0 until classeContext.childCount) {
            val token = classeContext.getChild(i).text
            if (foundImplements) {
                if (token == "{") break
                if (token != "," && token != "implementa") {
                    result.add(token)
                }
            } else if (token == "implementa") {
                foundImplements = true
            }
        }
        return result
    }

    fun getSuperClasse(classeContext: PortugolPPParser.DeclaracaoClasseContext): String? {
        for (i in 0 until classeContext.childCount) {
            if (classeContext.getChild(i).text == "estende" && i + 1 < classeContext.childCount) {
                return classeContext.getChild(i + 1).text
            }
        }
        return null
    }


    fun define(nome: String, value: Value) {
        values[nome] = value
    }

    fun get(nome: String): Value {
        if (nome == "nulo") return Value.Null
        if (nome == "this" && thisObject != null) return thisObject!!
        //TODO: refatorar...
        val valor = values[nome]
        if (valor != null) return valor
        if (thisObject != null) {
            val campoValor = thisObject!!.campos[nome]
            if (campoValor != null) return campoValor
        }
        val externoValor = enclosing?.get(nome)
        if (externoValor != null && externoValor != Value.Null) return externoValor
        throw MemoryError("Nao foi possivel achar a variavel")
    }

    fun defineClass(nome: String, declaracao: PortugolPPParser.DeclaracaoClasseContext?) {
        classes[nome] = declaracao
    }

    fun getClass(nome: String): PortugolPPParser.DeclaracaoClasseContext? {
        return classes[nome] ?: enclosing?.getClass(nome)
    }

    fun classExists(nome: String): Boolean {
        return classes.containsKey(nome)
    }

    fun updateOrDefine(nome: String, value: Value) {
        var environmentAtual: Environment? = this
        while (environmentAtual != null) {
            if (environmentAtual.values.containsKey(nome)) {
                //found the variable, update the correct scope
                environmentAtual.values[nome] = value
                return
            }
            environmentAtual = environmentAtual.enclosing
        }
        values[nome] = value
    }
}
