package protocol.model;

public sealed interface DeltaInstruction permits DeltaInstruction.Copy, DeltaInstruction.Insert {
    static Copy copy(int offset, int size) {
        return new Copy(offset, size);
    }

    static Insert insert(byte[] data) {
        return new Insert(data);
    }

    record Copy(int offset, int size) implements DeltaInstruction {
    }

    record Insert(byte[] data) implements DeltaInstruction {
    }
}
