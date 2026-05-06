package com.batu.shared.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakScopeConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        String scopeAsString = jwt.getClaimAsString("scope");
        if (scopeAsString != null) {
            Arrays.stream(scopeAsString.split(" "))
                    .filter(scope -> !scope.isBlank())
                    .forEach(scope -> addScopeAuthorities(authorities, scope));
        }

        Object scopeObject = jwt.getClaim("scope");
        if (scopeObject instanceof Collection<?> scopes) {
            scopes.forEach(scope -> addScopeAuthorities(authorities, scope.toString()));
        }

        Object scpObject = jwt.getClaim("scp");
        if (scpObject instanceof Collection<?> scopes) {
            scopes.forEach(scope -> addScopeAuthorities(authorities, scope.toString()));
        }

        return authorities;
    }

    private void addScopeAuthorities(Set<GrantedAuthority> authorities, String scopeName) {
        authorities.add(new SimpleGrantedAuthority("SCOPE_" + scopeName));
        authorities.add(new SimpleGrantedAuthority(scopeName));
    }
}
