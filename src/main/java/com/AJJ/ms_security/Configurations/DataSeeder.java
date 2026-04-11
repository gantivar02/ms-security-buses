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
import java.util.Objects;

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
            upsertRole(data[0], data[1]);
        });
    }

    private void seedPermissions() {
        List<Permission> permissions = new ArrayList<>();

        addCrudPermissions(permissions, "users", "usuarios", "/api/users/**");
        addCrudPermissions(permissions, "roles", "roles", "/api/roles/**");
        addCrudPermissions(permissions, "permissions", "permisos", "/api/permissions/**");
        addCrudPermissions(permissions, "profiles", "perfiles", "/api/profiles/**");
        addCrudPermissions(permissions, "sessions", "sesiones", "/api/sessions/**");
        addCrudPermissions(permissions, "role_permission", "permisos por rol", "/api/role-permission/**");
        addCrudPermissions(permissions, "user_role", "roles de usuario", "/api/user-role/**");

        // Módulos de la HU HU-ENTR-1-001
        addCrudPermissions(permissions, "buses", "buses", "/api/buses/**");
        addCrudPermissions(permissions, "routes", "rutas", "/api/routes/**");
        addCrudPermissions(permissions, "schedules", "programaciones", "/api/schedules/**");
        addCrudPermissions(permissions, "reports", "reportes", "/api/reports/**");
        addCrudPermissions(permissions, "incidents", "incidentes", "/api/incidents/**");
        addCrudPermissions(permissions, "mass_messages", "mensajes masivos", "/api/mass-messages/**");

        for (Permission permission : permissions) {
            upsertPermission(permission);
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
                    findPermission("users_delete"),
                    findPermission("roles_read"),
                    findPermission("roles_create"),
                    findPermission("roles_update"),
                    findPermission("roles_delete"),
                    findPermission("permissions_read"),
                    findPermission("profiles_read"),
                    findPermission("profiles_create"),
                    findPermission("profiles_update"),
                    findPermission("sessions_read"),
                    findPermission("sessions_create"),
                    findPermission("sessions_update"),
                    findPermission("sessions_delete"),
                    findPermission("role_permission_read"),
                    findPermission("role_permission_create"),
                    findPermission("role_permission_delete"),
                    findPermission("user_role_read"),
                    findPermission("user_role_create"),
                    findPermission("user_role_delete"),
                    findPermission("buses_read"),
                    findPermission("buses_create"),
                    findPermission("buses_update"),
                    findPermission("buses_delete"),
                    findPermission("routes_read"),
                    findPermission("routes_create"),
                    findPermission("routes_update"),
                    findPermission("routes_delete"),
                    findPermission("schedules_read"),
                    findPermission("schedules_create"),
                    findPermission("schedules_update"),
                    findPermission("schedules_delete"),
                    findPermission("reports_read"),
                    findPermission("incidents_read"),
                    findPermission("incidents_create"),
                    findPermission("incidents_update"),
                    findPermission("incidents_delete"),
                    findPermission("mass_messages_read"),
                    findPermission("mass_messages_create"),
                    findPermission("mass_messages_update"),
                    findPermission("mass_messages_delete")
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
                    findPermission("role_permission_read"),
                    findPermission("buses_read"),
                    findPermission("routes_read"),
                    findPermission("schedules_read"),
                    findPermission("reports_read"),
                    findPermission("incidents_read"),
                    findPermission("incidents_create"),
                    findPermission("incidents_update"),
                    findPermission("mass_messages_read")
            ));
        }

        // Conductor
        if (conductor != null) {
            assignPermissions(conductor, List.of(
                    findPermission("profiles_read"),
                    findPermission("profiles_update"),
                    findPermission("sessions_create"),
                    findPermission("sessions_read"),
                    findPermission("routes_read"),
                    findPermission("schedules_read"),
                    findPermission("incidents_read"),
                    findPermission("incidents_create"),
                    findPermission("incidents_update")
            ));
        }

        // Ciudadano
        if (ciudadano != null) {
            assignPermissions(ciudadano, List.of(
                    findPermission("profiles_read"),
                    findPermission("profiles_update"),
                    findPermission("sessions_create"),
                    findPermission("routes_read"),
                    findPermission("schedules_read"),
                    findPermission("reports_read")
            ));
        }
    }

    private void addCrudPermissions(List<Permission> permissions, String module, String label, String urlPattern) {
        permissions.add(buildPermission(module + "_read", module, "read",
                "Permite consultar " + label, urlPattern, "GET"));
        permissions.add(buildPermission(module + "_create", module, "create",
                "Permite crear " + label, urlPattern, "POST"));
        permissions.add(buildPermission(module + "_update", module, "update",
                "Permite actualizar " + label, urlPattern, "PUT"));
        permissions.add(buildPermission(module + "_delete", module, "delete",
                "Permite eliminar " + label, urlPattern, "DELETE"));
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

    private void upsertRole(String name, String description) {
        Role existing = this.theRoleRepository.findByNameIgnoreCase(name);
        if (existing == null) {
            this.theRoleRepository.save(new Role(name, description));
            System.out.println("[DataSeeder] Rol creado: " + name);
            return;
        }

        boolean changed = false;
        if (!Objects.equals(existing.getName(), name)) {
            existing.setName(name);
            changed = true;
        }
        if (!Objects.equals(existing.getDescription(), description)) {
            existing.setDescription(description);
            changed = true;
        }

        if (changed) {
            this.theRoleRepository.save(existing);
            System.out.println("[DataSeeder] Rol actualizado: " + name);
        }
    }

    private void upsertPermission(Permission expectedPermission) {
        Permission existing = this.thePermissionRepository.findByName(expectedPermission.getName());
        if (existing == null) {
            this.thePermissionRepository.save(expectedPermission);
            System.out.println("[DataSeeder] Permiso creado: " + expectedPermission.getName());
            return;
        }

        boolean changed = false;
        if (!Objects.equals(existing.getModule(), expectedPermission.getModule())) {
            existing.setModule(expectedPermission.getModule());
            changed = true;
        }
        if (!Objects.equals(existing.getAction(), expectedPermission.getAction())) {
            existing.setAction(expectedPermission.getAction());
            changed = true;
        }
        if (!Objects.equals(existing.getDescription(), expectedPermission.getDescription())) {
            existing.setDescription(expectedPermission.getDescription());
            changed = true;
        }
        if (!Objects.equals(existing.getUrl(), expectedPermission.getUrl())) {
            existing.setUrl(expectedPermission.getUrl());
            changed = true;
        }
        if (!Objects.equals(existing.getMethod(), expectedPermission.getMethod())) {
            existing.setMethod(expectedPermission.getMethod());
            changed = true;
        }

        if (changed) {
            this.thePermissionRepository.save(existing);
            System.out.println("[DataSeeder] Permiso actualizado: " + expectedPermission.getName());
        }
    }
}
