package com.batu.transaction_service.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class KeycloakScopeConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        String scopeAsString = jwt.getClaimAsString("scope");
        if (scopeAsString != null) {
            Arrays.stream(scopeAsString.split(" ")).forEach(scope -> {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
                authorities.add(new SimpleGrantedAuthority(scope));
            });
        }

        Object scopeObject = jwt.getClaim("scope");
        if (scopeObject instanceof Collection) {
            Collection<String> scopes = (Collection<String>) scopeObject;
            scopes.forEach(scope -> {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
                authorities.add(new SimpleGrantedAuthority(scope));
            });
        }

        Object scpObject = jwt.getClaim("scp");
        if (scpObject instanceof Collection) {
            Collection<String> scopes = (Collection<String>) scpObject;
            scopes.forEach(scope -> {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
                authorities.add(new SimpleGrantedAuthority(scope));
            });
        }

        return authorities;
    }
}