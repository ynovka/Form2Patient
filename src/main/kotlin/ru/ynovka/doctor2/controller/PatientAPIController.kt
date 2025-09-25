package ru.ynovka.doctor2.controller

import org.springframework.context.MessageSource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import ru.ynovka.doctor2.model.FormAnswer
import ru.ynovka.doctor2.service.FormService
import java.util.Locale

@Controller
class PatientAPIController(
    private val formService: FormService,
    private val messageSource: MessageSource
) {
    private fun msg(key: String, locale: Locale): String =
        messageSource.getMessage(key, null, locale)

    @ResponseBody
    @PostMapping("/api/patient/form/blank/{filename}/submit")
    fun patientFormBlankSubmit(
        @PathVariable filename: String,
        @RequestBody answers: List<FormAnswer>,
        locale: Locale
    ): ResponseEntity<Map<String, String>> {
        return try {
            val responseFilename = formService.saveResponse(filename, answers)
            if (responseFilename != null) {
                ResponseEntity.ok(mapOf("success" to "true", "responseId" to responseFilename))
            } else {
                ResponseEntity.badRequest().body(
                    mapOf(
                        "success" to "false",
                        "error" to msg("error.save.answers", locale)
                    )
                )
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to "false",
                    "error" to (e.message ?: msg("error.unknown", locale))
                )
            )
        }
    }

}