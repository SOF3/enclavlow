package io.github.sof3.enclavlow

fun <T : Any> newDiGraph(
    nodes: IndexedSet<T>,
): MutableDiGraph<T> {
    val edges: MutableList<MutableList<Edge?>> = MutableList(nodes.size) { MutableList(nodes.size) { null } }
    return MutableDiGraph(nodes, edges)
}

data class Edge(val causes: MutableSet<String>) {
    var copyFlow: Boolean = false
}

infix fun Edge?.graphEquals(other: Any?): Boolean {
    return (this == null) == (other == null)
}

private val findSourceStack = ThreadLocal.withInitial { mutableSetOf<Int>() }

sealed class DiGraph<T : Any>(
    nodes: IndexedSet<T>,
    edges: MutableList<MutableList<Edge?>>,
) {
    open var nodes = nodes
        protected set
    open var edges = edges
        protected set

    override fun equals(other: Any?): Boolean {
        if (other !is DiGraph<*>) return false
        if (nodes != other.nodes) {
            return false
        }
        for (i in 0 until nodes.size) {
            for (j in 0 until nodes.size) {
                if (!(edges[i][j] graphEquals other.edges[i][j])) {
                    return false
                }
            }
        }
        return true
    }

    override fun hashCode() = throw NotImplementedError()

    override fun toString(): String {
        return toGraphviz("DirGraph${System.identityHashCode(this)}", {
            val ret = mutableListOf(
                "label" to it.toString(),
                "color" to ("#" + it.javaClass.hashCode().toString(16).padStart(6, '0').substring(0, 6))
            )
            if (it is ControlNode) ret.add("fillColor" to "lightblue")
            ret
        }, { _, _, edge ->
            listOf("label" to edge.causes.joinToString(", "))
        })
    }

    /**
     * Returns a string in Graphviz DOT format representing this graph
     */
    private inline fun toGraphviz(name: String, nodeAttr: (T) -> List<Pair<String, String>>, edgeAttr: (T, T, Edge) -> List<Pair<String, String>>): String {
        val ret = StringBuilder()
        ret.appendLine("digraph $name {")
        for ((i, node) in nodes.withIndex()) {
            val attrs = nodeAttr(node)
            val attrString = attrs.map { (k, v) -> "$k = \"$v\"" }
            ret.appendLine("\t$i [${attrString.joinToString(",")}];")
        }
        for ((i, iEdges) in edges.withIndex()) {
            for ((j, edge) in iEdges.withIndex()) {
                if (edge != null) {
                    val attrs = edgeAttr(nodes[i], nodes[j], edge)
                    val attrString = attrs.map { (k, v) -> "$k = \"$v\"" }
                    ret.appendLine("\t$i -> $j [${attrString.joinToString(",")}];")
                }
            }
        }
        ret.appendLine("}")
        return ret.toString()
    }

    fun filterNodes(pred: (T) -> Boolean): MutableDiGraph<T> {
        val newOldMap = mutableListOf<Int>()
        for ((i, t) in nodes.withIndex()) {
            if (pred(t)) {
                newOldMap.add(i)
            }
        }

        val newNodes = IndexedSet<T>()
        newNodes.addAll(newOldMap.map { nodes[it] })
        val newEdges = MutableList(newOldMap.size) { i ->
            MutableList(newOldMap.size) { j ->
                edges[newOldMap[i]][newOldMap[j]]
            }
        }
        return MutableDiGraph(newNodes, newEdges)
    }

    fun indexOf(node: T): Int = nodes.find(node) ?: throw IllegalArgumentException("Nonexistent node $node")

    fun flowsFrom(src: T): List<T> = flowsFromIndex(indexOf(src)).map { nodes[it] }
    fun flowsFromEdges(src: T): List<Pair<T, Edge>> {
        val i = indexOf(src)
        return flowsFromIndex(i).map { nodes[it] to edges[i][it]!! }
    }

    private fun flowsFromIndex(i: Int) = edges[i].mapIndexed { j, t -> if (t != null) j else null }.filterNotNull()

    fun flowsTo(dest: T): List<T> = flowsToIndex(indexOf(dest)).map { nodes[it] }
    fun flowsToEdges(dest: T): List<Pair<T, Edge>> {
        val j = indexOf(dest)
        return flowsToIndex(j).map { nodes[it] to edges[it][j]!! }
    }

    inline fun flowsToIndex(j: Int, edgeFilter: (Edge) -> Boolean = { true }) = edges.mapIndexed { i, arr ->
        val edge = arr[j]
        if (edge != null && edgeFilter(edge)) i else null
    }.filterNotNull()

    fun visitAncestors(leaves: Set<T>, visitor: (T) -> Unit) {
        val visited = mutableSetOf<Int>()
        for (leaf in leaves) {
            visitAncestors(leaf, visited, visitor)
        }
    }

    private fun visitAncestors(leaf: T, visited: MutableSet<Int>, visitor: (T) -> Unit) {
        val j = indexOf(leaf)
        for (i in 0 until nodes.size) {
            if (edges[i][j] != null && visited.add(i)) {
                visitor(nodes[i])
                visitAncestors(nodes[i], visited, visitor)
            }
        }
    }

    fun deleteAllSources(dest: T) {
        val j = indexOf(dest)
        for (i in 0 until nodes.size) {
            edges[i][j] = null
        }
    }

    /**
     * Finds the node c such that
     * there exists paths (a, ..., c), (b, ..., c) with all path elements satisfying filter,
     * with the smallest path length sums
     */
    inline fun lca(a: T, b: T, nodeFilter: (T) -> Boolean, edgeFilter: (Edge) -> Boolean): T? {
        println("Finding LCA of $a and $b in $this")
        val index = lcaIndex(indexOf(a), indexOf(b), { nodeFilter(nodes[it]) }, edgeFilter) ?: return null
        return nodes[index]
    }

    inline fun lcaIndex(a: Int, b: Int, nodeFilter: (Int) -> Boolean, edgeFilter: (Edge) -> Boolean): Int? {
        val left = mutableSetOf(a)
        var leftEden = setOf(a)
        val right = mutableSetOf(b)
        var rightEden = setOf(b)

        while (true) {
            val intersect = left intersect right
            for (node in intersect) {
                return node // return any element from intersect
            }

            if (leftEden.isEmpty() && rightEden.isEmpty()) {
                return null
            }

            leftEden = leftEden.flatMap { flowsToIndex(it, edgeFilter) }.filter { nodeFilter(it) && it !in left }.toSet()
            left.addAll(leftEden)
            rightEden = rightEden.flatMap { flowsToIndex(it, edgeFilter) }.filter { nodeFilter(it) && it !in right }.toSet()
            right.addAll(rightEden)
        }
    }
}

class MutableDiGraph<T : Any>(
    nodes: IndexedSet<T>,
    edges: MutableList<MutableList<Edge?>>,
) : DiGraph<T>(nodes, edges), Cloneable {
    override var edges: MutableList<MutableList<Edge?>>
        get() = super.edges
        public set(value) {
            super.edges = value
        }

    fun addNodeIfMissing(node: T) {
        if (nodes.addIfMissing(node)) {
            for (edgeList in edges) {
                edgeList.add(null)
            }
            edges.add(MutableList(nodes.size) { null })
        }
    }

    fun touch(from: T, to: T, cause: String, config: Edge.() -> Unit = {}) {
        val a = nodes.find(from) ?: throw IllegalArgumentException("Nonexistent node $from")
        val b = nodes.find(to) ?: throw IllegalArgumentException("Nonexistent node $to")

        if (a == b) return // self-loops are not allowed

        val edge = edges[a][b] ?: Edge(mutableSetOf())
        edge.causes.add(cause)
        edge.config()
        edges[a][b] = edge
    }

    public override fun clone() = MutableDiGraph(nodes.clone(), MutableList(nodes.size) { edges[it].toMutableList() })

    infix fun copyTo(target: MutableDiGraph<T>) {
        target.nodes = nodes.clone()
        target.edges = MutableList(nodes.size) { edges[it].toMutableList() }
    }

    inline fun merge(other: MutableDiGraph<T>, biMap: (Edge?, Edge?) -> Edge?): MutableDiGraph<T> {
        // L
        val out = clone()

        // L intersect R (overwrites the intersection part in the first clone)
        val intersect = nodes.filter { it in other.nodes }
        for (node1 in intersect) {
            for (node2 in intersect) {
                if (node1 != node2) {
                    out.edges[out.nodes.find(node1)!!][out.nodes.find(node2)!!] = biMap(
                        edges[other.nodes.find(node1)!!][nodes.find(node2)!!],
                        other.edges[other.nodes.find(node1)!!][other.nodes.find(node2)!!],
                    )
                }
            }
        }

        // R minus L
        val rml = other.nodes.filter { it !in nodes }
        for (node in rml) {
            out.addNodeIfMissing(node)
        }
        for (node in rml) {
            val find = out.nodes.find(node)!!
            for ((dest, edge) in other.flowsFromEdges(node)) {
                out.edges[find][out.nodes.find(dest)!!] = edge
            }
        }
        for (node in rml) {
            val find = out.nodes.find(node)!!
            for ((src, edge) in other.flowsToEdges(node)) {
                if (src in rml) continue // duplicate
                out.edges[out.nodes.find(src)!!][find] = edge
            }
        }

        return out
    }
}
