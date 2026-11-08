package com.salesbot.shared.security;

/** Names shared by whoever issues dashboard tokens (identity) and whoever validates them (security config). */
public final class TokenClaims {

    public static final String ISSUER = "salesbot";

    private TokenClaims() {
    }
}
