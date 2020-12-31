package io.github.sof3.enclavlow.util

class SparseGraph<N : Any, E : Edge<E, N>>(private val edgeFactory: () -> E) {
    val nodes: IndexedSet<N> = IndexedSet()
    val forwardEdges: MutableMap<Int, MutableMap<Int, E>> = mutableMapOf()
    private val backwardEdges: MutableMap<Int, MutableMap<Int, E>> = mutableMapOf()

    private fun indexOf(node: N): Int = nodes.find(node)
        ?: throw IllegalArgumentException("Nonexistent node " +
            "${node.toString().replace("\n", "[LF]")} in $this")

    fun addNodeIfMissing(node: N) {
        nodes.addIfMissing(node)
    }

    fun touch(src: N, dest: N, configure: E.() -> Unit) {
        val srcIndex = indexOf(src)
        val destIndex = indexOf(dest)
        val edge = forwardEdges.getOrPut(srcIndex, ::mutableMapOf)
            .getOrPut(destIndex, edgeFactory)
        backwardEdges.getOrPut(destIndex, ::mutableMapOf)
            .putIfAbsent(srcIndex, edge)
        configure(edge)
    }

    fun flowsFromEdges(src: N): List<Pair<N, E>> =
        forwardEdges[indexOf(src)]
            ?.map { (j, e) -> nodes[j] to e }
            ?: emptyList()

    /**
     * Returns a string in Graphviz DOT format representing this graph
     */
    inline fun toGraphviz(
        name: String,
        graphAttr: List<Pair<String, String>> = emptyList(),
        nodeAttr: (N) -> List<Pair<String, String>> = {
            val ret = mutableListOf(
                "label" to it.toString(),
                "color" to randomColor(it.javaClass.name)
            )
            ret
        },
        crossinline edgeAttr: (N, N, E) -> Iterable<Pair<String, String>> = { from, to, edge ->
            edge.getGraphvizAttributes(from, to)
        },
    ): String {
        return writeGraphviz(name, graphAttr, nodeAttr, edgeAttr, nodes, ::forEachEdgeIndex)
    }

    inline fun forEachEdgeIndex(fn: (Int, Int, E) -> Unit) {
        for ((i, iEdges) in forwardEdges.entries) {
            for ((j, edge) in iEdges) {
                fn(i, j, edge)
            }
        }
    }
}
