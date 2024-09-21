package protocol.client;

import lombok.Data;

import java.net.URI;

@Data
public abstract class GitClient {
    private URI url;

    public GitClient(URI url) {
        this.url = url;
    }

    public boolean clone(URI url, String path) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean commit(String message) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean push() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean pull() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
