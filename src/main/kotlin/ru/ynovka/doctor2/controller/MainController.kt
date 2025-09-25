package ru.ynovka.doctor2.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping


@Controller
class MainController() {

    @GetMapping("/")
    fun home(): String = "main"

}
