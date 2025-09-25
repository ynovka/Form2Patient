package ru.ynovka.doctor2.controller

import jakarta.annotation.PostConstruct
import kotlinx.datetime.LocalDateTime
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import ru.ynovka.doctor2.model.PagedResponses
import ru.ynovka.doctor2.model.ResponseFilter
import ru.ynovka.doctor2.service.FormService
import ru.ynovka.doctor2.service.PasswordService
import java.io.File
import java.util.Properties

@Controller
class DoctorController(
    private val formService: FormService,
    private val passwordService: PasswordService
) {

    @GetMapping("/doctor")
    fun doctor(
        model: Model
    ): String {
        val lastCompletedForms = formService.getLatestResponses()
        model.addAttribute("last_completed_forms", lastCompletedForms)
        return "doctor/doctor"
    }

    @GetMapping("/doctor/settings")
    fun doctorSettings(model: Model): String {
        val (adminPassword, clientPassword) = passwordService.getCurrentPasswords()
        model.addAttribute("adminPassword", adminPassword)
        model.addAttribute("clientPassword", clientPassword)
        return "doctor/settings"
    }

    @GetMapping("/doctor/form/blank/create")
    fun doctorFormBlankCreate(): String = "doctor/form_blank"

    @GetMapping("/doctor/form/blank/list")
    fun doctorFormBlankList(
        model: Model
    ): String {
        model.addAttribute("blank_forms", formService.getAllForms())
        return "doctor/form_blank_list"
    }

    @GetMapping("/doctor/form/blank/{filename}")
    fun doctorViewFormBlank(
        @PathVariable filename: String,
        model: Model
    ): String {
        val form = formService.getForm(filename)
        return if (form != null) {
            model.addAttribute("blank_form", form)
            model.addAttribute("filename", filename)
            println("filename: $filename")
            "doctor/form_blank"
        } else {
            "redirect:/doctor/form/blank/list?error=notfound"
        }
    }

    @GetMapping("/doctor/form/completed/list")
    fun doctorFormCompletedList(
        model: Model,
    ): String {
        val uniqueTemplates = formService.getUniqueTemplatesTitles()
        model.addAttribute("uniqueTemplates", uniqueTemplates)
        return "doctor/form_completed_list"
    }
    @GetMapping("/api/doctor/form/completed/search")
    @ResponseBody
    fun searchCompletedForms(
        @RequestParam(required = false) templateTitle: String?,
        @RequestParam(required = false) dateFrom: String?,
        @RequestParam(required = false) dateTo: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PagedResponses {

        val filter = ResponseFilter(
            templateTitle = templateTitle?.takeIf { it.isNotBlank() },
            dateFrom = dateFrom?.let {
                try {
                    LocalDateTime.parse("${it}T00:00:00")
                } catch (e: Exception) {
                    null
                }
            },
            dateTo = dateTo?.let {
                try {
                    LocalDateTime.parse("${it}T23:59:59")
                } catch (e: Exception) {
                    null
                }
            },
            page = page,
            size = size
        )

        return formService.getFilteredResponses(filter)
    }


    @GetMapping("/doctor/form/completed/{filename}")
    fun doctorViewFormCompleted(
        @PathVariable filename: String,
        model: Model
    ): String {
        model.addAttribute("filename", filename)
        val response = formService.getResponse(filename)
        model.addAttribute("completed_form", response)
        val form = formService.getForm(response?.templateFilename ?: "")
        model.addAttribute("blank_form", form)
        return "doctor/form_completed"
    }
}