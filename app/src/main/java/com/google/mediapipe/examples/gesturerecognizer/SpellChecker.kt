package com.tahakalam.signifyx

/**
 *
 * @author Mostafa Asgari
 * @since 4/13/17
 */
// SpellChecker.kt

/**
 * SpellChecker is a thin wrapper around BKTree. It is NOT serialized.
 * Use SpellCheckerHelper to persist / load BKTree.
 */
class SpellChecker private constructor(private val bkTree: BKTree) {

    companion object {
        fun buildFromWords(words: Collection<String>, distanceFunc: (String, String) -> Int = ::LevenshteinDistance): SpellChecker {
            val tree = BKTree(distanceFunc)
            words.forEach { tree.add(it) }
            return SpellChecker(tree)
        }

        fun fromBKTree(tree: BKTree, distanceFunc: (String, String) -> Int = ::LevenshteinDistance): SpellChecker {
            tree.setDistanceFunction(distanceFunc)
            return SpellChecker(tree)
        }
    }

    fun suggest(word: String): List<String> = bkTree.getSpellSuggestion(word)

    val totalWords: Int get() = bkTree.totalWords

    fun clear() {
        bkTree.clear()
    }

    /** internal helper used by SpellCheckerHelper to serialize the BKTree */
    internal fun exportBKTree(): BKTree = bkTree
}
