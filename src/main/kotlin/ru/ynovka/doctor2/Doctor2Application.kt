package ru.ynovka.doctor2

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class Doctor2Application

fun main(args: Array<String>) {
    runApplication<Doctor2Application>(*args)
}
