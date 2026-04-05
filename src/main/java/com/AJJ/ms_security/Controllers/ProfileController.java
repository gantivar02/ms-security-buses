package com.AJJ.ms_security.Controllers;

import com.AJJ.ms_security.Models.Profile;
import com.AJJ.ms_security.Services.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    @Autowired
    private ProfileService theProfileService;

    @GetMapping("")
    public List<Profile> find() {
        return this.theProfileService.find();
    }

    @GetMapping("{id}")
    public Profile findById(@PathVariable String id) {
        return this.theProfileService.findById(id);
    }

    @PostMapping
    public Profile create(@RequestBody Profile newProfile) {
        return this.theProfileService.create(newProfile);
    }

    @PutMapping("{id}")
    public Profile update(@PathVariable String id, @RequestBody Profile newProfile) {
        return this.theProfileService.update(id, newProfile);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable String id) {
        this.theProfileService.delete(id);
    }


}