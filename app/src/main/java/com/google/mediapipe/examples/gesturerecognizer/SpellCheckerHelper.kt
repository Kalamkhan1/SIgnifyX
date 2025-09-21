package com.tahakalam.signifyx


import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

// SpellCheckerHelper.kt

class SpellCheckerHelper(private val context: Context) {

    private var spellChecker: SpellChecker? = null
    private val TAG = "SpellCheckerHelper"

    fun clear() {
        spellChecker?.clear()
    }

    /**
     * 1) If serialized BKTree exists in filesDir -> load it and wrap in SpellChecker
     * 2) Else -> build from dictionary asset, save BKTree to filesDir, and return SpellChecker
     *
     * function name unchanged: loadSpellCheckerFromAssets(fileName, serializedFile)
     */
    fun loadSpellCheckerFromAssets(fileName: String, serializedFile: String) {
        try {
            val file = File(context.filesDir, serializedFile)
            Log.d(TAG, "Looking for serialized file at: ${file.absolutePath}")

            spellChecker = if (file.exists()) {
                Log.d(TAG, "Serialized file exists, attempting to read it.")
                try {
                    ObjectInputStream(FileInputStream(file)).use { ois ->
                        val obj = ois.readObject()
                        when (obj) {
                            is BKTree -> {
                                Log.d(TAG, "Deserialized BKTree successfully.")
                                obj.setDistanceFunction(::LevenshteinDistance)
                                SpellChecker.fromBKTree(obj)
                            }
                            else -> {
                                // Backwards compatibility: maybe older code wrote a SpellChecker object (not recommended).
                                Log.w(TAG, "Serialized file contained object ${obj?.javaClass?.name}; attempting to extract BKTree.")
                                // Try to extract an internal BKTree from the object via known accessors or reflection.
                                val bkTree = tryExtractBKTree(obj)
                                if (bkTree != null) {
                                    bkTree.setDistanceFunction(::LevenshteinDistance)
                                    SpellChecker.fromBKTree(bkTree)
                                } else {
                                    Log.w(TAG, "Could not extract BKTree from serialized object; rebuilding.")
                                    buildFromAssets(fileName, file)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to deserialize BKTree, rebuilding...", e)
                    buildFromAssets(fileName, file)
                }
            } else {
                Log.d(TAG, "Serialized file not found, building from assets.")
                buildFromAssets(fileName, file)
            }

            Log.d(TAG, "Spell checker loaded with ${spellChecker?.totalWords} words.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load spell checker", e)
            e.printStackTrace()
        }
    }

    /**
     * Build a SpellChecker by reading words from the asset (fileName),
     * create a BKTree, then serialize the BKTree to `file` (atomically).
     */
    private fun buildFromAssets(fileName: String, file: File): SpellChecker {
        val TAG = "SpellCheckerHelper.build"
        val wordsList = mutableListOf<String>()
        try {
            context.assets.open(fileName).bufferedReader().useLines { lines ->
                lines.forEach { wordsList.add(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read dictionary asset: $fileName", e)
            // continue with empty wordsList
        }

        val checker = SpellChecker.buildFromWords(wordsList, ::LevenshteinDistance)

        // serialize only the BKTree (not the SpellChecker or Context)
        val tmpFile = File(file.absolutePath + ".tmp")
        try {
            ObjectOutputStream(FileOutputStream(tmpFile)).use { oos ->
                val tree = checker.exportBKTree()
                oos.writeObject(tree)
                oos.flush()
            }
            // move temp to final (atomic-ish)
            if (tmpFile.renameTo(file)) {
                Log.d(TAG, "Wrote serialized BKTree to ${file.absolutePath}. exists=${file.exists()}, size=${file.length()} bytes")
            } else {
                // fallback: try copy
                tmpFile.copyTo(file, overwrite = true)
                tmpFile.delete()
                Log.d(TAG, "Wrote serialized BKTree (via copy) to ${file.absolutePath}. exists=${file.exists()}, size=${file.length()} bytes")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize BKTree", e)
            try { tmpFile.delete() } catch (_: Exception) {}
        }

        Log.d(TAG, "Spell checker built from assets with ${checker.totalWords} words.")
        return checker
    }

    /**
     * Try to extract a BKTree from an arbitrary deserialized object.
     * This attempts:
     *  1) If obj has a method named 'exportBKTree' -> invoke it
     *  2) Else try to find a field named 'bkTree' via reflection
     * Returns null if extraction fails.
     */
    private fun tryExtractBKTree(obj: Any?): BKTree? {
        if (obj == null) return null
        try {
            // try method exportBKTree()
            val m = obj::class.java.methods.firstOrNull { it.name == "exportBKTree" && it.parameterCount == 0 }
            if (m != null) {
                val tree = m.invoke(obj)
                if (tree is BKTree) return tree
            }
        } catch (e: Exception) {
            Log.w(TAG, "exportBKTree invocation failed", e)
        }
        try {
            // try field 'bkTree'
            val f = obj::class.java.declaredFields.firstOrNull { it.name == "bkTree" }
            if (f != null) {
                f.isAccessible = true
                val tree = f.get(obj)
                if (tree is BKTree) return tree
            }
        } catch (e: Exception) {
            Log.w(TAG, "bkTree field extraction failed", e)
        }
        return null
    }

    fun isSpellCheckerInitialized(): Boolean = spellChecker != null

    fun suggestWord(misspellWord: String): List<String> {
        if (spellChecker == null) return listOf("Spell checker not initialized")
        val result = spellChecker!!.suggest(misspellWord)
        return result.ifEmpty { listOf("Not a Word") }
    }

    fun totalWords(): Int = spellChecker?.totalWords ?: 0
}