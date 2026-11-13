package br.senac.footfanatics.seguranca;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Funções de apoio para hash e segredos (não é componente de arquitetura). Nunca registram a entrada. */
public final class Hashes {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Hashes() {
    }

    public static String sha256(String valor) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    /** Comparação em tempo constante. */
    public static boolean iguais(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] bytesAleatorios(int tamanho) {
        byte[] b = new byte[tamanho];
        ALEATORIO.nextBytes(b);
        return b;
    }

    public static String segredoAleatorio() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytesAleatorios(32));
    }

    public static String hmac(byte[] chave, String mensagem) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chave, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(mensagem.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 indisponível", e);
        }
    }
}
