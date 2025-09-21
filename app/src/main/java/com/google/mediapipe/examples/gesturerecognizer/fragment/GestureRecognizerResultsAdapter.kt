/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tahakalam.signifyx.fragment

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tahakalam.signifyx.SpellCheckerHelper


import com.tahakalam.signifyx.databinding.ItemGestureRecognizerResultBinding
import com.google.mediapipe.tasks.components.containers.Category
import java.util.Locale

import kotlin.math.min

class GestureRecognizerResultsAdapter(private var spellCheckerHelper: SpellCheckerHelper?):
    RecyclerView.Adapter<GestureRecognizerResultsAdapter.ViewHolder>() {
    companion object {
        private const val NO_VALUE = "--"
    }

    private var adapterCategories: MutableList<Category?> = mutableListOf()
    private var adapterSize: Int = 0

    fun updateSpellCheckerHelper(newSpellCheckerHelper: SpellCheckerHelper?) {
        if (newSpellCheckerHelper != null) {
            this.spellCheckerHelper = newSpellCheckerHelper
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateResults(categories: List<Category>?) {
        adapterCategories = MutableList(adapterSize) { null }
        if (categories != null) {
            val sortedCategories = categories.sortedByDescending { it.score() }
            val min = min(sortedCategories.size, adapterCategories.size)
            for (i in 0 until min) {
                adapterCategories[i] = sortedCategories[i]
            }
            adapterCategories.sortedBy { it?.index() }
            notifyDataSetChanged()
        }
    }

    fun updateAdapterSize(size: Int) {
        adapterSize = size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = ItemGestureRecognizerResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, spellCheckerHelper = spellCheckerHelper)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        adapterCategories[position].let { category ->
            holder.bind(category?.categoryName(), category?.score())
            Log.d("TAG: onBindViewHolder", "onBindViewHolder: " + category?.categoryName())

        }
    }

    override fun getItemCount(): Int = adapterCategories.size

    inner class ViewHolder(
        private val binding: ItemGestureRecognizerResultBinding,
        private val skipFrameTime: Long = 500L,
        private var currentFrameTime: Long? = 0L,
        var spellCheckerHelper:SpellCheckerHelper?= null
    ) :
        RecyclerView.ViewHolder(binding.root) {

        private var hadLetters:Int =1
        private fun removeConsecutiveDuplicates(text: String): String {
            if (text.length <= 1) return text // Handles both empty and single-character cases

            val result = StringBuilder(text.length)
            var previousChar = text[0]

            result.append(previousChar)

            for (i in 1 until text.length) {
                val currentChar = text[i]
                if (currentChar != previousChar) {
                    result.append(currentChar)
                    previousChar = currentChar
                }
            }

            return result.toString()
        }
        private fun getSuggestedWord(misspellWord: String): List<String> {
            return if (spellCheckerHelper?.isSpellCheckerInitialized() == true) {
                spellCheckerHelper!!.suggestWord(misspellWord)

            } else {
                listOf("Spell checker not initialized")
            }
        }
        fun bind(label: String?, score: Float?) {


            with(binding) {
                // Check if we need to skip frame based on skipFrameTime
                if (currentFrameTime != null && currentFrameTime!! <= skipFrameTime) {
                    currentFrameTime = currentFrameTime!! + 100L
                    return
                }

                // Reset current frame time
                Log.d("ViewHolder", "$currentFrameTime")
                currentFrameTime = 0L

                // Process and update label
                val processedLabel = label ?: NO_VALUE
                val updatedText = if (processedLabel != NO_VALUE) {
                    val concatenatedText = tvLabel.text.toString() + processedLabel
                    hadLetters = 0
                    removeConsecutiveDuplicates(concatenatedText)
                } else if(hadLetters == 0){
                    hadLetters = 1
                    "**${getSuggestedWord(tvLabel.text.toString())?.get(0)}**"
                } else {
                    NO_VALUE
                }



                tvLabel.text = updatedText

                // Format and set score
                tvScore.text = score?.let {
                    String.format(Locale.US, "%.2f", it)
                } ?: NO_VALUE
            }




        }
        
    }
}
