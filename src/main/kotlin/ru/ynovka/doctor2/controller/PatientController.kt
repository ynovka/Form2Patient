package ru.ynovka.doctor2.controller

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import ru.ynovka.doctor2.service.FormService

@Controller
class PatientController(
    private val formService: FormService,
) {

    @GetMapping("/patient")
    fun patient(
        model: Model
    ): String {
        model.addAttribute("blank_forms", formService.getAllForms())
        return "patient/form_blank_list"
    }

    @GetMapping("/patient/form/blank/{filename}")
    fun patientViewBlankForm(
        @PathVariable filename: String,
        model: Model
    ): String {
        val blankForm = formService.getForm(filename)
        model.addAttribute("blank_form", blankForm)
        model.addAttribute("filename", filename)
        return "patient/form_blank"
    }

    @GetMapping("/patient/thanks")
    fun patientThanks(
        @RequestParam(value = "completedFormLink", required = true) completedFormLink: String,
        model: Model
    ): String {
        model.addAttribute("completed_form_link", completedFormLink)
        return "patient/thanks"
    }

}