package com.AJJ.ms_security.Controllers;

import com.AJJ.ms_security.Models.UserRole;
import com.AJJ.ms_security.Services.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/user-role")
public class UserRoleController {
    @Autowired
    private UserRoleService theUserRoleService;


    @PostMapping("/{userId}/role/{roleId}")
    public ResponseEntity<Map<String, String>> addUserRole(
            @PathVariable String userId,
            @PathVariable String roleId) {

        String result = this.theUserRoleService.addUserRole(userId, roleId);

        switch (result){

            case "SUCCESS":
                return ResponseEntity.ok(
                        Map.of("message","Role assigned successfully"));

            case "USER_NOT_FOUND":
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message","User not found"));

            case "ROLE_NOT_FOUND":
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message","Role not found"));

            case "ROLE_ALREADY_ASSIGNED":
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message","User already has this role"));

            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message","Unexpected error"));
        }
    }
    @PostMapping("/{userId}/roles")
    public ResponseEntity<Map<String,String>> addMultipleRoles(
            @PathVariable String userId,
            @RequestBody List<String> roleIds){

        String result = this.theUserRoleService.addMultipleRoles(userId,roleIds);
        switch (result){

            case "SUCCESS":
                return ResponseEntity.ok(Map.of("message","Roles assignados"));

            case "PARTIAL_SUCCESS":
                return ResponseEntity.ok(Map.of("message","Algunos roles estan duplicados"));

            case "USER_NOT_FOUND":
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message","No se encontró el usuario"));

            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message","Error al asignar el usuario"));
        }
    }
    @DeleteMapping("{userRoleId}")
    public ResponseEntity<Map<String, String>> removeUserRole(
            @PathVariable String userRoleId) {

        boolean response = this.theUserRoleService.removeUserRole(userRoleId);
        if (response) {
            return ResponseEntity.ok(Map.of("message", "Success"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User or Role not found"));
        }
    }
    @GetMapping("/user/{userId}")
    public List<UserRole> getRolesByUser(@PathVariable String userId){
        return this.theUserRoleService.getRolesByUser(userId);
    }


}
