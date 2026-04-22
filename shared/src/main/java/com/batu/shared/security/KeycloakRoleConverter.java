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
        extractResourceRoles(jwt, authorities);
        extractRealmRoles(jwt, authorities);
        extractGroups(jwt, authorities);
        return authorities;
    }

    private void extractResourceRoles(Jwt jwt, Set<GrantedAuthority> authorities) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null) {
            return;
        }

        resourceAccess.values().forEach(clientAccess -> {
            if (clientAccess instanceof Map<?, ?> clientAccessMap) {
                Object rolesObject = clientAccessMap.get("roles");
                if (rolesObject instanceof Collection<?> roles) {
                    roles.forEach(role -> {
                        String roleName = role.toString();
                        authorities.add(new SimpleGrantedAuthority(roleName));
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
                    });
                }
            }
        });
    }

    private void extractRealmRoles(Jwt jwt, Set<GrantedAuthority> authorities) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return;
        }

        Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");
        realmRoles.forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("REALM_" + role));
            authorities.add(new SimpleGrantedAuthority(role));
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        });
    }

    private void extractGroups(Jwt jwt, Set<GrantedAuthority> authorities) {
        Object groupsObject = jwt.getClaim("groups");
        if (!(groupsObject instanceof Collection<?> groups)) {
            return;
        }

        groups.forEach(group -> {
            String groupName = group.toString();
            groupName = groupName.startsWith("/") ? groupName.substring(1) : groupName;
            authorities.add(new SimpleGrantedAuthority("GROUP_" + groupName));
            authorities.add(new SimpleGrantedAuthority(groupName));
        });
    }
}
