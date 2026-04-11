package com.AJJ.ms_security.Services;

import com.AJJ.ms_security.Models.*;
import com.AJJ.ms_security.Repositories.PermissionRepository;
import com.AJJ.ms_security.Repositories.RolePermissionRepository;
import com.AJJ.ms_security.Repositories.UserRepository;
import com.AJJ.ms_security.Repositories.UserRoleRepository;
import com.AJJ.ms_security.Services.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Comparator;
import java.util.List;

@Service
public class ValidatorsService {
    @Autowired
    private JwtService jwtService;

    @Autowired
    private PermissionRepository thePermissionRepository;
    @Autowired
    private UserRepository theUserRepository;
    @Autowired
    private RolePermissionRepository theRolePermissionRepository;

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    private static final String BEARER_PREFIX = "Bearer ";
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // HU-009: retorna código HTTP según el resultado de la validación
    // 200 = OK, 401 = sin token o token inválido, 403 = token válido pero sin permiso
    public int validationRolePermission(HttpServletRequest request,
                                        String url,
                                        String method) {
        User theUser = this.getUser(request);

        // Sin token o token inválido → 401 Sesión expirada o inválida
        if (theUser == null) {
            return 401;
        }

        String normalizedUrl = normalizeUrl(url);
        Permission thePermission = this.resolvePermission(url, normalizedUrl, method);

        List<UserRole> roles = this.theUserRoleRepository.getRolesByUser(theUser.getId());

        for (UserRole actual : roles) {
            Role theRole = actual.getRole();
            if (theRole != null && thePermission != null) {
                RolePermission theRolePermission = this.theRolePermissionRepository
                        .getRolePermission(theRole.getId(), thePermission.getId());
                if (theRolePermission != null) {
                    return 200; // tiene permiso
                }
            }
        }

        // Token válido pero no tiene permiso para esta URL → 403 Acceso denegado
        return 403;
    }
    /***
     * analiza el token y descifra los datos
     * */
    public User getUser(final HttpServletRequest request) {
        User theUser=null;
        String authorizationHeader = request.getHeader("Authorization");
        System.out.println("Header "+authorizationHeader);
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            System.out.println("Bearer Token: " + token);
            User theUserFromToken=jwtService.getUserFromToken(token);
            if(theUserFromToken!=null) {
                theUser= this.theUserRepository.findById(theUserFromToken.getId())
                        .orElse(null);

            }
        }
        return theUser;
    }

    private Permission resolvePermission(String requestUrl, String normalizedUrl, String method) {
        return this.thePermissionRepository.findByMethod(method).stream()
                .filter(permission -> matchesPermissionUrl(permission.getUrl(), requestUrl, normalizedUrl))
                .max(Comparator.comparingInt(permission -> permissionSpecificity(permission.getUrl())))
                .orElse(null);
    }

    private boolean matchesPermissionUrl(String permissionUrl, String requestUrl, String normalizedUrl) {
        if (permissionUrl == null || permissionUrl.isBlank()) {
            return false;
        }

        if (permissionUrl.equals(requestUrl) || permissionUrl.equals(normalizedUrl)) {
            return true;
        }

        if (permissionUrl.endsWith("/**")) {
            String basePath = permissionUrl.substring(0, permissionUrl.length() - 3);
            if (requestUrl.equals(basePath) || normalizedUrl.equals(basePath)) {
                return true;
            }
        }

        if (permissionUrl.contains("*") && pathMatcher.match(permissionUrl, requestUrl)) {
            return true;
        }

        if (permissionUrl.contains("?")) {
            String regex = toSegmentRegex(permissionUrl);
            return requestUrl.matches(regex) || normalizedUrl.matches(regex);
        }

        return false;
    }

    private String normalizeUrl(String url) {
        return url.replaceAll("[0-9a-fA-F]{24}|\\d+", "?");
    }

    private String toSegmentRegex(String permissionUrl) {
        StringBuilder regex = new StringBuilder("^");
        for (char current : permissionUrl.toCharArray()) {
            if (current == '?') {
                regex.append("[^/]+");
            } else {
                if ("\\.[]{}()+-^$|".indexOf(current) >= 0) {
                    regex.append("\\");
                }
                regex.append(current);
            }
        }
        regex.append("$");
        return regex.toString();
    }

    private int permissionSpecificity(String permissionUrl) {
        return permissionUrl.replace("*", "").replace("?", "").length();
    }
}
