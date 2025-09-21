package com.tahakalam.signifyx
import java.io.Serializable
import java.util.*

/**
 *
 * @author Mostafa Asgari
 * @since 4/13/17
 */

class Node(val word: String) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
    val children: HashMap<Int, Node> = hashMapOf()
}
