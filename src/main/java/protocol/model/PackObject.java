package protocol.model;


import model.GitObject;
import model.ObjectType;

import java.util.List;

public sealed interface PackObject permits PackObject.Undeltafied, PackObject.Deltafied {

    record Undeltafied(ObjectType type , byte[] content) implements PackObject {
    }

    record Deltafied(String baseHash, int size , List<DeltaInstruction> instructions) implements PackObject {
    }

    static PackObject undeltafied(ObjectType type , byte[] content) {
        return new Undeltafied(type , content);
    }

    static PackObject deltafied(String baseHash, int size, List<DeltaInstruction> instructions) {
        return new Deltafied(baseHash, size, instructions);
    }

}
