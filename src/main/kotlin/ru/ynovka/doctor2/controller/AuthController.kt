package ru.ynovka.doctor2.controller

import jakarta.annotation.PostConstruct
import kotlin.to
import kotlin.text.uppercase
import org.springframework.ui.Model
import org.springframework.http.ResponseEntity
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.MessageSource
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import ru.ynovka.doctor2.service.PasswordService
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Locale
import java.util.Properties


@Controller
class AuthController(
    private val passwordService: PasswordService,
    private val messageSource: MessageSource
) {
    private var externalAccess = true
    private fun msg(key: String, locale: Locale): String =
        messageSource.getMessage(key, null, locale)

    @GetMapping("/login/{type}")
    fun loginPage(
        @RequestParam(value = "returnUrl", required = false) returnUrl: String?,
        @PathVariable type: String,
        model: Model,
        locale: Locale
    ): String {
        if (type !in listOf("doctor", "patient")) {
            return "redirect:/"
        }

        model.addAttribute("type", type)
        model.addAttribute("returnUrl", returnUrl ?: "/$type")
        model.addAttribute("title", if (type == "doctor") msg("title.doctor", locale) else msg("patient.workplace.title", locale))
        return "login"
    }


    @PostMapping("/api/login/{type}")
    @ResponseBody
    fun login(
        @PathVariable type: String,
        @RequestParam password: String,
        request: HttpServletRequest,
        locale: Locale
    ): ResponseEntity<Map<String, Any>> {
        val ip = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.remoteAddr

        val isValid = when (type) {
            "doctor" -> passwordService.validateDoctorPassword(password)
            "patient" -> passwordService.validatePatientPassword(password)
            else -> false
        }

        return if (isValid) {
            if (type == "doctor" && !isLocalAddress(ip) && !externalAccess) {
                return ResponseEntity.status(403).body(mapOf(
                    "success" to false,
                    "error" to msg("auth.doctor.403", locale)
                ))
            }
            val session = request.session
            session.setAttribute("authenticated_${type.uppercase()}", true)

            ResponseEntity.ok(
                mapOf(
                    "success" to true
                )
            )
        } else {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "error" to msg("incorrect.password", locale)
                )
            )
        }
    }

    fun isLocalAddress(ip: String): Boolean {
        val clientAddress = InetAddress.getByName(ip)

        if (clientAddress.isLoopbackAddress || clientAddress.isAnyLocalAddress) return true

        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (iface in interfaces) {
            for (inetAddress in iface.inetAddresses) {
                if (clientAddress == inetAddress) {
                    return true
                }
            }
        }
        return false
    }


    @GetMapping("/logout")
    fun logout(request: HttpServletRequest): String {
        val session = request.session
        session.invalidate()
        return "redirect:/"
    }

    @PostConstruct
    private fun readDoctorExternalAccessRuleFromEnv() {
        val rule = try {
            if (passwordService.envFile?.exists() == true) {
                val properties = Properties()
                passwordService.envFile!!.inputStream().use { inputStream ->
                    properties.load(inputStream)
                }
                properties.getProperty("APP_DOCTOR_EXTERNAL_ACCESS_RULE")
            } else {
                "false"
            }
        } catch (e: Exception) {
            println("Error reading rule from .env: ${e.message}")
            "false"
        }
        if (rule != "true") externalAccess = false
    }
}