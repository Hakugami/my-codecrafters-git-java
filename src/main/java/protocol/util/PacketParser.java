package protocol.util;

import lombok.RequiredArgsConstructor;
import model.GitObject;
import protocol.client.GitHttpClient;
import protocol.model.DeltaInstruction;
import protocol.model.PackObject;
import protocol.model.PackObjectHeader;
import protocol.model.PackObjectType;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

@RequiredArgsConstructor
public class PacketParser {

    public static final int TYPE_MASK = 0b01110000;
    public static final int SIZE_4_MASK = 0b00001111;
    public static final int SIZE_7_MASK = 0b01111111;
    public static final int SIZE_CONTINUE_MASK = 0b10000000;

    private final ByteBuffer buffer;

    private static int parseVariableLengthIntegerLittleEndian(ByteBuffer buffer) {
        int result = 0;
        int shift = 0;
        int b;
        do {
            b = Byte.toUnsignedInt(buffer.get());
            result |= (b & SIZE_7_MASK) << shift;
            shift += 7;
        } while ((b & SIZE_CONTINUE_MASK) != 0);
        return result;
    }

    public static int parseVariableLengthInteger(ByteBuffer buffer, boolean... enableStates) {
        int result = 0;
        int offset = 0;
        for (final boolean enableState : enableStates) {
            if (enableState) {
                int b = Byte.toUnsignedInt(buffer.get());
                result += b << offset;
            }
            offset += 8;
        }
        return result;
    }

    public static List<DeltaInstruction> parseDeltaInstructions(ByteBuffer contentBuffer) {
        final List<DeltaInstruction> instructions = new ArrayList<>();
        while (contentBuffer.hasRemaining()) {
            final int instruction = Byte.toUnsignedInt(contentBuffer.get());
            if (instruction == 0) {
                throw new IllegalArgumentException("Invalid instruction");
            }
            final int command = instruction & SIZE_CONTINUE_MASK;

            if (command == 0) {
                final int size = instruction & SIZE_7_MASK;
                final byte[] bytes = new byte[size];
                contentBuffer.get(bytes);
                instructions.add(DeltaInstruction.insert(bytes));
            } else {
                final var hasOffset1 = (instruction & 0b0000_0001) != 0;
                final var hasOffset2 = (instruction & 0b0000_0010) != 0;
                final var hasOffset3 = (instruction & 0b0000_0100) != 0;
                final var hasOffset4 = (instruction & 0b0000_1000) != 0;
                final var hasSize1 = (instruction & 0b0001_0000) != 0;
                final var hasSize2 = (instruction & 0b0010_0000) != 0;
                final var hasSize3 = (instruction & 0b0100_0000) != 0;

                final int offset = parseVariableLengthInteger(contentBuffer, hasOffset1, hasOffset2, hasOffset3, hasOffset4);

                int size = parseVariableLengthInteger(contentBuffer, hasSize1, hasSize2, hasSize3);

                if (size == 0) {
                    size = 0x10000;
                }

                instructions.add(DeltaInstruction.copy(offset, size));
            }
        }
        return instructions;
    }

    public List<PackObject> parse() throws DataFormatException {
        parseSignature();
        parseVersion();

        final int objectCount = buffer.getInt();
        final ArrayList<PackObject> objectTypes = new ArrayList<>(objectCount);
        final Map<Integer, Object> objectByOffset = new HashMap<>(objectCount);

        for (int i = 0; i < objectCount; i++) {
            final PackObjectHeader header = parseObjectHeader();
            PackObjectType type = header.type();
            final int bufferPosition = buffer.position();

            final PackObject object = switch (type) {
                case COMMIT:
                case TREE:
                case BLOB: {
                    final byte[] content = inflate(header.size());
                    yield PackObject.undeltafied(type.typeName(), content);
                }

                case TAG: {
                    throw new UnsupportedOperationException("Tag type is not supported");
                }

                case OFS_DELTA: {
                    throw new UnsupportedOperationException("OFS_DELTA type is not supported");
                }

                case REF_DELTA: {
                    final byte[] hashBytes = new byte[GitHttpClient.HASH_BYTES_LENGTH];
                    buffer.get(hashBytes);

                    final String baseHash = HexFormat.of().formatHex(hashBytes);
                    final byte[] content = inflate(header.size());
                    ByteBuffer contentBuffer = ByteBuffer.wrap(content);

                    final int baseObjectSize = parseVariableLengthIntegerLittleEndian(contentBuffer);
                    final int newObjectSize = parseVariableLengthIntegerLittleEndian(contentBuffer);

                    final List<DeltaInstruction> instructions = parseDeltaInstructions(contentBuffer);

                    yield PackObject.deltafied(baseHash, newObjectSize, instructions);

                }
            };
            if(object != null) {
                objectByOffset.put(bufferPosition, object);
                objectTypes.add(object);
            }
        }
        return objectTypes;
    }

    public byte[] inflate(int size) throws DataFormatException {
        final Inflater inflater = new Inflater();
        inflater.setInput(buffer);

        final byte[] result = new byte[size];
        inflater.inflate(result);
        return result;
    }

    public void parseSignature() {
        final var bytes = new byte[Integer.BYTES];
        buffer.get(bytes);
        final var signature = new String(bytes);
        if (!"PACK".equals(signature)) {
            throw new IllegalStateException("invalid signature: " + signature);
        }
    }
    public void parseVersion() {
        final var version = buffer.getInt();
        if (version != 2) {
            throw new IllegalStateException("invalid version: " + version);
        }
    }

    public PackObjectHeader parseObjectHeader() {
        var read = Byte.toUnsignedInt(buffer.get());

        final var type = PackObjectType.valueOf((read & TYPE_MASK) >> 4);
        var size = read & SIZE_4_MASK;
        if ((read & SIZE_CONTINUE_MASK) == 0) {
            return new PackObjectHeader(type, size);
        }
        size |= parseVariableLengthIntegerLittleEndian(buffer) << 4;
        return new PackObjectHeader(type, size);
    }
}
