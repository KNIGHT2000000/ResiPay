package com.resipay.idempotency.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class PayloadHasher {

    private PayloadHasher() {
    }

    public static String computeHash(String method, String path, String body) {
        String safeMethod = method != null ? method.toUpperCase().trim() : "";
        String safePath = path != null ? path.trim() : "";
        String safeBody = body != null ? body.trim() : "";

        String input = safeMethod + ":" + safePath + ":" + safeBody;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
