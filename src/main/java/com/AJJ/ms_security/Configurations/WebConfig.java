package com.AJJ.ms_security.Configurations;

import com.AJJ.ms_security.Interceptors.SecurityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private SecurityInterceptor securityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {


        // HU-009: proteger todas las rutas excepto las públicas
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/security/login",
                        "/security/forgot-password",   // HU-013: solicitud pública
                        "/security/reset-password"     // HU-013: confirmación con token
                );


    }
}