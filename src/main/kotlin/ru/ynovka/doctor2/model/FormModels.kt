package ru.ynovka.doctor2.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.Serializable
import java.time.format.DateTimeFormatter
import java.util.Locale


@Serializable
data class FormBlank(
    val title: String,
    val sections: List<FormSection>,
    val exportFormBlank: String? = null
)

@Serializable
data class FormSection(
    val title: String,
    val questions: List<FormQuestion>
)

@Serializable
data class FormQuestion(
    val id: Int,
    val text: String,
    val type: String,
    val options: List<FormOption> = emptyList()
)

@Serializable
data class FormOption(
    val text: String,
    val hasAdditionalText: Boolean = false
)

data class FormInfo(
    val filename: String,
    val title: String,
    val createdDate: LocalDateTime
) {
    fun getFormattedDate(): String {
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'г. в' HH:mm", Locale("ru"))
        return createdDate.toJavaLocalDateTime().format(formatter)}
}

data class ResponseFilter(
    val templateTitle: String? = null,
    val dateFrom: LocalDateTime? = null,
    val dateTo: LocalDateTime? = null,
    val page: Int = 0,
    val size: Int = 20
)

data class PagedResponses(
    val content: List<FormResponseInfo>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

@Serializable
data class FormAnswer(
    val questionId: Int,
    val value: String
)

@Serializable
data class FormResponse(
    val templateFilename: String,
    val templateTitle: String,
    val submittedAt: LocalDateTime,
    val answers: List<FormAnswer>
) {
    fun getFormattedDate(): String {
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'г. в' HH:mm", Locale("ru"))
        return submittedAt.toJavaLocalDateTime().format(formatter)}
}

data class FormResponseInfo(
    val answers: List<FormAnswer>,
    val templateFilename: String,
    val templateTitle: String,
    val submittedAt: LocalDateTime,
    val filename: String
) {
    fun getFormattedDate(): String {
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'г. в' HH:mm", Locale("ru"))
        return submittedAt.toJavaLocalDateTime().format(formatter)}
}