package com.AJJ.ms_security.Services;

import com.AJJ.ms_security.Models.Profile;
import com.AJJ.ms_security.Models.RegisterRequest;
import com.AJJ.ms_security.Models.Role;
import com.AJJ.ms_security.Models.Session;
import com.AJJ.ms_security.Models.User;
import com.AJJ.ms_security.Models.UserRole;
import com.AJJ.ms_security.Repositories.RoleRepository;
import com.AJJ.ms_security.Repositories.SessionRepository;
import com.AJJ.ms_security.Repositories.ProfileRepository;
import com.AJJ.ms_security.Repositories.UserRepository;
import com.AJJ.ms_security.Repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private ProfileRepository theProfileRepository;

    @Autowired
    private SessionRepository theSessionRepository;

    @Autowired
    private EncryptionService theEncryptionService;

    @Autowired
    private RoleRepository theRoleRepository;

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    @Autowired
    private NegocioSyncService negocioSyncService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.negocio.sync.default-role:Ciudadano}")
    private String defaultRoleOnRegister;

    public List<User> find(){

        return this.theUserRepository.findAll();
    }

    public User findById(String id){
        User theUser=this.theUserRepository.findById(id).orElse(null);
        return theUser;
    }

    public User create(User newUser){
        this.normalizeUserFields(newUser);
        this.validateUniqueEmail(newUser.getEmail(), null);
        newUser.setPassword(theEncryptionService.convertSHA256(newUser.getPassword()));
        return this.theUserRepository.save(newUser);
    }

    public Map<String, String> registerPublicUser(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setLastName(request.getLastName());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(request.getPassword());

        User createdUser = this.create(newUser);

        // Asigna el rol default (Ciudadano) automaticamente al registro publico.
        this.assignDefaultRoleSafe(createdUser);

        // Sincroniza con ms-negocio: crea persona + ciudadano en MySQL.
        this.negocioSyncService.syncUser(createdUser);

        this.sendRegistrationConfirmationEmail(createdUser);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Cuenta creada exitosamente. Te enviamos un correo de confirmación.");
        response.put("email", createdUser.getEmail());
        return response;
    }

    /**
     * Asigna el rol por defecto al user recien registrado. Si el rol no existe
     * en la coleccion (poco probable) o ya esta asignado, se ignora silenciosamente
     * para no romper el flujo de registro.
     */
    private void assignDefaultRoleSafe(User user) {
        try {
            Role role = this.theRoleRepository.findByNameIgnoreCase(defaultRoleOnRegister);
            if (role == null) {
                System.err.println("[UserService] Rol default '"
                        + defaultRoleOnRegister + "' no encontrado. Skip.");
                return;
            }
            UserRole existing = this.theUserRoleRepository
                    .findByUser_IdAndRole_Id(user.getId(), role.getId());
            if (existing == null) {
                this.theUserRoleRepository.save(new UserRole(user, role));
            }
        } catch (Exception e) {
            System.err.println("[UserService] Error asignando rol default: "
                    + e.getMessage());
        }
    }



    public User update(String id, User newUser){
        User actualUser=this.theUserRepository.findById(id).orElse(null);

        if(actualUser!=null){
            if (newUser.getName() != null && !newUser.getName().isBlank()) {
                actualUser.setName(newUser.getName().trim());
            }
            if (newUser.getLastName() != null && !newUser.getLastName().isBlank()) {
                actualUser.setLastName(newUser.getLastName().trim());
            }
            if (newUser.getEmail() != null && !newUser.getEmail().isBlank()) {
                String cleanEmail = newUser.getEmail().toLowerCase().trim();
                this.validateUniqueEmail(cleanEmail, id);
                actualUser.setEmail(cleanEmail);
            }
            if (newUser.getPassword() != null && !newUser.getPassword().isBlank()) {
                actualUser.setPassword(theEncryptionService.convertSHA256(newUser.getPassword()));
            }
            this.theUserRepository.save(actualUser);
            return actualUser;
        }else{
            return null;
        }
    }

    public void delete(String id){
        User theUser=this.theUserRepository.findById(id).orElse(null);
        if (theUser!=null){
            // Sincroniza con ms-negocio antes de borrar de Mongo: marca la
            // persona como inactiva (fecha_borrado = NOW) y conserva el
            // historial de boletos, recargas, citas, grupos, etc. Si el
            // mismo email se vuelve a registrar, syncUser la reactiva.
            this.negocioSyncService.deleteUserByEmail(theUser.getEmail());
            this.theUserRepository.delete(theUser);
        }
    }

    /**
     * Permite asociar un usuario y un perfil . Para que funcione ambos
     * ya deben estar creados.
     *
     * @param userId
     * @param profileId
     * @return
     */
    public boolean addProfile(String userId, String profileId){
        User theUser=this.theUserRepository.findById(userId).orElse(null);
        Profile theProfile= theProfileRepository.findById(profileId).orElse(null);
        if(theUser!=null && theProfile!=null){
            theProfile.setUser(theUser);
            this.theProfileRepository.save(theProfile);
            return true;
        }else{
            return false;
        }
    }

    public boolean removeProfile(String userId, String profileId){
        User theUser=this.theUserRepository.findById(userId).orElse(null);
        Profile theProfile=this.theProfileRepository.findById(profileId).orElse(null);
        if(theUser!=null && theProfile!=null){
            theProfile.setUser(null);
            this.theProfileRepository.save(theProfile);
            return true;
        }else{
            return false;
        }

    }


    /**
     * Permite asociar un usuario y una sesión. Para que funcione ambos
     * ya deben de existir en la base de datos
     * @param userId
     * @param sessionId
     * @return
     */
    public boolean addSession(String userId,String sessionId){
        User theUser=this.theUserRepository.findById(userId).orElse(null);
        Session theSession=this.theSessionRepository.findById(sessionId).orElse(null);
        if(theUser!=null && theSession!=null){
            theSession.setUser(theUser);
            this.theSessionRepository.save(theSession);
            return true;
        }else{
            return false;
        }
    }
    public boolean removeSession(String userId,String sessionId){
        User theUser=this.theUserRepository.findById(userId).orElse(null);
        Session theSession=this.theSessionRepository.findById(sessionId).orElse(null);
        if(theUser!=null && theSession!=null){
            theSession.setUser(null);
            this.theSessionRepository.save(theSession);
            return true;
        }else{
            return false;
        }
    }

    public User findByEmail(String email){
        return this.theUserRepository.getUserByEmail(email);
    }


    public List<User> searchUsers(String text){

        List<User> usersByName =
                this.theUserRepository.findByNameContainingIgnoreCase(text);

        List<User> usersByEmail =
                this.theUserRepository.findByEmailContainingIgnoreCase(text);

        usersByName.addAll(usersByEmail);

        return usersByName;
    }

    private void normalizeUserFields(User user) {
        if (user.getName() != null) {
            user.setName(user.getName().trim());
        }
        if (user.getLastName() != null) {
            user.setLastName(user.getLastName().trim());
        }
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().toLowerCase().trim());
        }
    }

    private void validateUniqueEmail(String email, String currentUserId) {
        User existingUser = this.theUserRepository.getUserByEmail(email);
        if (existingUser != null && (currentUserId == null || !existingUser.getId().equals(currentUserId))) {
            throw new IllegalStateException("El correo electrónico ya se encuentra registrado.");
        }
    }

    private void sendRegistrationConfirmationEmail(User user) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String loginLink = frontendUrl + "/login";
            String body = "{"
                    + "\"to\":\"" + user.getEmail() + "\","
                    + "\"subject\":\"Confirmación de creación de cuenta\","
                    + "\"message\":\"Hola " + user.getName() + " " + user.getLastName()
                    + ", tu cuenta fue creada exitosamente. Ya puedes iniciar sesión en: " + loginLink + "\""
                    + "}";

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject("http://localhost:5000/send-email", request, String.class);
        } catch (Exception e) {
            System.out.println("Error enviando confirmación de registro: " + e.getMessage());
        }
    }



}
