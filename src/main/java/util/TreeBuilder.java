package util;

import model.GitObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

public class TreeBuilder {
    public static byte[] buildTree(Path dir) throws IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (Stream<Path> paths = Files.list(dir)) {
            List<Path> sortedPaths = paths
                    .filter(path -> !path.getFileName().toString().equals(".git"))
                    .sorted(Path::compareTo)
                    .toList();

            for (Path path : sortedPaths) {
                String fileName = path.getFileName().toString();
                if (Files.isDirectory(path)) {
                    buffer.write("40000 ".getBytes());
                    buffer.write(fileName.getBytes());
                    buffer.write(0);
                    byte[] subtreeContent = buildTree(path);
                    GitObject subtree = new GitObject("tree", subtreeContent);
                    buffer.write(HexFormat.of().parseHex(subtree.write()));
                } else {
                    buffer.write("100644 ".getBytes());
                    buffer.write(fileName.getBytes());
                    buffer.write(0);
                    byte[] fileContent = Files.readAllBytes(path);
                    GitObject blob = new GitObject("blob", fileContent);
                    buffer.write(HexFormat.of().parseHex(blob.write()));
                }
            }
        }

        return buffer.toByteArray();
    }
}

