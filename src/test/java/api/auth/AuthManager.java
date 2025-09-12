package api.auth;

import api.client.AsyncRestClient;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AuthManager {
    private final Map<String, AuthProvider> authProviders = new HashMap<>();

    public AuthManager() {
        // Register default auth providers
        registerProvider("basic", new BasicAuthProvider());
        registerProvider("bearer", new BearerTokenAuthProvider());
    }

    public void registerProvider(String name, AuthProvider provider) {
        authProviders.put(name, provider);
    }

    public void applyAuthToClient(String authType, Map<String, String> authParams, AsyncRestClient client) {
        AuthProvider provider = authProviders.get(authType);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown auth type: " + authType);
        }

        // Apply auth to client's default headers
        Map<String, String> authHeaders = provider.getAuthHeaders(authParams);
        authHeaders.forEach(client::addHeader);
    }

    public SimpleHttpRequest applyAuthToRequest(String authType, Map<String, String> authParams, SimpleHttpRequest request) {
        AuthProvider provider = authProviders.get(authType);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown auth type: " + authType);
        }

        // Apply auth to specific request
        Map<String, String> authHeaders = provider.getAuthHeaders(authParams);
        authHeaders.forEach(request::addHeader);
        return request;
    }

    public interface AuthProvider {
        Map<String, String> getAuthHeaders(Map<String, String> params);
    }

    public static class BasicAuthProvider implements AuthProvider {
        @Override
        public Map<String, String> getAuthHeaders(Map<String, String> params) {
            String username = params.get("username");
            String password = params.get("password");
            String credentials = username + ":" + password;
            String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes());

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Basic " + base64Credentials);
            return headers;
        }
    }

    public static class BearerTokenAuthProvider implements AuthProvider {
        @Override
        public Map<String, String> getAuthHeaders(Map<String, String> params) {
            String token = params.get("token");

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + token);
            return headers;
        }
    }
}