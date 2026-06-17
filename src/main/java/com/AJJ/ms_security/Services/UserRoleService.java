package com.AJJ.ms_security.Services;


import com.AJJ.ms_security.Models.Role;
import com.AJJ.ms_security.Models.User;
import com.AJJ.ms_security.Models.UserRole;
import com.AJJ.ms_security.Repositories.RoleRepository;
import com.AJJ.ms_security.Repositories.UserRepository;
import com.AJJ.ms_security.Repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.List;

@Service
public class UserRoleService {
    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private RoleRepository theRoleRepository;

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    @Autowired
    private NegocioSyncService negocioSyncService;

    @Value("${app.email.service.url}")
    private String emailServiceUrl;

    public String addUserRole(String userId, String roleId){

        User user = this.theUserRepository.findById(userId).orElse(null);

        if(user == null){
            return "USER_NOT_FOUND";
        }

        Role role = this.theRoleRepository.findById(roleId).orElse(null);

        if(role == null){
            return "ROLE_NOT_FOUND";
        }

        UserRole existing =
                this.theUserRoleRepository.findByUser_IdAndRole_Id(userId, roleId);

        if(existing != null){
            return "ROLE_ALREADY_ASSIGNED";
        }

        UserRole theUserRole = new UserRole(user, role);
        this.theUserRoleRepository.save(theUserRole);
        // 🔥 AQUÍ SE ENVÍA EL CORREO
        sendEmail(user.getEmail(), role.getName());

        // Sincroniza con ms-negocio: si el rol asignado es Ciudadano o Conductor,
        // se crea la fila correspondiente en MySQL.
        this.negocioSyncService.syncUser(user);

        return "SUCCESS";
    }


    public boolean removeUserRole(String userRoleId){
        UserRole userRole = this.theUserRoleRepository.findById(userRoleId).orElse(null);
        if (userRole != null) {
            String email = userRole.getUser().getEmail();
            String roleName = userRole.getRole().getName();
            String userId = userRole.getUser().getId();
            this.theUserRoleRepository.delete(userRole);
            sendRoleEmail(email, roleName, false);

            // Sincroniza con ms-negocio. La fila en `ciudadanos`/`conductores` NO
            // se elimina automaticamente (preserva historial); solo se actualizan
            // los roles registrados para la persona en futuras sincronizaciones.
            this.negocioSyncService.syncUserById(userId);

            return true;
        } else {
            return false;
        }
    }
    public List<UserRole> getRolesByUser(String userId){
        return this.theUserRoleRepository.getRolesByUser(userId);
    }
    public String addMultipleRoles(String userId, List<String> roleIds){

        User user = this.theUserRepository.findById(userId).orElse(null);

        if(user == null){
            return "USER_NOT_FOUND";
        }

        boolean duplicateFound = false;

        for(String roleId : roleIds){

            Role role = this.theRoleRepository.findById(roleId).orElse(null);

            if(role == null){
                continue;
            }

            UserRole existing =
                    this.theUserRoleRepository.findByUser_IdAndRole_Id(userId, roleId);

            if(existing != null){
                duplicateFound = true;
            } else {
                UserRole newUserRole = new UserRole(user, role);
                this.theUserRoleRepository.save(newUserRole);
                sendEmail(user.getEmail(), role.getName()); // ← línea agregada
            }
        }

        // Una sola sincronizacion final al terminar de procesar todos los roles
        this.negocioSyncService.syncUser(user);

        if(duplicateFound){
            return "PARTIAL_SUCCESS";
        }

        return "SUCCESS";
    }
    private void sendEmail(String email, String roleName) {
        sendRoleEmail(email, roleName, true);
    }

    private void sendRoleEmail(String email, String roleName, boolean assigned) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String subject = assigned ? "Rol asignado en JAP Team" : "Rol removido en JAP Team";
            String message = assigned
                    ? "Se te ha asignado el rol " + roleName + " en el sistema JAP Team."
                    : "Se te ha removido el rol " + roleName + " del sistema JAP Team.";

            String body = "{"
                    + "\"to\":\"" + email + "\","
                    + "\"subject\":\"" + subject + "\","
                    + "\"message\":\"" + message + "\""
                    + "}";

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(emailServiceUrl + "/send-email", request, String.class);

        } catch (Exception e) {
            System.out.println("Error enviando correo de rol: " + e.getMessage());
        }
    }


}