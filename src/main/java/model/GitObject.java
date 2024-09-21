package model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public record GitObject(String type, byte[] content) {

    // Method to write the GitObject to the filesystem and return the SHA-1 hash
    public String write() throws IOException, NoSuchAlgorithmException {
        // Prepare the Git object header (e.g., "commit 150\0")
        String header = type + " " + content.length + "\0";
        byte[] headerBytes = header.getBytes();
        byte[] fullContent = new byte[headerBytes.length + content.length];

        // Concatenate header and content to form the full Git object
        System.arraycopy(headerBytes, 0, fullContent, 0, headerBytes.length);
        System.arraycopy(content, 0, fullContent, headerBytes.length, content.length);

        // Generate SHA-1 hash for the full content (header + content)
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(fullContent);
        String hashString = HexFormat.of().formatHex(hash);  // Convert to 40-char hex string

        // Determine the path for the object based on its hash
        Path objectPath = Path.of(".git", "objects", hashString.substring(0, 2), hashString.substring(2));
        Files.createDirectories(objectPath.getParent());

        // Write the object to the file system using zlib compression
        try (OutputStream fos = Files.newOutputStream(objectPath);
             DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
            dos.write(fullContent);  // Write compressed content to disk
        }

        return hashString;  // Return the 40-character SHA-1 hash
    }

    // Method to read a Git object from the filesystem based on its hash
    public static GitObject read(String hash) throws IOException {
        Path objectPath = Path.of(".git", "objects", hash.substring(0, 2), hash.substring(2));

        // Read the compressed object from disk and inflate it
        try (InputStream fis = Files.newInputStream(objectPath);
             InflaterInputStream iis = new InflaterInputStream(fis);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = iis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }

            // Extract the header and content from the inflated object
            byte[] content = baos.toByteArray();
            int nullIndex = indexOf(content, (byte) 0);
            String header = new String(content, 0, nullIndex);
            String[] parts = header.split(" ", 2);  // Format: "type length"
            String type = parts[0];  // Get the type (commit, tree, etc.)
            byte[] objectContent = new byte[content.length - nullIndex - 1];
            System.arraycopy(content, nullIndex + 1, objectContent, 0, objectContent.length);

            return new GitObject(type, objectContent);
        }
    }

    // Helper method to find the index of a specific byte in an array
    private static int indexOf(byte[] array, byte target) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == target) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return "model.GitObject[type=" + type + ", content=" + new String(content) + ']';
    }
}
