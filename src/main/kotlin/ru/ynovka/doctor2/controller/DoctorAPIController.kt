package ru.ynovka.doctor2.controller

import org.springframework.context.MessageSource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import ru.ynovka.doctor2.model.FormAnswer
import ru.ynovka.doctor2.model.FormBlank
import ru.ynovka.doctor2.service.FormService
import ru.ynovka.doctor2.service.PasswordService
import java.util.*

@Controller
class DoctorAPIController(
    private val formService: FormService,
    private val passwordService: PasswordService,
    private val messageSource: MessageSource
) {
    private fun msg(key: String, locale: Locale): String =
        messageSource.getMessage(key, null, locale)

    @ResponseBody
    @PostMapping("/api/doctor/form/blank/new")
    fun doctorAPISaveFormBlank(
        @RequestBody form: FormBlank,
        locale: Locale
    ): ResponseEntity<Map<String, String>> {
        return try {
            val filename = formService.saveForm(form)
            ResponseEntity.ok(mapOf("success" to "true", "filename" to filename))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to "false",
                    "error" to msg("error.unknown", locale)
                )
            )
        }
    }

    @ResponseBody
    @PutMapping("/api/doctor/form/blank/{filename}")
    fun doctorAPIUpdateFormBlank(
        @PathVariable filename: String,
        @RequestBody form: FormBlank,
        locale: Locale
    ): ResponseEntity<Map<String, String>> {
        return if (formService.updateForm(filename, form)) {
            ResponseEntity.ok(mapOf("success" to "true"))
        } else {
            ResponseEntity.badRequest().body(
                mapOf("success" to "false", "error" to msg("error.update.form", locale))
            )
        }
    }

    @ResponseBody
    @DeleteMapping("/api/doctor/form/blank/{filename}")
    fun doctorAPIDeleteFormBlank(
        @PathVariable filename: String,
        locale: Locale
    ): ResponseEntity<Map<String, String>> {
        return if (formService.deleteForm(filename)) {
            ResponseEntity.ok(mapOf("success" to "true"))
        } else {
            ResponseEntity.badRequest().body(
                mapOf("success" to "false", "error" to msg("error.delete.form", locale))
            )
        }
    }

    @ResponseBody
    @PutMapping("/api/doctor/form/completed/{filename}")
    fun doctorAPIUpdateFormCompleted(
        @PathVariable filename: String,
        @RequestBody answers: List<FormAnswer>,
        locale: Locale
    ): ResponseEntity<Map<String, Any>> {
        return try {
            formService.updateResponse(filename, answers)
            ResponseEntity.ok(mapOf("success" to true))
        } catch (e: Exception) {
            ResponseEntity.badRequest()
                .body(mapOf("error" to msg("error.unknown", locale)))
        }
    }

    @ResponseBody
    @DeleteMapping("/api/doctor/form/completed/{filename}")
    fun doctorAPIDeleteFormCompleted(
        @PathVariable filename: String,
        locale: Locale
    ): ResponseEntity<Map<String, String>> {
        return if (formService.deleteResponse(filename)) {
            ResponseEntity.ok(mapOf("success" to "true"))
        } else {
            ResponseEntity.badRequest().body(
                mapOf("success" to "false", "error" to msg("error.delete.answer", locale))
            )
        }
    }

    @PostMapping("/api/doctor/settings/passwords")
    @ResponseBody
    fun updatePasswords(
        @RequestParam adminPassword: String,
        @RequestParam clientPassword: String,
        locale: Locale
    ): ResponseEntity<Map<String, String>> {
        return if (passwordService.updatePasswords(adminPassword, clientPassword)) {
            ResponseEntity.ok(mapOf("success" to "true", "message" to msg("password.update.success", locale)))
        } else {
            ResponseEntity.badRequest().body(
                mapOf("success" to "false", "error" to msg("password.update.fail", locale))
            )
        }
    }
}
