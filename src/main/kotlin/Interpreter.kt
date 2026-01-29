package org.gustavolyra.portugolpp

import helpers.solvePath
import isDot
import models.Environment
import models.Value
import models.enums.LOOP
import models.errors.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.gustavolyra.portugolpp.PortugolPPParser.*
import processors.*
import java.nio.file.Files


@Suppress("REDUNDANT_OVERRIDE")
class Interpreter : PortugolPPBaseVisitor<Value>() {
    private var global = Environment()

    // actual exec environment
    private var ambiente = global

    // ref to the function being executed
    private var funcaoAtual: Value.Funcao? = null

    private val arquivosImportados = mutableSetOf<String>()

    init {
        defineDefaultFunctions(global)
    }

    override fun visitImportarDeclaracao(ctx: ImportarDeclaracaoContext): Value {
        val nomeArquivo = ctx.TEXTO_LITERAL().text.removeSurrounding("\"")
        processarImport(nomeArquivo)
        return Value.Null
    }

    private fun processarDeclaracoesDoArquivo(tree: ProgramaContext) {
        tree.declaracao().forEach { declaracao ->
            declaracao.declaracaoInterface()?.let {
                visitDeclaracaoInterface(it)
            }
        }

        tree.declaracao()?.forEach { declaracao ->
            declaracao.declaracaoClasse()?.let {
                visitDeclaracaoClasse(it)
            }
        }

        tree.declaracao()?.forEach { declaracao ->
            declaracao.declaracaoFuncao()?.let {
                visitDeclaracaoFuncao(it)
            }
        }

        tree.declaracao()?.forEach { declaracao ->
            declaracao.declaracaoVar()?.let {
                visitDeclaracaoVar(it)
            }
        }
    }


    fun processarImport(nomeArquivo: String) {
        if (arquivosImportados.contains(nomeArquivo)) return
        arquivosImportados.add(nomeArquivo)
        try {
            val path = solvePath(nomeArquivo)
            val conteudo = Files.readString(path)
            val lexer = PortugolPPLexer(CharStreams.fromString(conteudo))
            val tokens = CommonTokenStream(lexer)
            val parser = PortugolPPParser(tokens)
            val arvore = parser.programa()

            arvore.importarDeclaracao().forEach { import ->
                visitImportarDeclaracao(import)
            }

            processarDeclaracoesDoArquivo(arvore)
        } catch (e: Exception) {
            throw ArquivoException(e.message ?: "Falha ao processar import")
        }
    }

    fun interpretar(tree: ProgramaContext) {
        try {
            tree.importarDeclaracao()?.forEach { import ->
                visitImportarDeclaracao(import)
            }

            visitInterfaces(tree, global)
            visitClasses(tree, global)
            tree.declaracao().forEach { visit(it) }
        } catch (e: Exception) {
            println(e)
        }
    }

    override fun visitDeclaracaoInterface(ctx: DeclaracaoInterfaceContext): Value {
        val nomeInterface = ctx.ID().text
        global.setInterface(nomeInterface, ctx)
        return Value.Null
    }

    override fun visitDeclaracaoTentarCapturar(ctx: DeclaracaoTentarCapturarContext?): Value? {
        try {
            visit(ctx?.bloco(0))
        } catch (_: Exception) {
            visit(ctx?.bloco(1))
        }
        return Value.Null
    }

    override fun visitDeclaracaoClasse(ctx: DeclaracaoClasseContext): Value {
        val nomeClasse = ctx.ID(0).text

        getSuperClass(ctx)?.let { sc ->
            validateSuperClass(sc, nomeClasse, global)
        }
        getIndexFromWord(ctx, "implementa").takeIf { it >= 0 }?.let { idx ->
            val interfaces = readIdentitiesToKey(ctx, idx + 1)
            validateInterface(ctx, nomeClasse, interfaces, global)
        }
        global.defineClass(nomeClasse, ctx)
        return Value.Null
    }

    override fun visitDeclaracaoVar(ctx: DeclaracaoVarContext): Value {
        val nome = ctx.ID().text
        val tipo = ctx.tipo()?.text
        val valor = when {
            (ctx.expressao() != null) -> visit(ctx.expressao());
            else -> visit(ctx.declaracaoFuncao())
        }

        if (tipo != null) {
            if (valor is Value.Object) {
                val nomeClasse = valor.klass
                if (tipo != nomeClasse && valor.superClasse != tipo && !valor.interfaces.contains(tipo)) throw SemanticError(
                    "Tipo de variável '$tipo' não corresponde ao tipo do objeto '$nomeClasse'"
                )
            } else {
                if (tipo != valor.typeString()) throw SemanticError("Tipo da variavel nao corresponde ao tipo correto atribuido.")
            }
        }
        ambiente.define(nome, valor)
        return Value.Null
    }

    override fun visitDeclaracaoFuncao(ctx: DeclaracaoFuncaoContext): Value {
        val nome = ctx.ID().text
        val tipoRetorno = ctx.tipo()?.text
        if (isReturnInvalid(tipoRetorno, global)) throw SemanticError("Tipo de retorno inválido: $tipoRetorno")
        val func = Value.Funcao(
            nome = nome,
            declaracao = ctx,
            tipoRetorno = tipoRetorno,
            closure = ambiente,
            implementacao = definirImplementacao(ctx, nome, Environment(ambiente))
        )
        ambiente.define(nome, func)
        return func
    }

    private fun definirImplementacao(
        ctx: DeclaracaoFuncaoContext, nome: String, closure: Environment
    ): (List<Value>) -> Value {
        return { argumentos ->
            val numParamsDeclarados = ctx.listaParams()?.param()?.size ?: 0
            if (argumentos.size > numParamsDeclarados) throw SemanticError("Função '$nome' recebeu ${argumentos.size} parâmetros, mas espera $numParamsDeclarados")
            ctx.listaParams()?.param()?.forEachIndexed { i, param ->
                if (i < argumentos.size) closure.define(param.ID().text, argumentos[i])
            }

            val ambienteAnterior = ambiente
            ambiente = closure
            val funcao = Value.Funcao(
                ctx.ID().text, ctx, ctx.tipo()?.text, global
            )
            val funcaoAnterior = funcaoAtual
            funcaoAtual = funcao
            try {
                visit(ctx.bloco())
                Value.Null
            } catch (retorno: RetornoException) {
                retorno.value
            } finally {
                ambiente = ambienteAnterior
                funcaoAtual = funcaoAnterior
            }
        }
    }

    //TODO: refatorar vist para declaracao de return
    override fun visitDeclaracaoRetornar(ctx: DeclaracaoRetornarContext): Value {
        val valueRetorno = ctx.expressao()?.let { visit(it) } ?: Value.Null
        // apenas valida se estivermos dentro de uma funcao
        if (funcaoAtual != null && funcaoAtual!!.tipoRetorno != null) {
            val tipoEsperado = funcaoAtual!!.tipoRetorno
            val tipoAtual = valueRetorno.typeString()
            if (tipoEsperado != tipoAtual) {
                if (valueRetorno is Value.Object) {
                    //TODO: colocar verificao de superclasses e interfaces...
                    if (valueRetorno.superClasse == tipoEsperado || valueRetorno.interfaces.contains(tipoEsperado)) throw RetornoException(
                        valueRetorno
                    )
                }
                throw SemanticError("Erro de tipo: funcao '${funcaoAtual!!.nome}' deve retornar '$tipoEsperado', mas esta retornando '$tipoAtual'")
            }
        }
        throw RetornoException(valueRetorno)
    }

    override fun visitDeclaracaoSe(ctx: DeclaracaoSeContext): Value {
        val condicao = visit(ctx.expressao())
        if (condicao !is Value.Logico) throw SemanticError("Condição do 'if' deve ser lógica")
        return if (condicao.valor) visit(ctx.declaracao(0)) else ctx.declaracao(1)?.let { visit(it) } ?: Value.Null
    }

    override fun visitBloco(ctx: BlocoContext): Value {
        val anterior = ambiente
        ambiente = Environment(anterior)
        ambiente.thisObject = anterior.thisObject
        try {
            ctx.declaracao().forEach { visit(it) }
        } finally {
            ambiente = anterior
        }
        return Value.Null
    }

    override fun visitExpressao(ctx: ExpressaoContext): Value = visit(ctx.getChild(0))

    override fun visitAtribuicao(ctx: AtribuicaoContext): Value {
        ctx.logicaOu()?.let { return visit(it) }
        val rhs = when {
            ctx.expressao() != null -> visit(ctx.expressao())
            else -> throw SemanticError("Atribuicao invalida")
        }
        val id = ctx.ID()
        val acesso = ctx.acesso()
        val arr = ctx.acessoArray()
        return when {
            id != null -> rhs.also { v ->
                ambiente.updateOrDefine(id.text, v)
            }

            acesso != null -> {
                val obj = visit(acesso.primario()) as? Value.Object
                    ?: throw SemanticError("Não é possível atribuir a uma propriedade de um não-objeto")
                val v = rhs
                obj.campos[acesso.ID().text] = v
                v
            }

            arr != null -> {
                when (val container = visit(arr.primario())) {
                    is Value.List -> {
                        val i = visit(arr.expressao(0)) as? Value.Integer
                            ?: throw SemanticError("Índice de lista deve ser um número inteiro")
                        val indice = i.valor
                        if (indice < 0) throw SemanticError("Índice negativo não permitido: $indice")
                        container.elementos[indice] = rhs
                        rhs
                    }

                    //TODO: arrumar indices nos mapas...
                    is Value.Map -> {
                        val chave = visit(arr.expressao(0))
                        container.elementos[chave] = rhs
                        rhs
                    }

                    else -> throw SemanticError(
                        "Operação de atribuição com índice não suportada para ${container::class.simpleName}"
                    )
                }
            }

            else -> throw SemanticError("Erro de sintaxe na atribuição")
        }
    }

    override fun visitAcesso(ctx: AcessoContext): Value {
        val objeto = visit(ctx.primario())

        if (objeto !is Value.Object) {
            throw SemanticError("Tentativa de acessar propriedade de um não-objeto")
        }

        val propriedade = ctx.ID().text

        val value = objeto.campos[propriedade] ?: return Value.Null
        return value
    }

    override fun visitLogicaOu(ctx: LogicaOuContext): Value {
        var esquerda = visit(ctx.logicaE(0))
        for (i in 1 until ctx.logicaE().size) {
            if (esquerda is Value.Logico && esquerda.valor) return Value.Logico(true)
            val direita = visit(ctx.logicaE(i))
            if (esquerda !is Value.Logico || direita !is Value.Logico) throw SemanticError("Operador 'ou' requer valores lógicos")
            esquerda = Value.Logico(direita.valor)
        }
        return esquerda
    }

    override fun visitLogicaE(ctx: LogicaEContext): Value {
        var esquerda = visit(ctx.igualdade(0))
        for (i in 1 until ctx.igualdade().size) {
            if (esquerda is Value.Logico && !esquerda.valor) return Value.Logico(false)
            val direita = visit(ctx.igualdade(i))
            if (esquerda !is Value.Logico || direita !is Value.Logico) throw SemanticError("Operador 'e' requer valores lógicos")
            esquerda = Value.Logico(direita.valor)
        }
        return esquerda
    }

    override fun visitIgualdade(ctx: IgualdadeContext): Value {
        var esquerda = visit(ctx.comparacao(0))

        for (i in 1 until ctx.comparacao().size) {
            val operador = ctx.getChild(i * 2 - 1).text
            val direita = visit(ctx.comparacao(i))

            if (operador == "==") {
                val resultado = when {
                    esquerda == Value.Null && direita == Value.Null -> true
                    esquerda == Value.Null || direita == Value.Null -> false
                    else -> areEqual(esquerda, direita)
                }
                esquerda = Value.Logico(resultado)
            } else if (operador == "!=") {
                val resultado = when {
                    esquerda == Value.Null && direita == Value.Null -> false
                    esquerda == Value.Null || direita == Value.Null -> true
                    else -> !areEqual(esquerda, direita)
                }
                esquerda = Value.Logico(resultado)
            }
        }

        return esquerda
    }

    override fun visitComparacao(ctx: ComparacaoContext): Value {
        var esquerda = visit(ctx.adicao(0))
        for (i in 1 until ctx.adicao().size) {
            val operador = ctx.getChild(i * 2 - 1).text
            val direita = visit(ctx.adicao(i))
            esquerda = when (operador) {
                "<" -> compare("<", esquerda, direita)
                "<=" -> compare("<=", esquerda, direita)
                ">" -> compare(">", esquerda, direita)
                ">=" -> compare(">=", esquerda, direita)
                else -> throw SemanticError("Operador desconhecido: $operador")
            }
        }
        return esquerda
    }


    override fun visitAdicao(ctx: AdicaoContext): Value {
        var esquerda = visit(ctx.multiplicacao(0))
        for (i in 1 until ctx.multiplicacao().size) {
            val operador = ctx.getChild(i * 2 - 1).text
            val direita = visit(ctx.multiplicacao(i))
            esquerda = processAdd(operador, esquerda, direita)
        }
        return esquerda
    }

    override fun visitMultiplicacao(ctx: MultiplicacaoContext): Value {
        var esquerda = visit(ctx.unario(0))
        for (i in 1 until ctx.unario().size) {
            val operador = ctx.getChild(i * 2 - 1).text
            val direita = visit(ctx.unario(i))
            esquerda = processMultiplication(operador, esquerda, direita)
        }
        return esquerda
    }

    override fun visitUnario(ctx: UnarioContext): Value {
        if (ctx.childCount == 2) {
            val operador = ctx.getChild(0).text
            val operando = visit(ctx.unario())
            return when (operador) {
                "!" -> if (operando is Value.Logico) Value.Logico(!operando.valor) else throw SemanticError("Operador '!' requer valor lógico")
                "-" -> when (operando) {
                    is Value.Integer -> Value.Integer(-operando.valor)
                    is Value.Real -> Value.Real(-operando.valor)
                    else -> throw SemanticError("Operador '-' requer valor numérico")
                }

                else -> throw SemanticError("Operador unário desconhecido: $operador")
            }
        }
        return visit(ctx.getChild(0))
    }

    private fun buscarPropriedadeNaHierarquia(`object`: Value.Object, nomeCampo: String): Value? {
        val valorCampo = `object`.campos[nomeCampo]
        if (valorCampo != null) {
            return valorCampo
        }

        if (`object`.superClasse != null) {
            val tempObjeto = criarObjetoTemporarioDaClasse(`object`.superClasse)
            return buscarPropriedadeNaHierarquia(tempObjeto, nomeCampo)
        }

        return null
    }


    private fun criarObjetoTemporarioDaClasse(nomeClasse: String): Value.Object {
        val classe = global.getClass(nomeClasse) ?: throw SemanticError("Classe não encontrada: $nomeClasse")

        val superClasse = global.getSuperClasse(classe)
        val interfaces = global.getInterfaces(classe)

        val `object` = Value.Object(nomeClasse, mutableMapOf(), superClasse, interfaces)

        classe.declaracaoVar().forEach { decl ->
            val nomeCampo = decl.ID().text
            val value = decl.expressao()?.let {
                val oldAmbiente = ambiente
                ambiente = Environment(global).apply { thisObject = `object` }
                val result = visit(it)
                ambiente = oldAmbiente
                result
            } ?: Value.Null
            `object`.campos[nomeCampo] = value
        }

        return `object`
    }

    private fun buscarMetodoNaHierarquia(`object`: Value.Object, nomeMetodo: String): DeclaracaoFuncaoContext? {
        val classe = global.getClass(`object`.klass) ?: return null
        val metodo = classe.declaracaoFuncao().find { it.ID().text == nomeMetodo }
        if (metodo != null) return metodo
        if (`object`.superClasse != null) {
            val classeBase = global.getClass(`object`.superClasse) ?: return null
            val metodoBase = classeBase.declaracaoFuncao().find { it.ID().text == nomeMetodo }
            if (metodoBase != null) {
                return metodoBase
            }
            val superClasseDaBase = global.getSuperClasse(classeBase)
            if (superClasseDaBase != null) {
                val objectBase = Value.Object(`object`.superClasse, mutableMapOf(), superClasseDaBase)
                return buscarMetodoNaHierarquia(objectBase, nomeMetodo)
            }
        }
        return null
    }


    private fun ehChamada(ctx: ChamadaContext, i: Int, n: Int) = (i + 2) < n && ctx.getChild(i + 2).text == "("

    private fun extrairArgumentosEPasso(ctx: ChamadaContext, i: Int, n: Int): Pair<List<Value>, Int> {
        val temArgsCtx = (i + 3) < n && ctx.getChild(i + 3) is ArgumentosContext
        val argumentos = if (temArgsCtx) {
            val argsCtx = ctx.getChild(i + 3) as ArgumentosContext
            argsCtx.expressao().map { visit(it) }
        } else emptyList()
        val passo = if (temArgsCtx) 5 else 4
        return argumentos to passo
    }

    private fun chamarMetodoOuErro(obj: Value.Object, nome: String, argumentos: List<Value>): Value {
        val metodo = buscarMetodoNaHierarquia(obj, nome)
            ?: throw SemanticError("Metodo nao encontrado: $nome em classe ${obj.klass}")
        return executarMetodo(obj, metodo, argumentos)
    }

    private fun lerPropriedadeOuNulo(obj: Value.Object, nome: String): Value? = buscarPropriedadeNaHierarquia(obj, nome)

    override fun visitChamada(ctx: ChamadaContext): Value {
        ctx.acessoArray()?.let { return visit(it) }

        var r = visit(ctx.primario())
        var i = 1
        val n = ctx.childCount

        while (i < n) {
            if (r == Value.Null) return Value.Null
            if (!isDot(ctx, i)) break

            val id = ctx.getChild(i + 1).text
            val obj = comoObjetoOuErro(r)

            if (ehChamada(ctx, i, n)) {
                val (argumentos, passo) = extrairArgumentosEPasso(ctx, i, n)
                r = chamarMetodoOuErro(obj, id, argumentos)
                i += passo
            } else {
                r = lerPropriedadeOuNulo(obj, id) ?: Value.Null
                i += 2
            }
        }
        return r
    }

    override fun visitDeclaracaoEnquanto(ctx: DeclaracaoEnquantoContext): Value {
        var iteracoes = 0
        val maxIteracoes = LOOP.MAX_LOOP.valor

        while (iteracoes < maxIteracoes) {
            val condicao = visit(ctx.expressao())
            println("Condição do loop: $condicao")

            if (condicao !is Value.Logico) {
                throw SemanticError("Condição do 'enquanto' deve ser um valor lógico")
            }

            if (!condicao.valor) {
                println("Condição falsa, saindo do loop")
                break
            }

            iteracoes++
            println("Iteração $iteracoes do loop")

            try {
                visit(ctx.declaracao())
            } catch (e: RetornoException) {
                throw e
            } catch (_: BreakException) {
                break
            } catch (_: ContinueException) {
                continue
            }
        }

        if (iteracoes >= maxIteracoes) {
            println("Aviso: Loop infinito detectado! Saindo do loop.")
            return Value.Null
        }
        return Value.Null
    }

    override fun visitDeclaracaoPara(ctx: DeclaracaoParaContext): Value {
        ctx.declaracaoVar()?.let { visit(it) } ?: ctx.expressao(0)?.let { visit(it) }
        loop@ while (true) {
            val cond = visit(ctx.expressao(0)) as? Value.Logico
                ?: throw SemanticError("Condição do 'para' deve ser um valor lógico")
            if (!cond.valor) break

            var doIncrement = true
            try {
                visit(ctx.declaracao())
            } catch (e: Exception) {
                when (e) {
                    is RetornoException -> throw e
                    is BreakException -> {
                        doIncrement = false; break@loop
                    }

                    is ContinueException -> {}
                    else -> throw e
                }
            } finally {
                if (doIncrement) {
                    visit(ctx.expressao(1))
                }
            }
        }
        return Value.Null
    }

    override fun visitDeclaracaoFacaEnquanto(ctx: DeclaracaoFacaEnquantoContext): Value {
        var iter = 0
        do {
            try {
                visit(ctx.declaracao())
            } catch (_: BreakException) {
                break
            } catch (_: ContinueException) {
                // apenas pula...
            }
            val c = visit(ctx.expressao())
            val logicRes =
                (c as? Value.Logico)?.valor ?: throw SemanticError("Condição do 'enquanto' deve ser um valor lógico")
            if (!logicRes) break
            if (++iter >= 100) {
                println("Loop infinito detectado! Saindo do loop.")
                break
            }
        } while (true)

        return Value.Null
    }

    override fun visitDeclaracaoQuebra(ctx: DeclaracaoQuebraContext): Value {
        throw BreakException()
    }

    //TODO: ajustar declaracao de listas....
    override fun visitListaLiteral(ctx: ListaLiteralContext): Value {
        val indice = ctx.NUMERO().text.toInt()
        val list = mutableListOf<Value>()
        while (list.size < indice) list.add(Value.Null)
        return Value.List(list, indice)
    }


    override fun visitMapaLiteral(ctx: MapaLiteralContext): Value {
        return Value.Map()
    }

    private fun validarAcessoArray(ctx: AcessoArrayContext, container: Value.List): Value {
        val indice = visit(ctx.expressao(0))
        if (indice !is Value.Integer) throw SemanticError("Índice de lista deve ser um número inteiro")
        if (indice.valor < 0 || indice.valor >= container.tamanho) throw SemanticError("Índice fora dos limites da lista: ${indice.valor}")
        return container.elementos[indice.valor]
    }

    private fun validarAcessoMapa(ctx: AcessoArrayContext, container: Value.Map): Value {
        val chave = visit(ctx.expressao(0))

        // Para acesso bidimensional em mapas
        if (ctx.expressao().size > 1) {
            val primeiroElemento = container.elementos[chave] ?: Value.Null
            val segundoIndice = visit(ctx.expressao(1))

            when (primeiroElemento) {
                is Value.List -> {
                    when {
                        segundoIndice !is Value.Integer -> {
                            throw SemanticError("Segundo índice deve ser um número inteiro para acessar uma lista")
                        }

                        segundoIndice.valor < 0 || segundoIndice.valor >= primeiroElemento.elementos.size -> {
                            throw SemanticError("Segundo índice fora dos limites da lista: ${segundoIndice.valor}")
                        }

                        else -> return primeiroElemento.elementos[segundoIndice.valor]
                    }
                }
                //TODO: rever mapa case
                is Value.Map -> {
                    return primeiroElemento.elementos[segundoIndice] ?: Value.Null
                }
                // TODO: rever objeto case
                is Value.Object -> {
                    if (segundoIndice !is Value.Text) {
                        throw SemanticError("Chave para acessar campo de objeto deve ser texto")
                    }
                    return primeiroElemento.campos[segundoIndice.valor] ?: Value.Null
                }

                else -> {
                    throw SemanticError("Elemento com chave $chave não suporta acesso indexado")
                }
            }
        }
        return container.elementos[chave] ?: Value.Null
    }

    override fun visitAcessoArray(ctx: AcessoArrayContext): Value {
        return when (val container = visit(ctx.primario())) {
            is Value.List -> validarAcessoArray(ctx, container)
            is Value.Map -> validarAcessoMapa(ctx, container)
            else -> throw SemanticError("Operação de acesso com índice não suportada para ${container::class.simpleName}")
        }
    }

    override fun visitDeclaracaoContinue(ctx: DeclaracaoContinueContext): Value {
        throw ContinueException()
    }

    override fun visitChamadaFuncao(ctx: ChamadaFuncaoContext): Value {
        //TODO: implementar validacao dos tipos dos parametros
        val argumentos = ctx.argumentos()?.expressao()?.map { visit(it) } ?: emptyList()
        val funcName = ctx.ID().text
        return if (ctx.primario() != null) {
            val objeto = visit(ctx.primario())
            if (objeto !is Value.Object) throw SemanticError("Chamada de método em não-objeto")
            val classe =
                global.getClass(objeto.klass) ?: throw SemanticError("Classe não encontrada: ${objeto.klass}")
            val metodo = classe.declaracaoFuncao().find { it.ID().text == funcName }
                ?: throw SemanticError("Método não encontrado: $funcName")
            executarMetodo(objeto, metodo, argumentos)
        } else {
            chamadaFuncao(funcName, argumentos)
        }
    }

    private fun resolverFuncao(nome: String): Value.Funcao =
        runCatching { ambiente.get(nome) as? Value.Funcao }.getOrNull()
            ?: throw SemanticError("Função não encontrada ou não é função: $nome")

    private fun chamadaFuncao(nome: String, argumentos: List<Value>): Value {
        ambiente.thisObject?.let { obj ->
            buscarMetodoNaHierarquia(obj, nome)?.let { ctx ->
                return executarMetodo(
                    obj, ctx, argumentos
                )
            }
        }
        val funcao = resolverFuncao(nome)
        return funcao.implementacao?.invoke(argumentos)
            ?: throw SemanticError("Função '$nome' não possui implementação.")
    }


    private fun executarMetodo(
        `object`: Value.Object,
        metodo: DeclaracaoFuncaoContext,
        argumentos: List<Value>
    ): Value {
        val metodoEnvironment = Environment(global)
        metodoEnvironment.thisObject = `object`

        val funcao = Value.Funcao(
            metodo.ID().text, metodo, metodo.tipo()?.text, metodoEnvironment
        )
        val funcaoAnterior = funcaoAtual
        funcaoAtual = funcao

        val params = metodo.listaParams()?.param() ?: listOf()
        for (i in params.indices) {
            val paramNome = params[i].ID().text

            if (i < argumentos.size) {
                val valorArg = argumentos[i]
                metodoEnvironment.define(paramNome, valorArg)
            } else {
                metodoEnvironment.define(paramNome, Value.Null)
            }
        }

        val oldAmbiente = ambiente
        ambiente = metodoEnvironment

        try {
            visit(metodo.bloco())
            return Value.Null
        } catch (retorno: RetornoException) {
            return retorno.value
        } finally {
            ambiente = oldAmbiente
            funcaoAtual = funcaoAnterior
        }
    }

    private fun resolverIdPrimario(ctx: PrimarioContext): Value {
        val nome = ctx.ID().text
        if (ctx.childCount > 1 && ctx.getChild(1).text == "(") {
            val argumentos = if (ctx.childCount > 2 && ctx.getChild(2) is ArgumentosContext) {
                val argsCtx = ctx.getChild(2) as ArgumentosContext
                argsCtx.expressao().map { visit(it) }
            } else {
                emptyList()
            }
            return chamadaFuncao(nome, argumentos)
        } else {
            try {
                return ambiente.get(nome)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    private fun resolverClassePrimario(ctx: PrimarioContext): Value {
        val match = Regex("novo([A-Za-z0-9_]+)\\(.*\\)").find(ctx.text)
        if (match != null) {
            val nomeClasse = match.groupValues[1]

            val classe = global.getClass(nomeClasse) ?: throw SemanticError("Classe não encontrada: $nomeClasse")
            return criarObjetoClasse(nomeClasse, ctx, classe)
        } else {
            throw SemanticError("Sintaxe inválida para criação de objeto")
        }
    }


    override fun visitPrimario(ctx: PrimarioContext): Value {
        return when {
            ctx.text == "nulo" -> Value.Null
            ctx.listaLiteral() != null -> visit(ctx.listaLiteral())
            ctx.mapaLiteral() != null -> visit(ctx.mapaLiteral())
            ctx.NUMERO() != null -> ctx.NUMERO().text.let {
                if (it.contains(".")) Value.Real(it.toDouble()) else Value.Integer(
                    it.toInt()
                )
            }

            ctx.TEXTO_LITERAL() != null -> Value.Text(ctx.TEXTO_LITERAL().text.removeSurrounding("\""))
            ctx.ID() != null && !ctx.text.startsWith("novo") -> resolverIdPrimario(ctx);
            ctx.text == "verdadeiro" -> Value.Logico(true)
            ctx.text == "falso" -> Value.Logico(false)
            ctx.text == "este" -> ambiente.thisObject ?: throw SemanticError("'este' fora de contexto de objeto")
            ctx.text.startsWith("novo") -> resolverClassePrimario(ctx)
            ctx.expressao() != null -> visit(ctx.expressao())
            else -> {
                throw SemanticError("Tipo primario invalido")
            }
        }
    }

    //TODO: testar mais essa funcao de extracao argumentos do constructor..., ja testei com tipos simples e com objetos
    private fun extrairArgumentosDoConstructor(ctx: PrimarioContext): List<Value> {
        val args = mutableListOf<Value>()
        if (!ctx.argumentos().isEmpty) {
            ctx.argumentos().expressao().forEach { expr ->
                args.add(visit(expr))
            }
        }
        return args
    }

    //TODO: rever uso de recursao...
    private fun inicializarCamposDaClasseBase(`object`: Value.Object, nomeClasseBase: String) {
        val classeBase = global.getClass(nomeClasseBase) ?: return

        val superClasseDaBase = global.getSuperClasse(classeBase)
        if (superClasseDaBase != null) {
            inicializarCamposDaClasseBase(`object`, superClasseDaBase)
        }

        classeBase.declaracaoVar().forEach { decl ->
            val nomeCampo = decl.ID().text
            if (!`object`.campos.containsKey(nomeCampo)) {
                val oldAmbiente = ambiente
                ambiente = Environment(global).apply { thisObject = `object` }
                val value = decl.expressao()?.let { visit(it) } ?: Value.Null
                `object`.campos[nomeCampo] = value
                ambiente = oldAmbiente
            }
        }
    }

    private fun criarObjetoClasse(nomeClasse: String, ctx: PrimarioContext, classe: DeclaracaoClasseContext): Value {
        val superClasse = global.getSuperClasse(classe)
        val interfaces = global.getInterfaces(classe)

        val `object` = Value.Object(nomeClasse, mutableMapOf(), superClasse, interfaces)

        if (superClasse != null) inicializarCamposDaClasseBase(`object`, superClasse)

        classe.declaracaoVar().forEach { decl ->
            val nomeCampo = decl.ID().text
            val oldAmbiente = ambiente
            ambiente = Environment(global).apply { thisObject = `object` }
            val value = decl.expressao()?.let { visit(it) } ?: Value.Null
            `object`.campos[nomeCampo] = value
            ambiente = oldAmbiente
        }

        val inicializarMetodo = classe.declaracaoFuncao().find { it.ID().text == "inicializar" }
        if (inicializarMetodo != null) {
            val argumentos = extrairArgumentosDoConstructor(ctx)
            executarMetodo(`object`, inicializarMetodo, argumentos)
        }
        return `object`
    }
}
