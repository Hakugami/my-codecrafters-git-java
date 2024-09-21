package util;


import model.Blob;
import model.Commit;
import model.ObjectType;
import model.Tree;
import protocol.client.GitHttpClient;
import protocol.model.DeltaInstruction;
import protocol.model.PackObject;
import protocol.util.PacketParser;

import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Git {
    public static final int HASH_BYTES_LENGTH = 20;
    public static final int HASH_STRING_LENGTH = 40;
    public static final HexFormat HEX = HexFormat.of();
    public static final Set<Path> FORBIDDEN_DIRECTORIES =
            Set.of(Paths.get(".git"));
    private static final byte[] SPACE_BYTES = {' '};
    private static final byte[] NULL_BYTES = {0};
    private final Path root;

    public Git(Path root) {
        this.root = root;
    }

    //    ---------------------------------ALL INIT RELATED CODE IS
    //    BELOW---------------------------------
    public static Git init(Path root) throws IOException {
        final var git = new Git(root);
        final var dotGit = git.getDotGit();
        if (Files.exists(dotGit)) {
            throw new FileAlreadyExistsException(dotGit.toString());
        }
        Files.createDirectories(git.getObjectsDirectory());
        Files.createDirectories(git.getRefsDirectory());
        final var head = git.getHeadFile();
        Files.createFile(head);
        Files.write(head, "ref: refs/heads/master\n".getBytes());
        final var config = git.getConfigFile();
        Files.createFile(config);
        Files.write(config, ("[core]\n        autocrlf = false").getBytes());
        return git;
    }

    //    ---------------------------------ALL CLONE RELATED CODE IS
    //    BELOW---------------------------------
    public static Git clone(URI uri, Path path)
            throws Exception {
        final var client = new GitHttpClient(uri);
        final var head = client.getRefs().getFirst();
        final var pack = client.getPack(head);
        final var packParser = new PacketParser(ByteBuffer.wrap(pack));
        final var objects = packParser.parse();
        final var git = init(path);
        //		final var git = open(path);
        for (final var object : objects) {
            if (!(object instanceof PackObject.Undeltafied undeltified)) {
                continue;
            }
            final var type = undeltified.type();
            final var hash =
                    git.writeRawObject(new RawObject(type, undeltified.content()));
            System.err.println("wrote %s %s".formatted(hash, type.getName()));
        }
        for (final var object : objects) {
            if (!(object instanceof PackObject.Deltafied deltified)) {
                continue;
            }
            final var baseHash = deltified.baseHash();
            final var base = git.readRawObject(baseHash);
            final var baseType = base.type();
            System.err.println(
                    "apply delta %s %s".formatted(baseHash, baseType.getName()));
            final var content = new byte[deltified.size()];
            final var buffer = ByteBuffer.wrap(content);
            for (final var instruction : deltified.instructions()) {
                if (instruction instanceof DeltaInstruction.Copy copy) {
                    buffer.put(base.content(), copy.offset(), copy.size());
                } else if (instruction instanceof DeltaInstruction.Insert insert) {
                    buffer.put(insert.data());
                } else {
                    throw new UnsupportedOperationException("unknown instruction: " +
                            instruction);
                }
            }
            if (buffer.hasRemaining()) {
                throw new IllegalStateException("buffer is not full");
            }
            final var hash = git.writeRawObject(new RawObject(baseType, content));
            System.err.println("wrote %s %s".formatted(hash, baseType.getName()));
        }
        final var headCommit = git.readCommit(head.hash());
        final var headTree = git.readTree(headCommit.treeHash());
        git.checkout(headTree);
        return git;
    }

    public Path getDotGit() {
        return root.resolve(".git");
    }

    public Path getObjectsDirectory() {
        return getDotGit().resolve("objects");
    }

    public Path getRefsDirectory() {
        return getDotGit().resolve("refs");
    }

    public Path getHeadFile() {
        return getDotGit().resolve("HEAD");
    }

    public Path getConfigFile() {
        return getDotGit().resolve("config");
    }

    //    ---------------------------------ALL BLOB RELATED CODE IS
    //    BELOW---------------------------------
    public String writeBlob(Path path)
            throws IOException, NoSuchAlgorithmException {
        final var bytes = Files.readAllBytes(path);
        final var blob = new Blob(bytes);
        return writeObject(blob);
    }

    public Blob readBlob(String hash) throws FileNotFoundException, IOException {
        return readObject(ObjectType.BLOB, hash);
    }

    //    ---------------------------------ALL TREE RELATED CODE IS
    //    BELOW---------------------------------
    public Tree readTree(String hash) throws FileNotFoundException, IOException {
        return readObject(ObjectType.TREE, hash);
    }

    //    ---------------------------------ALL COMMIT RELATED CODE IS
    //    BELOW---------------------------------
    public Commit readCommit(String hash)
            throws FileNotFoundException, IOException {
        return readObject(ObjectType.COMMIT, hash);
    }

    //    ---------------------------------OBJECT READ RELATED CODE IS
    //    BELOW---------------------------------
    public <T> T readObject(ObjectType<T> type, String hash)
            throws FileNotFoundException, IOException {
        final var first2 = hash.substring(0, 2);
        final var remaining38 = hash.substring(2);
        final var path = getObjectsDirectory().resolve(first2).resolve(remaining38);
        try (final var inputStream = new FileInputStream(path.toFile());
             final var inflaterInputStream = new InflaterInputStream(inputStream)) {
            final var builder = new StringBuilder();
            int value;
            while ((value = inflaterInputStream.read()) != -1 && value != ' ') {
                builder.append((char) value);
            }
            final var typeString = builder.toString();
            if (!type.getName().equals(typeString)) {
                throw new IllegalArgumentException(
                        "trying to read %s as %s (%s)".formatted(typeString, type.getName(),
                                hash));
            }
            builder.setLength(0);
            while ((value = inflaterInputStream.read()) != -1 && value != 0) {
                builder.append((char) value);
            }
            @SuppressWarnings("unused") final var length = Integer.parseInt(builder.toString());
            return type.deserialize(new DataInputStream(inflaterInputStream));
        }
    }

    //    ---------------------------------OBJECT WRITE RELATED CODE IS
    //    BELOW---------------------------------
    @SuppressWarnings("unchecked")
    public <T> String writeObject(T object)
            throws IOException, NoSuchAlgorithmException {
        final var objectType = ObjectType.byClass(object.getClass());
        return writeRawObject(objectType.serialize(object));
    }

    public String writeRawObject(byte[] data)
            throws IOException, NoSuchAlgorithmException {
        final var hashBytes = MessageDigest.getInstance("SHA-1").digest(data);
        final var hash = HexFormat.of().formatHex(hashBytes);
        final var first2 = hash.substring(0, 2);
        final var first2Directory = getObjectsDirectory().resolve(first2);
        Files.createDirectories(first2Directory);
        final var remaining38 = hash.substring(2);
        final var path = first2Directory.resolve(remaining38);
        try (final var outputStream = Files.newOutputStream(path);
             final var deflaterOutputStream =
                     new DeflaterOutputStream(outputStream);) {
            deflaterOutputStream.write(data);
        }
        return hash;
    }

    public RawObject readRawObject(String hash)
            throws FileNotFoundException, IOException {
        final var first2 = hash.substring(0, 2);
        final var remaining38 = hash.substring(2);
        final var path = getObjectsDirectory().resolve(first2).resolve(remaining38);
        try (final var inputStream = new FileInputStream(path.toFile());
             final var inflaterInputStream = new InflaterInputStream(inputStream)) {
            final var builder = new StringBuilder();
            int value;
            while ((value = inflaterInputStream.read()) != -1 && value != ' ') {
                builder.append((char) value);
            }
            final var type = ObjectType.byName(builder.toString());
            builder.setLength(0);
            while ((value = inflaterInputStream.read()) != -1 && value != 0) {
                builder.append((char) value);
            }
            final var length = Integer.parseInt(builder.toString());
            final var content = inflaterInputStream.readNBytes(length);
            return new RawObject(type, content);
        }
    }

    public String writeRawObject(RawObject object)
            throws IOException, NoSuchAlgorithmException {
        final var content = object.content();
        final var lengthBytes = String.valueOf(content.length).getBytes();
        byte[] data;
        try (final var outputStream = new ByteArrayOutputStream();
             final var dataOutputStream = new DataOutputStream(outputStream)) {
            outputStream.write(object.type().getName().getBytes());
            outputStream.write(SPACE_BYTES);
            outputStream.write(lengthBytes);
            outputStream.write(NULL_BYTES);
            outputStream.write(content);
            data = outputStream.toByteArray();
        }
        return writeRawObject(data);
    }

    public void checkout(Tree tree) throws FileNotFoundException, IOException {
        checkout(tree, root);
    }

    public void checkout(Tree tree, Path root)
            throws FileNotFoundException, IOException {
        for (final var entry : tree.entries()) {
            switch (entry.mode().type()) {
                case REGULAR_FILE: {
                    final var blob = readBlob(entry.hash());
                    final var path = root.resolve(entry.name());
                    checkout(blob, path);
                    break;
                }
                case DIRECTORY: {
                    final var subTree = readTree(entry.hash());
                    final var subRoot = root.resolve(entry.name());
                    Files.createDirectories(subRoot);
                    checkout(subTree, subRoot);
                    break;
                }
                default: {
                    throw new UnsupportedOperationException("entry type: " +
                            entry.mode().type());
                }
            }
        }
    }

    public void checkout(Blob blob, Path path)
            throws FileNotFoundException, IOException {
        System.err.println("checkout %s".formatted(path));
        Files.write(path, blob.data());
    }

    @SuppressWarnings("rawtypes")
    public static record RawObject(ObjectType type, byte[] content) {
    }
}
