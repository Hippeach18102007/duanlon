package com.retailmanagement.config;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VnPayConfig {
    public static String vnp_PayUrl    = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static String vnp_ReturnUrl;
    public static String vnp_TmnCode;
    public static String vnp_HashSecret;

    static {
        String returnUrl = System.getenv("VNP_RETURN_URL");
        vnp_ReturnUrl = (returnUrl != null && !returnUrl.isEmpty())
                ? returnUrl
                : "http://localhost:5173/payment-result";

        String tmnCode = System.getenv("VNP_TMN_CODE");
        vnp_TmnCode = (tmnCode != null && !tmnCode.isEmpty())
                ? tmnCode
                : "";

        String hashSecret = System.getenv("VNP_HASH_SECRET");
        vnp_HashSecret = (hashSecret != null && !hashSecret.isEmpty())
                ? hashSecret
                : "";
    }

    /**
     * Tạo Payment URL theo đúng spec VNPAY v2.1.0.
     * hashData = key=URLEncode(value)&key=URLEncode(value)...   (key không encode)
     * query    = URLEncode(key)=URLEncode(value)&...
     */
    public static String buildPaymentUrl(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query    = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName  = itr.next();
            String fieldValue = params.get(fieldName);

            if (fieldValue != null && fieldValue.length() > 0) {
                String encodedValue = encodeValue(fieldValue);
                String encodedKey   = encodeValue(fieldName);

                hashData.append(fieldName).append('=').append(encodedValue);
                query.append(encodedKey).append('=').append(encodedValue);

                if (itr.hasNext()) {
                    hashData.append('&');
                    query.append('&');
                }
            }
        }

        String queryStr    = query.toString();
        String hashDataStr = hashData.toString();
        String secureHash  = hmacSHA512(vnp_HashSecret, hashDataStr);

        System.out.println("\n========== [VNPAY DEBUG LOG] ==========");
        System.out.println("TmnCode    : " + vnp_TmnCode);
        System.out.println("HashSecret : " + vnp_HashSecret);
        System.out.println("HashData   :\n" + hashDataStr);
        System.out.println("SecureHash : " + secureHash + "  (length=" + secureHash.length() + ")");
        System.out.println("=========================================\n");

        return vnp_PayUrl + "?" + queryStr + "&vnp_SecureHash=" + secureHash;
    }

    private static String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * HMAC-SHA512 — trả về lowercase hex, đúng 128 ký tự.
     * FIX BUG CŨ: dùng %02x (lowercase) thay vì %02X để đảm bảo leading zero đúng.
     */
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) throw new NullPointerException();
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            hmac512.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(128);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff)); // lowercase, luôn đủ 2 ký tự/byte
            }
            return sb.toString(); // 128 ký tự lowercase
        } catch (Exception ex) {
            System.out.println("❌ LỖI HMACSHA512: " + ex.getMessage());
            return "";
        }
    }

    /**
     * Dùng cho IPN / Return URL.
     * VNPAY gửi vnp_SecureHash lowercase → so sánh thẳng, không toUpperCase.
     */
    public static String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName  = itr.next();
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                sb.append(fieldName).append('=').append(encodeValue(fieldValue));
            }
            if (itr.hasNext()) sb.append('&');
        }
        return hmacSHA512(vnp_HashSecret, sb.toString());
    }
}