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
        permissions.add(buildPermission("users_read", "users", "read", "Permite consultar usuarios", "/api/users", "GET"));
        permissions.add(buildPermission("users_create", "users", "create", "Permite crear usuarios", "/api/users", "POST"));
        permissions.add(buildPermission("users_update", "users", "update", "Permite actualizar usuarios", "/api/users/?", "PUT"));
        permissions.add(buildPermission("users_delete", "users", "delete", "Permite eliminar usuarios", "/api/users/?", "DELETE"));

        // ROLES
        permissions.add(buildPermission("roles_read", "roles", "read", "Permite consultar roles", "/api/roles", "GET"));
        permissions.add(buildPermission("roles_create", "roles", "create", "Permite crear roles", "/api/roles", "POST"));
        permissions.add(buildPermission("roles_update", "roles", "update", "Permite actualizar roles", "/api/roles/?", "PUT"));
        permissions.add(buildPermission("roles_delete", "roles", "delete", "Permite eliminar roles", "/api/roles/?", "DELETE"));

        // PERMISSIONS
        permissions.add(buildPermission("permissions_read", "permissions", "read", "Permite consultar permisos", "/api/permissions", "GET"));
        permissions.add(buildPermission("permissions_create", "permissions", "create", "Permite crear permisos", "/api/permissions", "POST"));
        permissions.add(buildPermission("permissions_update", "permissions", "update", "Permite actualizar permisos", "/api/permissions/?", "PUT"));
        permissions.add(buildPermission("permissions_delete", "permissions", "delete", "Permite eliminar permisos", "/api/permissions/?", "DELETE"));

        // PROFILES
        permissions.add(buildPermission("profiles_read", "profiles", "read", "Permite consultar perfiles", "/api/profiles", "GET"));
        permissions.add(buildPermission("profiles_create", "profiles", "create", "Permite crear perfiles", "/api/profiles", "POST"));
        permissions.add(buildPermission("profiles_update", "profiles", "update", "Permite actualizar perfiles", "/api/profiles/?", "PUT"));
        permissions.add(buildPermission("profiles_delete", "profiles", "delete", "Permite eliminar perfiles", "/api/profiles/?", "DELETE"));

        // SESSIONS
        permissions.add(buildPermission("sessions_read", "sessions", "read", "Permite consultar sesiones", "/api/sessions", "GET"));
        permissions.add(buildPermission("sessions_create", "sessions", "create", "Permite crear sesiones", "/api/sessions", "POST"));
        permissions.add(buildPermission("sessions_update", "sessions", "update", "Permite actualizar sesiones", "/api/sessions/?", "PUT"));
        permissions.add(buildPermission("sessions_delete", "sessions", "delete", "Permite eliminar sesiones", "/api/sessions/?", "DELETE"));

        // ROLE-PERMISSION
        permissions.add(buildPermission("role_permission_read", "role_permission", "read", "Permite consultar permisos por rol", "/api/role-permission/role/?", "GET"));
        permissions.add(buildPermission("role_permission_create", "role_permission", "create", "Permite asignar permisos a roles", "/api/role-permission/?/permission/?", "POST"));
        permissions.add(buildPermission("role_permission_delete", "role_permission", "delete", "Permite quitar permisos de roles", "/api/role-permission/?", "DELETE"));
        //User-Role
        permissions.add(buildPermission("user_role_read", "user_role", "read", "Permite consultar roles de usuario", "/api/user-role/user/?", "GET"));
        permissions.add(buildPermission("user_role_create", "user_role", "create", "Permite asignar roles a usuarios", "/api/user-role/?/role/?", "POST"));
        permissions.add(buildPermission("user_role_delete", "user_role", "delete", "Permite quitar roles de usuarios", "/api/user-role/?", "DELETE"));

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
                    findPermission("role_permission_create"),
                    findPermission("role_permission_delete"),
                    findPermission("user_role_read"),
                    findPermission("user_role_create"),
                    findPermission("user_role_delete")

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