package ru.ynovka.doctor2.service

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import java.util.*
import kotlin.io.inputStream
import kotlin.io.outputStream
import kotlin.io.use

@Service
class PasswordService {

    @Value("\${app.doctor.password:doctor}")
    private var defaultDoctorPassword: String = ""

    @Value("\${app.patient.password:patient}")
    private var defaultPatientPassword: String = ""

    @Value("\${spring.config.additional-location:./}")
    private var configLocation: String = ""

    var envFile: File? = null

    @PostConstruct
    fun init() {
        envFile = File(configLocation, ".env")
        if (!envFile!!.exists()) {
            createDefaultEnvFile()
        }
    }

    fun validateDoctorPassword(password: String): Boolean {
        return password == getCurrentDoctorPassword()
    }

    fun validatePatientPassword(password: String): Boolean {
        return password == getCurrentPatientPassword()
    }

    fun updatePasswords(newDoctorPassword: String, newPatientPassword: String): Boolean {
        return try {
            updateEnvFile(newDoctorPassword, newPatientPassword)
            true
        } catch (e: Exception) {
            println("Error updating passwords: ${e.message}")
            false
        }
    }

    fun getCurrentPasswords(): Pair<String, String> {
        return Pair(getCurrentDoctorPassword(), getCurrentPatientPassword())
    }

    private fun getCurrentDoctorPassword(): String {
        return readPasswordFromEnv("APP_DOCTOR_PASSWORD") ?: defaultDoctorPassword
    }

    private fun getCurrentPatientPassword(): String {
        return readPasswordFromEnv("APP_PATIENT_PASSWORD") ?: defaultPatientPassword
    }

    private fun readPasswordFromEnv(key: String): String? {
        return try {
            if (envFile?.exists() == true) {
                val properties = Properties()
                envFile!!.inputStream().use { inputStream ->
                    properties.load(inputStream)
                }
                properties.getProperty(key)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error reading password from .env: ${e.message}")
            null
        }
    }

    private fun createDefaultEnvFile() {
        try {
            val properties = Properties()
            properties.setProperty("APP_DOCTOR_PASSWORD", defaultDoctorPassword)
            properties.setProperty("APP_PATIENT_PASSWORD", defaultPatientPassword)
            properties.setProperty("APP_DOCTOR_EXTERNAL_ACCESS_RULE", "true")

            envFile!!.outputStream().use { outputStream ->
                properties.store(outputStream, "Default passwords - created automatically")
            }
            println("Created default .env file with default passwords")
        } catch (e: Exception) {
            println("Error creating default .env file: ${e.message}")
        }
    }

    private fun updateEnvFile(newDoctorPassword: String, newPatientPassword: String) {
        val properties = Properties()

        if (envFile?.exists() == true) {
            envFile!!.inputStream().use { inputStream ->
                properties.load(inputStream)
            }
        }

        properties.setProperty("APP_DOCTOR_PASSWORD", newDoctorPassword)
        properties.setProperty("APP_PATIENT_PASSWORD", newPatientPassword)

        envFile!!.outputStream().use { outputStream ->
            properties.store(outputStream, "Updated passwords - ${LocalDateTime.now()}")
        }

        println("Passwords updated in .env file")
    }
}