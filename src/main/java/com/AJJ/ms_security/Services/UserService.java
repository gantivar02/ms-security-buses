package com.AJJ.ms_security.Services;

import com.AJJ.ms_security.Models.Profile;
import com.AJJ.ms_security.Models.Session;
import com.AJJ.ms_security.Models.User;
import com.AJJ.ms_security.Repositories.SessionRepository;
import com.AJJ.ms_security.Repositories.ProfileRepository;
import com.AJJ.ms_security.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<User> find(){

        return this.theUserRepository.findAll();
    }

    public User findById(String id){
        User theUser=this.theUserRepository.findById(id).orElse(null);
        return theUser;
    }

    public User create(User newUser){
        //Antes de crear un nuevo usuario validar que no esté duplicado.
        newUser.setPassword(theEncryptionService.convertSHA256(newUser.getPassword()));
        return this.theUserRepository.save(newUser);
    }

    public User update(String id, User newUser){
        User actualUser=this.theUserRepository.findById(id).orElse(null);

        if(actualUser!=null){
            actualUser.setName(newUser.getName());
            actualUser.setEmail(newUser.getEmail());
            actualUser.setPassword(theEncryptionService.convertSHA256(newUser.getPassword()));
            this.theUserRepository.save(actualUser);
            return actualUser;
        }else{
            return null;
        }
    }

    public void delete(String id){
        User theUser=this.theUserRepository.findById(id).orElse(null);
        if (theUser!=null){
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
<<<<<<< HEAD
    public User findByEmail(String email){
        return this.theUserRepository.getUserByEmail(email);
    }
=======

    public List<User> searchUsers(String text){

        List<User> usersByName =
                this.theUserRepository.findByNameContainingIgnoreCase(text);

        List<User> usersByEmail =
                this.theUserRepository.findByEmailContainingIgnoreCase(text);

        usersByName.addAll(usersByEmail);

        return usersByName;
    }


>>>>>>> feature/HU-ENTR-1-002
}
