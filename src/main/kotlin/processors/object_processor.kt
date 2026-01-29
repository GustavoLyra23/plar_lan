package processors

import models.Ambiente
import models.Valor
import models.errors.SemanticError
import org.gustavolyra.portugolpp.PortugolPPParser.DeclaracaoClasseContext
import org.gustavolyra.portugolpp.PortugolPPParser.ProgramaContext

fun obterSuperclasseSeHouver(ctx: DeclaracaoClasseContext): String? =
    if (ctx.childCount > 3 && ctx.getChild(2).text == "estende") ctx.getChild(3).text else null


fun validarSuperclasseExiste(superClasse: String, nomeClasse: String, global: Ambiente) {
    global.obterClasse(superClasse)
        ?: throw SemanticError("Classe base '$superClasse' não encontrada para a classe '$nomeClasse'")
}

fun indiceDaPalavra(ctx: DeclaracaoClasseContext, palavra: String): Int {
    for (i in 0 until ctx.childCount) if (ctx.getChild(i).text == palavra) return i
    return -1
}

fun lerIdentificadoresAteChave(ctx: DeclaracaoClasseContext, inicio: Int): List<String> {
    val lista = mutableListOf<String>()
    var i = inicio
    while (i < ctx.childCount && ctx.getChild(i).text != "{") {
        val t = ctx.getChild(i).text
        if (t != "," && t != "implementa") lista.add(t)
        i++
    }
    return lista
}

fun validarInterfacesOuErro(
    classeCtx: DeclaracaoClasseContext,
    nomeClasse: String,
    interfaces: List<String>,
    global: Ambiente
) {
    interfaces.forEach { nome ->
        global.obterInterface(nome)
            ?: throw SemanticError("Interface '$nome' não encontrada")
        if (!verificarImplementacaoInterface(classeCtx, nome, global)) {
            throw SemanticError("A classe '$nomeClasse' não implementa todos os métodos da interface '$nome'")
        }
    }
}

fun verificarImplementacaoInterface(
    classeContext: DeclaracaoClasseContext,
    nomeInterface: String,
    global: Ambiente
): Boolean {
    val iface = global.obterInterface(nomeInterface) ?: return false

    val fornecidos = buildSet<String> {
        addAll(classeContext.declaracaoFuncao().map { it.ID().text })
        global.getSuperClasse(classeContext)
            ?.let { global.obterClasse(it) }
            ?.let { addAll(it.declaracaoFuncao().map { f -> f.ID().text }) }
    }
    return iface.assinaturaMetodo().all { it.ID().text in fornecidos }
}

fun visitClasses(tree: ProgramaContext, global: Ambiente) {
    tree.declaracao().forEach { decl ->
        decl.declaracaoClasse()?.let {
            val nome = it.ID(0).text
            global.definirClasse(nome, it)
        }
    }
}

fun visitInterfaces(tree: ProgramaContext, global: Ambiente) {
    tree.declaracao().forEach { decl ->
        decl.declaracaoInterface()?.let {
            val nome = it.ID().text
            global.definirInterface(nome, it)
        }
    }
}

fun comoObjetoOuErro(v: Valor): Valor.Objeto =
    v as? Valor.Objeto
        ?: throw SemanticError("Nao e possivel acessar propriedades de um nao-objeto: $v")
