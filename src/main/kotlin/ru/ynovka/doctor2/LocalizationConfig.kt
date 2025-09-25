package ru.ynovka.doctor2

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import java.util.Locale

@Configuration
class LocalizationConfig {

    @Bean
    fun messageSource(): MessageSource {
        val ms = ReloadableResourceBundleMessageSource()
        ms.setBasenames("file:messages", "classpath:messages")
        ms.setDefaultEncoding("UTF-8")
        ms.setCacheSeconds(10)
        ms.setFallbackToSystemLocale(false)
        return ms
    }

    @Bean
    fun localeResolver(): LocaleResolver {
        val resolver = AcceptHeaderLocaleResolver()
        resolver.setDefaultLocale(Locale.forLanguageTag("ru"))
        resolver.setSupportedLocales(listOf(
            Locale.forLanguageTag("ru"),
            Locale.forLanguageTag("de")
        ))
        return resolver
    }


}