package com.battlelab.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class JwtTokenHelper {

    private static final byte[] SECRET = "f801e024-8ffe-43d1-8102-cdf4a7540c1510000000".getBytes();

    private static final Algorithm algorithm = Algorithm.HMAC256(SECRET);

    public static String create(Map<String, Serializable> claims, int expiredSeconds) {
        Date expiredAt = new Date(System.currentTimeMillis() + expiredSeconds * 1000);
        JWTCreator.Builder builder = JWT.create();
        if (claims != null) {
            claims.forEach((x, y) -> {
                if (y instanceof Boolean) {
                    builder.withClaim(x, (Boolean) y);
                } else if (y instanceof String) {
                    builder.withClaim(x, (String) y);
                } else if (y instanceof Date) {
                    builder.withClaim(x, (Date) y);
                } else if (y instanceof Integer) {
                    builder.withClaim(x, (Integer) y);
                } else if (y instanceof Long) {
                    builder.withClaim(x, (Long) y);
                } else if (y instanceof Double) {
                    builder.withClaim(x, (Double) y);
                } else {
                    throw new RuntimeException("wrong claim type");
                }

            });
        }
        builder.withExpiresAt(expiredAt);
        return builder.sign(algorithm);
    }

    public static DecodedJWT verifyToken(String token) {
        Verification require = JWT.require(algorithm);
        return require.build().verify(token);
    }

}
