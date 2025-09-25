package ru.ynovka.doctor2

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken


@Component
class PasswordAuthFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val uri = request.requestURI

        when {
            uri.startsWith("/doctor") -> {
                if (!isAuthenticated("DOCTOR", request)) {
                    redirectToLogin(request, response, "doctor")
                    return
                }
            }
            uri.startsWith("/patient") -> {
                if (!isAuthenticated("PATIENT", request)) {
                    redirectToLogin(request, response, "patient")
                    return
                }
            }
            uri == "/" -> {
                clearAuthentication(request)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun isAuthenticated(
        role: String,
        request: HttpServletRequest
    ): Boolean {
        val session = request.session
        val isAuth = session.getAttribute("authenticated_$role") as? Boolean ?: false

        if (isAuth) {
            val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))
            val authentication = UsernamePasswordAuthenticationToken(role, null, authorities)
            SecurityContextHolder.getContext().authentication = authentication
        }

        return isAuth
    }

    private fun clearAuthentication(
        request: HttpServletRequest
    ) {
        val session = request.session
        session.removeAttribute("authenticated_DOCTOR")
        session.removeAttribute("authenticated_PATIENT")
        SecurityContextHolder.clearContext()
    }

    private fun redirectToLogin(
        request: HttpServletRequest,
        response: HttpServletResponse,
        type: String
    ) {
        val returnUrl = request.requestURI
        response.sendRedirect("/login/$type?returnUrl=$returnUrl")
    }
}