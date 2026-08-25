package com.stuttgarttaxi.taxihub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Arrays;
import java.util.Locale;

/**
 * TR/DE/EN language switching, admin panel and public site alike. Cookie-based
 * (not session-based) so a visitor's language choice survives a browser
 * restart, matching how the public booking wizard already persists its own
 * state to localStorage rather than a session.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Comma-separated origins allowed to call the two public endpoints from a
     * different host. Empty by default: in the normal deployment the React site
     * is built into this app's static folder, so every call is same-origin and
     * no CORS header should be sent at all. Only a separately hosted frontend
     * (the Vercel demo) needs this set.
     */
    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsRaw;

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("taxihub-locale");
        resolver.setDefaultLocale(Locale.forLanguageTag("tr"));
        resolver.setCookieMaxAge(java.time.Duration.ofDays(365));
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins();
        if (origins.length == 0) {
            return;
        }
        // Deliberately narrow: only the two endpoints SecurityConfig already
        // treats as public. Everything else is session-authenticated admin
        // surface and must stay same-origin only.
        registry.addMapping("/api/public/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST");
        registry.addMapping("/api/bookings/calculate-route")
                .allowedOrigins(origins)
                .allowedMethods("POST");
    }

    private String[] allowedOrigins() {
        return Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }
}
