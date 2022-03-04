package io.quarkiverse.openapi.generator.providers;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;

/**
 * Provider for Basic Authentication.
 * Username and password should be read by generated configuration properties, which is only known after openapi spec processing
 * during build time.
 */
public class BasicAuthenticationProvider implements ClientRequestFilter {

    private final String name;
    private final AuthProvidersConfig authProvidersConfig;

    public BasicAuthenticationProvider(final String name, final AuthProvidersConfig authProvidersConfig) {
        this.authProvidersConfig = authProvidersConfig;
        this.name = name;
    }

    private String getUsername() {
        return authProvidersConfig.auth().getOrDefault(name + "/username", "");
    }

    private String getPassword() {
        return authProvidersConfig.auth().getOrDefault(name + "/password", "");
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION,
                AuthUtils.basicAuthAccessToken(getUsername(), getPassword()));
    }
}
