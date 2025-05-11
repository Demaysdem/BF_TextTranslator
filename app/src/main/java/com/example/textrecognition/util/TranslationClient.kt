package com.example.textrecognition.util

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

object TranslationClient {

    private var translator: Translator = createTranslator("tr")

    private fun createTranslator(targetLanguage: String): Translator {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLanguage)
            .build()
        return Translation.getClient(options)
    }

    fun updateTargetLanguage(targetLanguage: String) {
        translator.close() // Free resources from previous translator
        translator = createTranslator(targetLanguage)
    }

    fun getTranslator(): Translator = translator
}
