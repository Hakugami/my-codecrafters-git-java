package protocol.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import model.ObjectType;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Accessors(fluent = true)
@Getter
public enum PackObjectType {
    COMMIT(1, ObjectType.COMMIT),
    TREE(2, ObjectType.TREE),
    BLOB(3, ObjectType.BLOB),
    TAG(4, null),
    OFS_DELTA(6, null),
    REF_DELTA(7, null);

    private static final Map<Integer, PackObjectType> MAPPING = new HashMap<>();

    static {
        for (final PackObjectType type : PackObjectType.values()) {
            MAPPING.put(type.value(), type);
        }
    }

    private final int value;

    private final ObjectType typeName;

    public static PackObjectType valueOf(final int value) {
        if (value == 4) {
            throw new UnsupportedOperationException("Tag type is not supported");
        }

        final PackObjectType type = MAPPING.get(value);

        if (type == null) {
            throw new IllegalArgumentException("Unknown object type: " + value);
        }

        return type;
    }


}
