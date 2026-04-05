package com.AJJ.ms_security.Interceptors;

import com.AJJ.ms_security.Services.ValidatorsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SecurityInterceptor implements HandlerInterceptor {

    @Autowired
    private ValidatorsService validatorService;

    // HU-009: valida token y permisos antes de que la petición llegue al controller
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        int status = this.validatorService.validationRolePermission(
                request, request.getRequestURI(), request.getMethod());

        if (status == 200) {
            return true;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (status == 401) {
            // HU-009: sin token o token inválido → 401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"message\": \"Sesión expirada o inválida\"}");
        } else {
            // HU-009: token válido pero sin permiso → 403
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"message\": \"Acceso denegado\"}");
        }

        return false;
    }
}
