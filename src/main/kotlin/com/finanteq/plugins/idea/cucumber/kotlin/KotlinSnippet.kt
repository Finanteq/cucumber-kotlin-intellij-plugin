package com.finanteq.plugins.idea.cucumber.kotlin

import cucumber.api.DataTable
import cucumber.runtime.snippets.Snippet

class KotlinSnippet : Snippet {
    override fun tableHint(): String = ""

    override fun escapePattern(pattern: String): String {
        return pattern.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    override fun template(): String {
        return "@{0}(\"{1}\")\n fun {2}({3} {5}) \'{\'\n    // {4}\n throw PendingException()\n\'}\'\n"
    }

    override fun arguments(argumentTypes: MutableList<Class<*>>): String {
        val result = StringBuilder()
        argumentTypes.forEachIndexed { index, clazz ->
            if (index > 0) {
                result.append(", ")
            }
            result.append("arg").append(index).append(": ").append(if (clazz == DataTable::class.java) "List<*>" else clazz.simpleName)
        }
        return result.toString()
    }

    override fun namedGroupStart(): String? = null

    override fun namedGroupEnd(): String? = null
}