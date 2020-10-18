package io.github.sof3.enclavlow

fun <T : Any> newDirGraph(
    nodes: IndexedSet<T>,
): MutableDirGraph<T> {
    val edges = MutableList(nodes.size) { MutableList(nodes.size) { false } }
    return MutableDirGraph(nodes, edges)
}

typealias Edge = Boolean

private val findSourceStack = ThreadLocal.withInitial { mutableSetOf<Int>() }

sealed class DirGraph<T : Any>(
    nodes: IndexedSet<T>,
    edges: MutableList<MutableList<Edge>>,
) {
    open var nodes = nodes
        protected set
    open var edges = edges
        protected set

    override fun equals(other: Any?): Boolean {
        if (other !is DirGraph<*>) return false
        if (nodes != other.nodes) {
            return false
        }
        for (i in 0 until nodes.size) {
            for (j in 0 until nodes.size) {
                if (edges[i][j] != other.edges[i][j]) {
                    return false
                }
            }
        }
        return true
    }

    override fun hashCode() = throw NotImplementedError()

    override fun toString(): String {
        val stmts = mutableListOf<String>()
        for (i in 0 until nodes.size) {
            for (j in 0 until nodes.size) {
                val e = edges[i][j]
                if (e) stmts.add("${nodes[i]} -> ${nodes[j]}")
            }
        }
        return "DirGraph($nodes; {\n${stmts.joinToString(separator = ",\n\t", prefix = "\t")}\n)"
    }

    fun flowsFrom(node: T): List<T> {
        val i = nodes.find(node) ?: throw IllegalArgumentException("Nonexistent node $node")
        return edges[i].mapIndexed { j, t -> if (t) nodes[j] else null }.filterNotNull()
    }

    fun flowsTo(node: T): List<T> {
        val j = nodes.find(node) ?: throw IllegalArgumentException("Nonexistent node $node")
        return edges.mapIndexed { i, arr -> if (arr[j]) nodes[i] else null }.filterNotNull()
    }

    fun findSources(node: T, canBeSelf: Boolean = true): Set<T> {
        val stack = findSourceStack.get()
        if (node.hashCode() in stack) {
            throw Exception("Infinite recursion detected")
        }
        stack.add(node.hashCode())

        try {
            val inputs = flowsTo(node)
            if (inputs.isEmpty() && canBeSelf) return setOf(node)

            return inputs.flatMap { findSources(it) }.toSet()
        } finally {
            stack.remove(node.hashCode())
        }
    }
}

class MutableDirGraph<T : Any>(
    nodes: IndexedSet<T>,
    edges: MutableList<MutableList<Edge>>,
) : DirGraph<T>(nodes, edges), Cloneable {
    override var edges: MutableList<MutableList<Edge>>
        get() = super.edges
        public set(value) {
            super.edges = value
        }

    fun addNodeIfMissing(node: T) {
        if (nodes.addIfMissing(node)) {
            for (edgeList in edges) {
                edgeList.add(false)
            }
            edges.add(MutableList(nodes.size) { false })
        }
    }

    fun touch(from: T, to: T): MutableDirGraph<T> {
        val a = nodes.find(from) ?: throw IllegalArgumentException("Nonexistent node $from")
        val b = nodes.find(to) ?: throw IllegalArgumentException("Nonexistent node $to")
        edges[a][b] = true
        return this
    }

    public override fun clone() = MutableDirGraph(nodes.clone(), MutableList(nodes.size) { edges[it].toMutableList() })

    fun copyTo(target: MutableDirGraph<T>) {
        target.nodes = nodes.clone()
        target.edges = MutableList(nodes.size) { edges[it].toMutableList() }
    }

    inline fun merge(other: MutableDirGraph<T>, biMap: (Edge, Edge) -> Edge): MutableDirGraph<T> {
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
            for (dest in other.flowsFrom(node)) {
                out.edges[find][out.nodes.find(dest)!!] = true
            }
        }
        for (node in rml) {
            val find = out.nodes.find(node)!!
            for (src in other.flowsTo(node)) {
                if (src in rml) continue // duplicate
                out.edges[out.nodes.find(src)!!][find] = true
            }
        }

        return out
    }
}
