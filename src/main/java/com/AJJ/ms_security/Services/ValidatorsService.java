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

        url = url.replaceAll("[0-9a-fA-F]{24}|\\d+", "?");
        Permission thePermission = this.thePermissionRepository.getPermission(url, method);

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
}
