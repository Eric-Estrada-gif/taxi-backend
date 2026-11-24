package io.agora.media;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Utils {
    public static final int VERSION_LENGTH = 3;

    public static String base64Encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] base64Decode(String data) {
        return Base64.getDecoder().decode(data);
    }

    public static int getTimestamp() {
        return (int) (new Date().getTime() / 1000);
    }

    public static int randomInt() {
        return new SecureRandom().nextInt();
    }

    public static boolean isUUID(String uuid) {
        return uuid != null && uuid.length() == 32 && uuid.matches("\\p{XDigit}+");
    }

    public static byte[] compress(byte[] data) {
        Deflater deflater = new Deflater();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        try {
            deflater.setInput(data);
            deflater.finish();
            byte[] buf = new byte[data.length];
            while (!deflater.finished()) {
                int n = deflater.deflate(buf);
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            return data;
        } finally {
            deflater.end();
        }
    }

    public static byte[] decompress(byte[] data) {
        Inflater inflater = new Inflater();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        try {
            inflater.setInput(data);
            byte[] buf = new byte[8192];
            int len;
            while ((len = inflater.inflate(buf)) > 0) {
                bos.write(buf, 0, len);
            }
        } catch (Exception ignored) {
        } finally {
            inflater.end();
        }
        return bos.toByteArray();
    }
}
