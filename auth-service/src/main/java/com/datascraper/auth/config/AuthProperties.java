package com.datascraper.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AuthProperties {

    private final Jwt jwt = new Jwt();
    private final Google google = new Google();
    private final Auth auth = new Auth();

    public Jwt getJwt() {
        return jwt;
    }

    public Google getGoogle() {
        return google;
    }

    public Auth getAuth() {
        return auth;
    }

    public static class Jwt {
        private String secret;
        private String issuer = "lead-intelligence-auth";
        private long accessTokenTtlMinutes = 15;
        private long refreshTokenTtlDays = 14;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public long getAccessTokenTtlMinutes() {
            return accessTokenTtlMinutes;
        }

        public void setAccessTokenTtlMinutes(long accessTokenTtlMinutes) {
            this.accessTokenTtlMinutes = accessTokenTtlMinutes;
        }

        public long getRefreshTokenTtlDays() {
            return refreshTokenTtlDays;
        }

        public void setRefreshTokenTtlDays(long refreshTokenTtlDays) {
            this.refreshTokenTtlDays = refreshTokenTtlDays;
        }
    }

    public static class Google {
        private List<String> clientIds = new ArrayList<>();
        private String userinfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";
        private String tokeninfoUrl = "https://oauth2.googleapis.com/tokeninfo";

        public List<String> getClientIds() {
            return clientIds;
        }

        public void setClientIds(List<String> clientIds) {
            this.clientIds = clientIds;
        }

        public String getUserinfoUrl() {
            return userinfoUrl;
        }

        public void setUserinfoUrl(String userinfoUrl) {
            this.userinfoUrl = userinfoUrl;
        }

        public String getTokeninfoUrl() {
            return tokeninfoUrl;
        }

        public void setTokeninfoUrl(String tokeninfoUrl) {
            this.tokeninfoUrl = tokeninfoUrl;
        }
    }

    public static class Auth {
        private boolean devLoginEnabled = false;

        public boolean isDevLoginEnabled() {
            return devLoginEnabled;
        }

        public void setDevLoginEnabled(boolean devLoginEnabled) {
            this.devLoginEnabled = devLoginEnabled;
        }
    }
}
