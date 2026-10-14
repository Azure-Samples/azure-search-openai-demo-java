package com.microsoft.openai.samples.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AppAuthConfigurationProperties(
    /** Whether or not login elements are enabled on the UI */
    boolean useLogin,
    /** Whether or not access control is required to access documents with access control lists */
    boolean requireAccessControl,
    /** Whether or not the user can access the app without login */
    boolean enableUnauthenticatedAccess,
    /** MSAL configuration */
    MsalConfig msalConfig,
    /** Login request configuration */
    LoginRequest loginRequest,
    /** Token request configuration */
    TokenRequest tokenRequest
) {
    public record MsalConfig(
        Auth auth,
        Cache cache
    ) {
        public record Auth(
            /** Client app id used for login */
            String clientId,
            /** Directory to use for login https://learn.microsoft.com/entra/identity-platform/msal-client-application-configuration#authority */
            String authority,
            /** Points to window.location.origin. You must register this URI on Azure Portal/App Registration. */
            String redirectUri,
            /** Indicates the page to navigate after logout. */
            String postLogoutRedirectUri,
            /** If "true", will navigate back to the original request location before processing the auth code response. */
            boolean navigateToLoginRequestUrl
        ) {}
        public record Cache(
            /** Configures cache location. "sessionStorage" is more secure, but "localStorage" gives you SSO between tabs. */
            String cacheLocation,
            /** Set this to "true" if you are having issues on IE11 or Edge */
            boolean storeAuthStateInCookie
        ) {}
    }
    public record LoginRequest(
        /** Scopes you add here will be prompted for user consent during sign-in. */
        java.util.List<String> scopes
        // Uncomment the following line to cause a consent dialog to appear on every login
        // String prompt
    ) {}
    public record TokenRequest(
        java.util.List<String> scopes
    ) {}
}
