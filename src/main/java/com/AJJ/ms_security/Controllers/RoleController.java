package com.AJJ.ms_security.Controllers;


import com.AJJ.ms_security.Models.Role;
import com.AJJ.ms_security.Services.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService theRoleService;

    @GetMapping("")
    public List<Role> find() {
        return this.theRoleService.find();
    }

    @GetMapping("{id}")
    public Role findById(@PathVariable String id) {
        return this.theRoleService.findById(id);
    }

    @PostMapping
    public Role create(@RequestBody Role newRole) {
        return this.theRoleService.create(newRole);
    }

    @PutMapping("{id}")
    public Role update(@PathVariable String id, @RequestBody Role newRole) {
        return this.theRoleService.update(id, newRole);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        boolean result = this.theRoleService.delete(id);
        if (result) {
            return ResponseEntity.ok(Map.of("message", "Role deleted successfully"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Role not found or has users assigned"));
        }
    }

}