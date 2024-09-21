package protocol.model;

import java.io.OutputStream;

public sealed interface PacketLine permits PacketLine.Data, PacketLine.Flush {
    static Data data(byte[] data) {
        return new Data(data);
    }

    static Data data(String data) {
        return new Data(data.getBytes());
    }

    static Flush flush() {
        return Flush.INSTANCE;
    }

    void serialize(OutputStream os) throws Exception;

    enum Flush implements PacketLine {
        INSTANCE;

        private static final byte[] FLUSH = "0000".getBytes();

        @Override
        public void serialize(OutputStream os) throws Exception {
            os.write(FLUSH);
        }
    }

    record Data(byte[] content) implements PacketLine {
        @Override
        public void serialize(OutputStream os) throws Exception {
            final byte[] size = "%04x".formatted(content.length + 4).getBytes();
            os.write(size);
            os.write(content);
        }

        public boolean isComment() {
            return content.length != 0 && content[0] == '#';
        }
    }

}
