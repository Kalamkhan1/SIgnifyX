package com.tahakalam.signifyx


/**
 *
 * @author Mostafa Asgari
 * @since 4/13/17
 */
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.*

class BKTree(@Transient private var distanceFunc: (String, String) -> Int) : Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    @Transient
    private var distanceFunction: (String, String) -> Int = distanceFunc

    private var root: Node? = null

    fun add(word: String) {
        if (root == null) root = Node(word)
        else add(root!!, word)
    }

    private fun add(node: Node, word: String) {
        val distance = distanceFunction(node.word, word)
        if (distance == 0) return
        node.children[distance]?.let { add(it, word) } ?: run { node.children[distance] = Node(word) }
    }

    fun getSpellSuggestion(word: String): List<String> {
        root ?: return listOf()
        val normalizedWord = if (word == word.uppercase(Locale.ROOT)) word.lowercase(Locale.ROOT) else word
        return getSpellSuggestion(root!!, normalizedWord)
    }

    private fun getSpellSuggestion(node: Node, word: String, tolerance: Int = 1): List<String> {
        val result = mutableListOf<String>()
        val distance = distanceFunction(word, node.word)

        if (distance == 0) return listOf(node.word)
        if (distance <= tolerance) result.add(node.word)

        val start = maxOf(1, distance - tolerance)
        val end = distance + tolerance

        for (d in start..end) {
            node.children[d]?.let {
                result.addAll(getSpellSuggestion(it, word, tolerance))
            }
        }
        return result
    }

    fun clear() {
        root = null
    }

    val totalWords: Int get() = countWords(root)

    private fun countWords(node: Node?): Int {
        if (node == null) return 0
        return 1 + node.children.values.sumOf { countWords(it) }
    }

    fun setDistanceFunction(func: (String, String) -> Int) {
        distanceFunction = func
    }

    // Only write the node structure (defaultWriteObject)
    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.defaultWriteObject()
    }

    // Restore a safe default distance function after deserialization.
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(inp: ObjectInputStream) {
        inp.defaultReadObject()
        // default function after deserialization; caller can set a custom one with setDistanceFunction(...)
        distanceFunction = ::LevenshteinDistance
    }
}
