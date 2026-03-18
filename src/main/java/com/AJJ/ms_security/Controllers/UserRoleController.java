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
@RequestMapping("/user-role")
public class UserRoleController {
    @Autowired
    private UserRoleService theUserRoleService;
    @PostMapping("/{userId}/role/{roleId}")

    public ResponseEntity<Map<String, String>> addUserRole(
            @PathVariable String userId,
            @PathVariable String roleId) {

        boolean response = this.theUserRoleService.addUserRole(userId, roleId);
        if (response) {
            return ResponseEntity.ok(Map.of("message", "Role assigned successfully"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "User already has this role or user/role not found"));
        }
    }
    public ResponseEntity<Map<String,String>> addMultipleRoles(
            @PathVariable String userId,
            @RequestBody List<String> roleIds){

        boolean response=this.theUserRoleService.addMultipleRoles(userId,roleIds);

        if(response){
            return ResponseEntity.ok(Map.of("message","Roles assigned"));
        }else{
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message","User not found"));
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
