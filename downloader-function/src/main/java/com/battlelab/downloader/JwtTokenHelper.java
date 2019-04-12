package com.battlelab.downloader;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;

import java.util.Date;
import java.util.Map;

public class JwtTokenHelper {

    private static final byte[] SECRET = "f801e024-8ffe-43d1-8102-cdf4a7540c1510000000".getBytes();

    private static final Algorithm algorithm = Algorithm.HMAC256(SECRET);

    public static String create(Map<String, String> claims, int expiredSeconds) {
        Date expiredAt = new Date(System.currentTimeMillis() + expiredSeconds * 1000);
        JWTCreator.Builder builder = JWT.create();
        if (claims != null) {
            claims.forEach((x, y) -> {
                builder.withClaim(x, y);
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
