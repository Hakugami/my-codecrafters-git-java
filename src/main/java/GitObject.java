import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public record GitObject(String type, byte[] content) {

    public String write() throws IOException, NoSuchAlgorithmException {
        byte[] data = (type + " " + content.length + "\0").getBytes();
        byte[] fullContent = new byte[data.length + content.length];
        System.arraycopy(data, 0, fullContent, 0, data.length);
        System.arraycopy(content, 0, fullContent, data.length, content.length);

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(fullContent);
        String hashString = HexFormat.of().formatHex(hash);

        Path objectPath = Path.of(".git", "objects", hashString.substring(0, 2), hashString.substring(2));
        Files.createDirectories(objectPath.getParent());

        try (OutputStream fos = Files.newOutputStream(objectPath);
             DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
            dos.write(fullContent);
        }

        return hashString;
    }

    public static GitObject read(String hash) throws IOException {
        Path objectPath = Path.of(".git", "objects", hash.substring(0, 2), hash.substring(2));

        try (InputStream fis = Files.newInputStream(objectPath);
             InflaterInputStream iis = new InflaterInputStream(fis);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = iis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }

            byte[] content = baos.toByteArray();
            int nullIndex = indexOf(content, (byte) 0);
            String header = new String(content, 0, nullIndex);
            String[] parts = header.split(" ", 2);
            String type = parts[0];
            byte[] objectContent = Arrays.copyOfRange(content, nullIndex + 1, content.length);

            return new GitObject(type, objectContent);
        }
    }

    private static int indexOf(byte[] array, byte target) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == target) {
                return i;
            }
        }
        return -1;
    }
}
