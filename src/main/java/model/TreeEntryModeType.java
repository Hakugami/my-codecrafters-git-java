package model;

import lombok.Getter;
import lombok.experimental.Accessors;
@Getter
@Accessors(fluent = true)
public enum TreeEntryModeType {
    DIRECTORY(0b0100), // tree
    REGULAR_FILE(0b1000), // blob
    SYMBOLIC_LINK(0b1010), // symlink
    GITLINK(0b1110); // submodule
    private final int mask;
    private TreeEntryModeType(int value) { this.mask = value; }
    public int shifted() { return mask << 12; } // 0b1000 -> 0b100000000000
    public boolean isPermissionless() { return this != REGULAR_FILE; }
    public static TreeEntryModeType match(int value) {
        value >>= 12;
        for (final var type : values()) {
            final var mask = type.mask();
            if (value == mask) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown value: 0b" +
                Integer.toBinaryString(value));
    }
}
