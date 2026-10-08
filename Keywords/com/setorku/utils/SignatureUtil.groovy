package com.setorku.utils

import java.security.MessageDigest

public class SignatureUtil {
    
    /**
     * Membentuk hash SHA-1 dari gabungan parameter request payload + secret key.
     */
    public static String generateSHA1(String rawText) {
        MessageDigest md = MessageDigest.getInstance("SHA-1")
        byte[] textBytes = rawText.getBytes("UTF-8")
        md.update(textBytes, 0, textBytes.length)
        byte[] sha1hash = md.digest()
        
        StringBuilder sb = new StringBuilder()
        for (byte b : sha1hash) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
