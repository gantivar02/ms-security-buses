package com.AJJ.ms_security.Controllers;

import com.AJJ.ms_security.Models.Session;
import com.AJJ.ms_security.Services.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    @Autowired
    private SessionService theSessionService;

    @GetMapping("")
    public List<Session> find() {
        return this.theSessionService.find();
    }

    @GetMapping("{id}")
    public Session findById(@PathVariable String id) {
        return this.theSessionService.findById(id);
    }

    @PostMapping
    public Session create(@RequestBody Session newSession) {
        return this.theSessionService.create(newSession);
    }

    @PutMapping("{id}")
    public Session update(@PathVariable String id, @RequestBody Session newSession) {
        return this.theSessionService.update(id, newSession);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable String id) {
        this.theSessionService.delete(id);
    }

}