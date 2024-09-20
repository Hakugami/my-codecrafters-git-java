import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.zip.*;

public class Main {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        final String command = args[0];

        switch (command) {
            case "init" -> initGitDirectory();
            case "cat-file" -> catFile(args[2]);
            case "hash-object" -> hashObject(args[2]);
            case "ls-tree" -> showTree(args[2]);
            default -> System.out.println("Unknown command: " + command);
        }
    }


    private static void initGitDirectory() {
        final File root = new File(".git");
        new File(root, "objects").mkdirs();
        new File(root, "refs").mkdirs();
        final File head = new File(root, "HEAD");

        try {
            head.createNewFile();
            Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
            System.out.println("Initialized git directory");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void catFile(String hash) throws IOException {
        final String folderHash = hash.substring(0, 2);
        final String fileHash = hash.substring(2);
        final File blobFile = new File(".git/objects/" + folderHash + "/" + fileHash);

        try (FileInputStream fis = new FileInputStream(blobFile)) {
            Inflater inflater = new Inflater();
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int count;
            while ((count = fis.read(buffer)) != -1) {
                inflater.setInput(buffer, 0, count);
                byte[] result = new byte[1024];
                int resultLength = inflater.inflate(result);
                outputStream.write(result, 0, resultLength);
            }
            inflater.end();
            String content = outputStream.toString();
            String formattedContent = content.substring(content.indexOf("\0") + 1);
            System.out.print(formattedContent);
        } catch (IOException | java.util.zip.DataFormatException e) {
            throw new RuntimeException(e);
        }
    }

    private static void hashObject(String filePath) throws IOException, NoSuchAlgorithmException {
        final byte[] fileType = "blob".getBytes();
        final byte[] space = " ".getBytes();
        final byte[] nullChar = {0};
        final File file = new File(filePath);
        final byte[] contentBytes = Files.readAllBytes(file.toPath());
        final int contentLength = contentBytes.length;

        MessageDigest digest = initializeDigest();
        updateDigest(digest, fileType, space, nullChar, contentBytes, contentLength);
        byte[] hash = digest.digest();

        final String hashString = HexFormat.of().formatHex(hash);
        final String folderHash = hashString.substring(0, 2);
        final String fileHash = hashString.substring(2);
        final File blobFile = new File(".git/objects/" + folderHash + "/" + fileHash);
        blobFile.getParentFile().mkdirs();

        writeCompressedContent(blobFile, fileType, space, nullChar, contentBytes, contentLength);

        System.out.println(hashString);
    }
    private static void showTree(final String arg) throws FileNotFoundException {
        final String folderHash = arg.substring(0, 2);
        final String fileHash = arg.substring(2);
        final File blobFile = new File(".git/objects/" + folderHash + "/" + fileHash);
        final ArrayList<String> tree = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader((new InputStreamReader(new InflaterInputStream(new FileInputStream(blobFile)))))) {
            StringBuilder content = new StringBuilder();
            int character;

            while ((character = bufferedReader.read()) != -1) {
                if (character == 0) {
                    break;
                }
            }

            while ((character = bufferedReader.read()) != -1) {
                content.append((char) character);
            }
            String[] lines = content.toString().split("100644|040000|100755|120000|40000");

            for (String line : lines) {
                String[] parts = line.split("\0");
                String hash = parts[0].trim();
                if(!hash.isBlank()) {
                    tree.add(hash);
                }
            }
            Collections.sort(tree);

            for (String hash : tree) {
                System.out.println(hash);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private static MessageDigest initializeDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-1");
    }

    private static void updateDigest(MessageDigest digest, byte[] fileType, byte[] space, byte[] nullChar, byte[] contentBytes, int contentLength) {
        digest.update(fileType);
        digest.update(space);
        digest.update(String.valueOf(contentLength).getBytes());
        digest.update(nullChar);
        digest.update(contentBytes);
    }

    private static void writeCompressedContent(File blobFile, byte[] fileType, byte[] space, byte[] nullChar, byte[] contentBytes, int contentLength) throws IOException {
        try (OutputStream fos = Files.newOutputStream(blobFile.toPath())) {
            DeflaterOutputStream dos = new DeflaterOutputStream(fos);
            dos.write(fileType);
            dos.write(space);
            dos.write(String.valueOf(contentLength).getBytes());
            dos.write(nullChar);
            dos.write(contentBytes);
            dos.close();
        }
    }
}