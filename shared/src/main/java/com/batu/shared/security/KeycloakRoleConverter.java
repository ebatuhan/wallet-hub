package com.batu.shared.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().forEach(clientAccess -> {
                if (clientAccess instanceof Map<?, ?> clientAccessMap) {
                    Object rolesObject = clientAccessMap.get("roles");
                    if (rolesObject instanceof Collection<?> roles) {
                        roles.forEach(role -> addRoleAuthorities(authorities, role.toString()));
                    }
                }
            });
        }

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");
            realmRoles.forEach(role -> {
                authorities.add(new SimpleGrantedAuthority("REALM_" + role));
                addRoleAuthorities(authorities, role);
            });
        }

        Object groupsObject = jwt.getClaim("groups");
        if (groupsObject instanceof Collection<?> groups) {
            groups.forEach(group -> {
                String groupName = group.toString();
                groupName = groupName.startsWith("/") ? groupName.substring(1) : groupName;
                authorities.add(new SimpleGrantedAuthority("GROUP_" + groupName));
                authorities.add(new SimpleGrantedAuthority(groupName));
            });
        }

        return authorities;
    }

    private void addRoleAuthorities(Set<GrantedAuthority> authorities, String roleName) {
        authorities.add(new SimpleGrantedAuthority(roleName));
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
    }
}
