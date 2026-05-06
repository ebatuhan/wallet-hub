package com.batu.shared.unit.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.shared.security.KeycloakScopeConverter;

class KeycloakScopeConverterTest {

    private final KeycloakScopeConverter converter = new KeycloakScopeConverter();

    @Test
    void convert_whenScopeClaimIsSpaceSeparatedString_shouldReturnScopeAuthorities() {
        Jwt jwt = jwt(Map.of("scope", "openid profile accounts:read"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorityNames(authorities))
                .containsExactlyInAnyOrder(
                        "SCOPE_openid",
                        "openid",
                        "SCOPE_profile",
                        "profile",
                        "SCOPE_accounts:read",
                        "accounts:read");
    }

    @Test
    void convert_whenScopeClaimContainsExtraSpaces_shouldIgnoreBlankScopes() {
        Jwt jwt = jwt(Map.of("scope", "openid   profile  "));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorityNames(authorities))
                .containsExactlyInAnyOrder("SCOPE_openid", "openid", "SCOPE_profile", "profile");
    }

    @Test
    void convert_whenScpClaimIsCollection_shouldReturnScopeAuthorities() {
        Jwt jwt = jwt(Map.of("scp", List.of("transactions:read", "budgets:write")));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorityNames(authorities))
                .containsExactlyInAnyOrder(
                        "SCOPE_transactions:read",
                        "transactions:read",
                        "SCOPE_budgets:write",
                        "budgets:write");
    }

    @Test
    void convert_whenScopeCollectionsContainBlankValues_shouldIgnoreBlankScopes() {
        Jwt jwt = jwt(Map.of("scp", List.of("transactions:read", " ")));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorityNames(authorities))
                .containsExactlyInAnyOrder("SCOPE_transactions:read", "transactions:read");
    }

    @Test
    void convert_whenJwtHasNoScopeClaims_shouldReturnEmptyAuthorities() {
        Jwt jwt = jwt(Map.of("sub", "11111111-1111-1111-1111-111111111111"));

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
