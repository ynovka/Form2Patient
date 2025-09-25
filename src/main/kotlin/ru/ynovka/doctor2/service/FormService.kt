@file:OptIn(ExperimentalTime::class)

package ru.ynovka.doctor2.service

import kotlin.to
import kotlin.run
import java.io.File
import kotlin.jvm.java
import kotlin.time.Clock
import kotlin.text.equals
import kotlin.io.readText
import kotlin.time.Instant
import kotlin.text.ifBlank
import kotlin.text.isBlank
import kotlin.io.writeText
import kotlin.text.matches
import kotlin.text.toRegex
import kotlin.io.extension
import kotlin.text.contains
import kotlin.text.endsWith
import kotlin.collections.map
import kotlin.text.isNotBlank
import kotlinx.datetime.format
import kotlin.text.toIntOrNull
import java.util.logging.Logger
import kotlin.collections.toSet
import ru.ynovka.doctor2.model.*
import kotlinx.datetime.TimeZone
import kotlin.collections.flatMap
import kotlin.collections.forEach
import kotlin.collections.distinct
import kotlinx.datetime.format.char
import kotlin.time.ExperimentalTime
import kotlin.collections.associate
import kotlin.collections.mapNotNull
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlin.collections.sortedByDescending
import org.springframework.stereotype.Service
import kotlinx.serialization.SerializationException


@Service
class FormService {
    private val logger = Logger.getLogger(FormService::class.java.name)
    private val formsDirectory = File("forms")
    private val responsesDirectory = File("responses")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val filenameFormatter = LocalDateTime.Format {
        year()
        monthNumber()
        day()
        char('_')
        hour()
        minute()
        second()
    }

    private val displayFormatter = LocalDateTime.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        day()
        char(' ')
        hour()
        char(':')
        minute()
        char(':')
        second()
    }

    init {
        initializeDirectories()
    }

    private fun initializeDirectories() {
        try {
            if (!formsDirectory.exists()) {
                formsDirectory.mkdirs()
                logger.info("Created forms directory: ${formsDirectory.absolutePath}")
            }
            if (!responsesDirectory.exists()) {
                responsesDirectory.mkdirs()
                logger.info("Created responses directory: ${responsesDirectory.absolutePath}")
            }
        } catch (e: Exception) {
            logger.severe("Failed to initialize directories: ${e.message}")
            throw kotlin.RuntimeException("Не удалось создать необходимые директории", e)
        }
    }

    fun saveForm(form: FormBlank): String {
        validateFormTemplate(form)

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val timestamp = now.format(filenameFormatter)
        val filename = "form_${timestamp}.json"
        val file = File(formsDirectory, filename)

        return try {
            val jsonString = json.encodeToString(form)
            file.writeText(jsonString, Charsets.UTF_8)
            logger.info("Form saved successfully: $filename")
            filename
        } catch (e: SerializationException) {
            logger.severe("Serialization error while saving form: ${e.message}")
            throw kotlin.RuntimeException("Ошибка сериализации формы", e)
        } catch (e: Exception) {
            logger.severe("Error saving form: ${e.message}")
            throw kotlin.RuntimeException("Не удалось сохранить форму", e)
        }
    }

    fun getAllForms(): List<FormInfo> {
        return try {
            formsDirectory.listFiles { file ->
                file.isFile && file.extension.equals("json", ignoreCase = true)
            }?.mapNotNull { file ->
                try {
                    val content = file.readText(Charsets.UTF_8)
                    val form = json.decodeFromString<FormBlank>(content)
                    val createdDate = fileTimestampToLocalDateTime(file.lastModified())
                    FormInfo(
                        filename = file.name,
                        title = form.title.ifBlank { "Без названия" },
                        createdDate = createdDate
                    )
                } catch (e: Exception) {
                    logger.warning("Failed to parse form file ${file.name}: ${e.message}")
                    FormInfo(
                        filename = file.name,
                        title = "Ошибка загрузки",
                        createdDate = fileTimestampToLocalDateTime(file.lastModified())
                    )
                }
            }?.sortedByDescending { it.title }?.reversed() ?: emptyList()
        } catch (e: Exception) {
            logger.severe("Error getting all forms: ${e.message}")
            emptyList()
        }
    }

    fun getForm(filename: String): FormBlank? {
        if (!isValidFilename(filename)) {
            logger.warning("Invalid filename requested: $filename")
            return null
        }

        val file = File(formsDirectory, filename)
        return if (file.exists() && file.isFile) {
            try {
                val content = file.readText(Charsets.UTF_8)
                val form = json.decodeFromString<FormBlank>(content)
                logger.info("Form loaded successfully: $filename")
                form
            } catch (e: SerializationException) {
                logger.severe("Deserialization error while loading form $filename: ${e.message}")
                null
            } catch (e: Exception) {
                logger.severe("Error loading form $filename: ${e.message}")
                null
            }
        } else {
            logger.warning("Form file not found: $filename")
            null
        }
    }

    fun updateForm(filename: String, form: FormBlank): Boolean {
        if (!isValidFilename(filename)) {
            logger.warning("Invalid filename for update: $filename")
            return false
        }

        validateFormTemplate(form)

        val file = File(formsDirectory, filename)
        return if (file.exists() && file.isFile) {
            try {
                val jsonString = json.encodeToString(form)
                file.writeText(jsonString, Charsets.UTF_8)
                logger.info("Form updated successfully: $filename")
                true
            } catch (e: SerializationException) {
                logger.severe("Serialization error while updating form $filename: ${e.message}")
                false
            } catch (e: Exception) {
                logger.severe("Error updating form $filename: ${e.message}")
                false
            }
        } else {
            logger.warning("Form file not found for update: $filename")
            false
        }
    }

    fun deleteForm(filename: String): Boolean {
        if (!isValidFilename(filename)) {
            logger.warning("Invalid filename for deletion: $filename")
            return false
        }

        val file = File(formsDirectory, filename)
        return try {
            if (file.exists() && file.isFile && file.delete()) {
                logger.info("Form deleted successfully: $filename")
                true
            } else {
                logger.warning("Form file not found for deletion: $filename")
                false
            }
        } catch (e: Exception) {
            logger.severe("Error deleting form $filename: ${e.message}")
            false
        }
    }

    fun saveResponse(templateFilename: String, answers: List<FormAnswer>): String? {
        if (!isValidFilename(templateFilename)) {
            logger.warning("Invalid template filename: $templateFilename")
            return null
        }

        val template = getForm(templateFilename) ?: run {
            logger.warning("Template not found for response: $templateFilename")
            return null
        }

        validateAnswers(template, answers)

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val timestamp = now.format(filenameFormatter)
        val responseFilename = "response_${timestamp}.json"
        val file = File(responsesDirectory, responseFilename)

        val response = FormResponse(
            templateFilename = templateFilename,
            templateTitle = template.title,
            submittedAt = now,
            answers = answers
        )

        return try {
            val jsonString = json.encodeToString(response)
            file.writeText(jsonString, Charsets.UTF_8)
            logger.info("Response saved successfully: $responseFilename")
            responseFilename
        } catch (e: SerializationException) {
            logger.severe("Serialization error while saving response: ${e.message}")
            null
        } catch (e: Exception) {
            logger.severe("Error saving response: ${e.message}")
            null
        }
    }

    fun getFilteredResponses(filter: ResponseFilter): PagedResponses {
        return try {
            val allResponses = responsesDirectory.listFiles { file ->
                file.isFile && file.extension.equals("json", ignoreCase = true)
            }?.mapNotNull { file ->
                try {
                    val content = file.readText(Charsets.UTF_8)
                    val response = json.decodeFromString<FormResponse>(content)
                    FormResponseInfo(
                        filename = file.name,
                        templateTitle = response.templateTitle.ifBlank { "Без названия" },
                        submittedAt = response.submittedAt,
                        templateFilename = response.templateFilename,
                        answers = response.answers
                    )
                } catch (e: Exception) {
                    logger.warning("Failed to parse response file ${file.name}: ${e.message}")
                    null
                }
            }?.sortedByDescending { it.submittedAt } ?: emptyList()

            val filtered = allResponses.filter { response ->
                var matches = true

                if (!filter.templateTitle.isNullOrBlank()) {
                    matches = matches && response.templateTitle.contains(filter.templateTitle, ignoreCase = true)
                }

                if (filter.dateFrom != null) {
                    matches = matches && response.submittedAt >= filter.dateFrom
                }

                if (filter.dateTo != null) {
                    matches = matches && response.submittedAt <= filter.dateTo
                }

                matches
            }

            val totalElements = filtered.size.toLong()
            val totalPages = (totalElements + filter.size - 1) / filter.size
            val startIndex = filter.page * filter.size
            val endIndex = minOf(startIndex + filter.size, filtered.size)

            val pagedContent = if (startIndex < filtered.size) {
                filtered.subList(startIndex, endIndex)
            } else {
                emptyList()
            }

            PagedResponses(
                content = pagedContent,
                totalElements = totalElements,
                totalPages = totalPages.toInt(),
                currentPage = filter.page,
                hasNext = filter.page < totalPages - 1,
                hasPrevious = filter.page > 0
            )
        } catch (e: Exception) {
            logger.severe("Error getting filtered responses: ${e.message}")
            PagedResponses(emptyList(), 0, 0, 0, false, false)
        }
    }

    fun getUniqueTemplatesTitles(): List<String> {
        return try {
            responsesDirectory.listFiles { file ->
                file.isFile && file.extension.equals("json", ignoreCase = true)
            }?.mapNotNull { file ->
                try {
                    val content = file.readText(Charsets.UTF_8)
                    val response = json.decodeFromString<FormResponse>(content)
                    response.templateTitle.ifBlank { null }
                } catch (e: Exception) {
                    null
                }
            }?.distinct()?.sorted() ?: emptyList()
        } catch (e: Exception) {
            logger.severe("Error getting unique template titles: ${e.message}")
            emptyList()
        }
    }

    fun getLatestResponses(limit: Int = 5): List<FormResponseInfo> {
        return try {
            responsesDirectory.listFiles { file ->
                file.isFile && file.extension.equals("json", ignoreCase = true)
            }?.mapNotNull { file ->
                try {
                    val content = file.readText(Charsets.UTF_8)
                    val response = json.decodeFromString<FormResponse>(content)
                    FormResponseInfo(
                        filename = file.name,
                        templateTitle = response.templateTitle.ifBlank { "Без названия" },
                        submittedAt = response.submittedAt,
                        templateFilename = response.templateFilename,
                        answers = response.answers
                    )
                } catch (e: Exception) {
                    logger.warning("Failed to parse response file ${file.name}: ${e.message}")
                    FormResponseInfo(
                        filename = file.name,
                        templateTitle = "Ошибка загрузки",
                        submittedAt = fileTimestampToLocalDateTime(file.lastModified()),
                        templateFilename = "",
                        answers = emptyList()
                    )
                }
            }?.sortedByDescending { it.submittedAt }?.take(limit) ?: emptyList()
        } catch (e: Exception) {
            logger.severe("Error getting latest responses: ${e.message}")
            emptyList()
        }
    }

    fun getResponse(filename: String): FormResponse? {
        if (!isValidFilename(filename)) {
            logger.warning("Invalid response filename requested: $filename")
            return null
        }

        val file = File(responsesDirectory, filename)
        return if (file.exists() && file.isFile) {
            try {
                val content = file.readText(Charsets.UTF_8)
                val response = json.decodeFromString<FormResponse>(content)
                logger.info("Response loaded successfully: $filename")
                response
            } catch (e: SerializationException) {
                logger.severe("Deserialization error while loading response $filename: ${e.message}")
                null
            } catch (e: Exception) {
                logger.severe("Error loading response $filename: ${e.message}")
                null
            }
        } else {
            logger.warning("Response file not found: $filename")
            null
        }
    }

    fun deleteResponse(filename: String): Boolean {
        if (!isValidFilename(filename)) {
            logger.warning("Invalid filename for response deletion: $filename")
            return false
        }

        val file = File(responsesDirectory, filename)
        return try {
            if (file.exists() && file.isFile && file.delete()) {
                logger.info("Response deleted successfully: $filename")
                true
            } else {
                logger.warning("Response file not found for deletion: $filename")
                false
            }
        } catch (e: Exception) {
            logger.severe("Error deleting response $filename: ${e.message}")
            false
        }
    }

    fun updateResponse(filename: String, answers: List<FormAnswer>): Boolean {
        if (!isValidFilename(filename)) {
            logger.warning("Invalid response filename for update: $filename")
            return false
        }

        val existingResponse = getResponse(filename) ?: run {
            logger.warning("Response not found for update: $filename")
            return false
        }

        val template = getForm(existingResponse.templateFilename) ?: run {
            logger.warning("Template not found for response update: ${existingResponse.templateFilename}")
            return false
        }

        validateAnswers(template, answers)

        val updatedResponse = existingResponse.copy(
            answers = answers,
        )

        val file = File(responsesDirectory, filename)
        return try {
            val jsonString = json.encodeToString(updatedResponse)
            file.writeText(jsonString, Charsets.UTF_8)
            logger.info("Response updated successfully: $filename")
            true
        } catch (e: SerializationException) {
            logger.severe("Serialization error while updating response $filename: ${e.message}")
            false
        } catch (e: Exception) {
            logger.severe("Error updating response $filename: ${e.message}")
            false
        }
    }

    private fun validateFormTemplate(form: FormBlank) {
        if (form.title.isBlank()) {
            throw kotlin.IllegalArgumentException("Название формы не может быть пустым")
        }

        if (form.sections.isEmpty()) {
            throw kotlin.IllegalArgumentException("Форма должна содержать хотя бы одну секцию")
        }

        form.sections.forEach { section ->
            if (section.title.isBlank()) {
                throw kotlin.IllegalArgumentException("Название секции не может быть пустым")
            }

            if (section.questions.isEmpty()) {
                throw kotlin.IllegalArgumentException("Секция '${section.title}' должна содержать хотя бы один вопрос")
            }

            section.questions.forEach { question ->
                if (question.text.isBlank()) {
                    throw kotlin.IllegalArgumentException("Текст вопроса не может быть пустым")
                }

                if (question.type !in listOf("text", "number", "radio", "checkbox", "textarea")) {
                    throw kotlin.IllegalArgumentException("Недопустимый тип вопроса: ${question.type}")
                }

                if (question.type in listOf("radio", "checkbox") && question.options.isEmpty()) {
                    throw kotlin.IllegalArgumentException("Вопрос типа '${question.type}' должен содержать варианты ответов")
                }
            }
        }

        val questionIds = form.sections.flatMap { it.questions }.map { it.id }
        if (questionIds.size != questionIds.distinct().size) {
            throw kotlin.IllegalArgumentException("ID вопросов должны быть уникальными")
        }
    }

    private fun validateAnswers(template: FormBlank, answers: List<FormAnswer>) {
        val questionIds = template.sections.flatMap { it.questions }.map { it.id }.toSet()

        answers.forEach { answer ->
            if (answer.questionId !in questionIds) {
                throw kotlin.IllegalArgumentException("Ответ содержит недопустимый ID вопроса: ${answer.questionId}")
            }

            if (answer.value.isBlank()) {
                throw kotlin.IllegalArgumentException("Ответ на вопрос ${answer.questionId} не может быть пустым")
            }
        }
    }

    private fun isValidFilename(filename: String): Boolean {
        return filename.isNotBlank() &&
                filename.endsWith(".json", ignoreCase = true) &&
                !filename.contains("..") &&
                filename.matches(Regex("^[a-zA-Z0-9_-]+\\.json$"))
    }

    private fun fileTimestampToLocalDateTime(timestamp: Long): LocalDateTime {
        return try {
            Instant.fromEpochMilliseconds(timestamp)
                .toLocalDateTime(TimeZone.currentSystemDefault())
        } catch (e: Exception) {
            kotlinx.datetime.Instant.fromEpochMilliseconds(0)
                .toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }

    /**
     * Экспортирует заполненную форму как текст по шаблону
     */
    fun exportResponseAsText(responseFilename: String): String? {
        if (!isValidFilename(responseFilename)) {
            logger.warning("Invalid response filename for export: $responseFilename")
            return null
        }

        val response = getResponse(responseFilename) ?: run {
            logger.warning("Response not found for export: $responseFilename")
            return null
        }

        val template = getForm(response.templateFilename) ?: run {
            logger.warning("Template not found for response export: ${response.templateFilename}")
            return null
        }

        val exportTemplate = template.exportFormBlank ?: run {
            logger.warning("Export template not found for form: ${response.templateFilename}")
            return null
        }

        return try {
            val answersMap = response.answers.associate { answer -> answer.questionId to answer.value }

            val regex = "\\{\\{(\\d+)\\}\\}".toRegex()
            val exportText = regex.replace(exportTemplate) { matchResult ->
                val questionId = matchResult.groupValues[1].toIntOrNull()
                if (questionId != null) {
                    answersMap[questionId] ?: "[Нет ответа]"
                } else {
                    matchResult.value
                }
            }

            logger.info("Response exported successfully: $responseFilename")
            exportText
        } catch (e: Exception) {
            logger.severe("Error exporting response $responseFilename: ${e.message}")
            null
        }
    }
}