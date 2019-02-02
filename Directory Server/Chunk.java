import java.util.Objects;

public class Chunk {
    private FileEntry file;
    private int chunkNumber;

    private Chunk() {}

    public Chunk(FileEntry file, int chunkNumber) {
        this.file = file;
        this.chunkNumber = chunkNumber;
    }

    public FileEntry getFile() {
        return file;
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chunk chunk = (Chunk) o;
        return chunkNumber == chunk.chunkNumber &&
                Objects.equals(file, chunk.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, chunkNumber);
    }
}