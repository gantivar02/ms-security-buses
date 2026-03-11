package com.AJJ.ms_security.Services;

import com.AJJ.ms_security.Models.Profile;
import com.AJJ.ms_security.Repositories.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileService {
    @Autowired
    private ProfileRepository theProfileRepository;

    public List<Profile> find(){
        return this.theProfileRepository.findAll();
    }

    public Profile findById(String id){
        Profile theProfile = this.theProfileRepository.findById(id).orElse(null);
        return theProfile;
    }

    public Profile create(Profile newProfile){
        return this.theProfileRepository.save(newProfile);
    }

    public Profile update(String id, Profile newProfile){
        Profile actualProfile = this.theProfileRepository.findById(id).orElse(null);

        if(actualProfile != null){
            actualProfile.setPhone(newProfile.getPhone());
            actualProfile.setPhoto(newProfile.getPhoto());
            this.theProfileRepository.save(actualProfile);
            return actualProfile;
        } else {
            return null;
        }
    }

    public void delete(String id){
        Profile theProfile = this.theProfileRepository.findById(id).orElse(null);
        if(theProfile != null){
            this.theProfileRepository.delete(theProfile);
        }
    }

}

