package com.AJJ.ms_security.Configurations;

import com.AJJ.ms_security.Models.Permission;
import com.AJJ.ms_security.Models.Role;
import com.AJJ.ms_security.Models.RolePermission;
import com.AJJ.ms_security.Repositories.PermissionRepository;
import com.AJJ.ms_security.Repositories.RolePermissionRepository;
import com.AJJ.ms_security.Repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements ApplicationRunner {

    @Autowired
    private RoleRepository theRoleRepository;

    @Autowired
    private PermissionRepository thePermissionRepository;

    @Autowired
    private RolePermissionRepository theRolePermissionRepository;

    private static final List<String[]> DEFAULT_ROLES = List.of(
            new String[]{"Administrador Sistema", "Acceso total al sistema"},
            new String[]{"Administrador Empresa", "Gestión de empresa de transporte"},
            new String[]{"Supervisor", "Supervisión de operaciones y rutas"},
            new String[]{"Conductor", "Acceso operativo para conductores"},
            new String[]{"Ciudadano", "Consulta de rutas y servicios"}
    );

    @Override
    public void run(ApplicationArguments args) {
        seedRoles();
        seedPermissions();
        assignPermissionsToDefaultRoles();
    }

    private void seedRoles() {
        DEFAULT_ROLES.forEach(data -> {
            String name = data[0];
            if (this.theRoleRepository.findByName(name) == null) {
                this.theRoleRepository.save(new Role(name, data[1]));
                System.out.println("[DataSeeder] Rol creado: " + name);
            }
        });
    }

    private void seedPermissions() {
        List<Permission> permissions = new ArrayList<>();

        // USERS
        permissions.add(buildPermission("users_read", "users", "read", "Permite consultar usuarios", "/users", "GET"));
        permissions.add(buildPermission("users_create", "users", "create", "Permite crear usuarios", "/users", "POST"));
        permissions.add(buildPermission("users_update", "users", "update", "Permite actualizar usuarios", "/users/?", "PUT"));
        permissions.add(buildPermission("users_delete", "users", "delete", "Permite eliminar usuarios", "/users/?", "DELETE"));

        // ROLES
        permissions.add(buildPermission("roles_read", "roles", "read", "Permite consultar roles", "/roles", "GET"));
        permissions.add(buildPermission("roles_create", "roles", "create", "Permite crear roles", "/roles", "POST"));
        permissions.add(buildPermission("roles_update", "roles", "update", "Permite actualizar roles", "/roles/?", "PUT"));
        permissions.add(buildPermission("roles_delete", "roles", "delete", "Permite eliminar roles", "/roles/?", "DELETE"));

        // PERMISSIONS
        permissions.add(buildPermission("permissions_read", "permissions", "read", "Permite consultar permisos", "/permissions", "GET"));
        permissions.add(buildPermission("permissions_create", "permissions", "create", "Permite crear permisos", "/permissions", "POST"));
        permissions.add(buildPermission("permissions_update", "permissions", "update", "Permite actualizar permisos", "/permissions/?", "PUT"));
        permissions.add(buildPermission("permissions_delete", "permissions", "delete", "Permite eliminar permisos", "/permissions/?", "DELETE"));

        // PROFILES
        permissions.add(buildPermission("profiles_read", "profiles", "read", "Permite consultar perfiles", "/profiles", "GET"));
        permissions.add(buildPermission("profiles_create", "profiles", "create", "Permite crear perfiles", "/profiles", "POST"));
        permissions.add(buildPermission("profiles_update", "profiles", "update", "Permite actualizar perfiles", "/profiles/?", "PUT"));
        permissions.add(buildPermission("profiles_delete", "profiles", "delete", "Permite eliminar perfiles", "/profiles/?", "DELETE"));

        // SESSIONS
        permissions.add(buildPermission("sessions_read", "sessions", "read", "Permite consultar sesiones", "/sessions", "GET"));
        permissions.add(buildPermission("sessions_create", "sessions", "create", "Permite crear sesiones", "/sessions", "POST"));
        permissions.add(buildPermission("sessions_update", "sessions", "update", "Permite actualizar sesiones", "/sessions/?", "PUT"));
        permissions.add(buildPermission("sessions_delete", "sessions", "delete", "Permite eliminar sesiones", "/sessions/?", "DELETE"));

        // ROLE-PERMISSION
        permissions.add(buildPermission("role_permission_read", "role_permission", "read", "Permite consultar permisos por rol", "/role-permission/?/permissions", "GET"));
        permissions.add(buildPermission("role_permission_create", "role_permission", "create", "Permite asignar permisos a roles", "/role-permission/?/permission/?", "POST"));
        permissions.add(buildPermission("role_permission_delete", "role_permission", "delete", "Permite quitar permisos de roles", "/role-permission/?", "DELETE"));

        for (Permission permission : permissions) {
            if (this.thePermissionRepository.findByName(permission.getName()) == null) {
                this.thePermissionRepository.save(permission);
                System.out.println("[DataSeeder] Permiso creado: " + permission.getName());
            }
        }
    }

    private void assignPermissionsToDefaultRoles() {
        Role adminSistema = this.theRoleRepository.findByName("Administrador Sistema");
        Role adminEmpresa = this.theRoleRepository.findByName("Administrador Empresa");
        Role supervisor = this.theRoleRepository.findByName("Supervisor");
        Role conductor = this.theRoleRepository.findByName("Conductor");
        Role ciudadano = this.theRoleRepository.findByName("Ciudadano");

        List<Permission> allPermissions = this.thePermissionRepository.findAll();

        // Admin Sistema -> todos
        if (adminSistema != null) {
            assignPermissions(adminSistema, allPermissions);
        }

        // Admin Empresa -> lectura y gestión casi completa
        if (adminEmpresa != null) {
            assignPermissions(adminEmpresa, List.of(
                    findPermission("users_read"),
                    findPermission("users_create"),
                    findPermission("users_update"),
                    findPermission("roles_read"),
                    findPermission("roles_create"),
                    findPermission("roles_update"),
                    findPermission("permissions_read"),
                    findPermission("profiles_read"),
                    findPermission("profiles_update"),
                    findPermission("sessions_read"),
                    findPermission("role_permission_read"),
                    findPermission("role_permission_create")
            ));
        }

        // Supervisor
        if (supervisor != null) {
            assignPermissions(supervisor, List.of(
                    findPermission("users_read"),
                    findPermission("roles_read"),
                    findPermission("permissions_read"),
                    findPermission("profiles_read"),
                    findPermission("profiles_update"),
                    findPermission("sessions_read"),
                    findPermission("role_permission_read")
            ));
        }

        // Conductor
        if (conductor != null) {
            assignPermissions(conductor, List.of(
                    findPermission("profiles_read"),
                    findPermission("profiles_update"),
                    findPermission("sessions_create"),
                    findPermission("sessions_read")
            ));
        }

        // Ciudadano
        if (ciudadano != null) {
            assignPermissions(ciudadano, List.of(
                    findPermission("profiles_read"),
                    findPermission("profiles_update"),
                    findPermission("sessions_create")
            ));
        }
    }

    private Permission buildPermission(String name, String module, String action, String description, String url, String method) {
        return new Permission(name, module, action, description, url, method);
    }

    private Permission findPermission(String name) {
        return this.thePermissionRepository.findByName(name);
    }

    private void assignPermissions(Role role, List<Permission> permissions) {
        for (Permission permission : permissions) {
            if (permission == null) {
                continue;
            }

            RolePermission existing = this.theRolePermissionRepository.getRolePermission(role.getId(), permission.getId());
            if (existing == null) {
                this.theRolePermissionRepository.save(new RolePermission(permission, role));
                System.out.println("[DataSeeder] Permiso " + permission.getName() + " asignado a rol " + role.getName());
            }
        }
    }
}