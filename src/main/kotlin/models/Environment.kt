package models

import models.errors.MemoryError
import org.gustavolyra.PlarParser
import org.gustavolyra.PlarParser.DeclaracaoClasseContext
import org.gustavolyra.PlarParser.DeclaracaoInterfaceContext

class Environment(val enclosing: Environment? = null) {
    private val values = mutableMapOf<String, Value>()

    private val classes = mutableMapOf<String, DeclaracaoClasseContext?>()

    private val interfaces = mutableMapOf<String, DeclaracaoInterfaceContext>()

    // refers to the actual instance
    var thisObject: Value.Object? = null

    fun setInterface(name: String, declaration: DeclaracaoInterfaceContext) {
        interfaces[name] = declaration
    }

    fun getInterface(name: String): DeclaracaoInterfaceContext? {
        return interfaces[name] ?: enclosing?.getInterface(name)
    }

    fun interfaceExists(name: String): Boolean {
        return this.interfaces.containsKey(name)
    }

    fun getInterfaces(classeContext: DeclaracaoClasseContext): List<String> {
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

    fun getSuperClasse(classeContext: PlarParser.DeclaracaoClasseContext): String? {
        for (i in 0 until classeContext.childCount) {
            if (classeContext.getChild(i).text == "estende" && i + 1 < classeContext.childCount) {
                return classeContext.getChild(i + 1).text
            }
        }
        return null
    }


    fun define(name: String, value: Value) {
        values[name] = value
    }

    fun get(name: String): Value {
        if (name == "nulo") return Value.Null
        if (name == "this" && thisObject != null) return thisObject!!
        //TODO: refatorar...
        val value = values[name]
        if (value != null) return value
        if (thisObject != null) {
            val fieldValue = thisObject!!.fields[name]
            if (fieldValue != null) return fieldValue
        }
        val externalValue = enclosing?.get(name)
        if (externalValue != null && externalValue != Value.Null) return externalValue
        throw MemoryError("Nao foi possivel achar a variavel")
    }

    fun defineClass(name: String, declaration: DeclaracaoClasseContext?) {
        classes[name] = declaration
    }

    fun getClass(name: String): DeclaracaoClasseContext? {
        return classes[name] ?: enclosing?.getClass(name)
    }

    fun classExists(name: String): Boolean {
        return classes.containsKey(name)
    }

    fun updateOrDefine(name: String, value: Value) {
        var actualEnv: Environment? = this
        while (actualEnv != null) {
            if (actualEnv.values.containsKey(name)) {
                //found the variable, update the correct scope
                actualEnv.values[name] = value
                return
            }
            actualEnv = actualEnv.enclosing
        }
        values[name] = value
    }
}
