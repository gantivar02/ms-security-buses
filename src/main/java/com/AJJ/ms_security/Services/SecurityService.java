package com.AJJ.ms_security.Services;

import com.AJJ.ms_security.Models.User;
import com.AJJ.ms_security.Models.UserRole;
import com.AJJ.ms_security.Repositories.UserRepository;
import com.AJJ.ms_security.Repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SecurityService {
    @Autowired
    private UserRepository theUserRepository;
    @Autowired
    private EncryptionService theEncryptionService;
    @Autowired
    private JwtService theJwtService;
    @Autowired
    private UserRoleRepository theUserRoleRepository;

    public String login(User theNewUser) {
        User theActualUser = this.theUserRepository.getUserByEmail(theNewUser.getEmail());

        if (theActualUser != null &&
                theActualUser.getPassword().equals(theEncryptionService.convertSHA256(theNewUser.getPassword()))) {

            // HU-009: incluir los roles del usuario en el token
            List<UserRole> userRoles = this.theUserRoleRepository.getRolesByUser(theActualUser.getId());
            List<String> roleNames = userRoles.stream()
                    .filter(ur -> ur.getRole() != null)
                    .map(ur -> ur.getRole().getName())
                    .collect(Collectors.toList());

            return theJwtService.generateToken(theActualUser, roleNames);
        } else {
            return null;
        }
    }
    /*
    public boolean permissionsValidation(final HttpServletRequest request,
                                         @RequestBody Permission thePermission) {
        boolean success=this.theValidatorsService.validationRolePermission(request,thePermission.getUrl(),thePermission.getMethod());
        return success;
    }
    */

}

