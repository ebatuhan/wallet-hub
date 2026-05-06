package com.batu.shared.unit.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.shared.security.KeycloakRoleConverter;

class KeycloakRoleConverterTest {

    private final KeycloakRoleConverter converter = new KeycloakRoleConverter();

    @Test
    void convert_whenJwtContainsRealmAndClientRoles_shouldReturnAllRoleAuthorities() {
        Jwt jwt = jwt(Map.of(
                "realm_access", Map.of("roles", List.of("admin")),
                "resource_access", Map.of(
                        "wallet-api", Map.of("roles", List.of("accounts:read", "budgets:write")))));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorityNames(authorities))
                .containsExactlyInAnyOrder(
                        "REALM_admin",
                        "admin",
                        "ROLE_admin",
                        "accounts:read",
                        "ROLE_accounts:read",
                        "budgets:write",
                        "ROLE_budgets:write");
    }

    @Test
    void convert_whenJwtContainsGroups_shouldReturnGroupAuthorities() {
        Jwt jwt = jwt(Map.of("groups", List.of("/finance", "support")));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorityNames(authorities))
                .containsExactlyInAnyOrder("GROUP_finance", "finance", "GROUP_support", "support");
    }

    @Test
    void convert_whenJwtHasNoRoleClaims_shouldReturnEmptyAuthorities() {
        Jwt jwt = jwt(Map.of("sub", "11111111-1111-1111-1111-111111111111"));

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void convert_whenRoleClaimsHaveUnexpectedShapes_shouldIgnoreMalformedValues() {
        Jwt jwt = jwt(Map.of(
                "realm_access", Map.of("roles", "admin"),
                "resource_access", Map.of("wallet-api", "not-a-map"),
                "groups", "not-a-list"));

        assertThat(converter.convert(jwt)).isEmpty();
    }

    private Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.parse("2026-05-06T10:15:30Z"))
                .expiresAt(Instant.parse("2026-05-06T11:15:30Z"))
                .claims(existingClaims -> existingClaims.putAll(claims))
                .build();
    }

    private List<String> authorityNames(Collection<GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }
}
