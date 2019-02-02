import java.util.Objects;

class FileEntry {
    private String filename;
    private int fileSize;
    private int chunkSize;
    private int numChunks;

    public FileEntry(String filename, int fileSize, int chunkSize) {
        this.filename = filename;
        this.fileSize = fileSize;
        this.chunkSize = chunkSize;
        this.numChunks = fileSize / chunkSize + (fileSize % chunkSize != 0 ? 1 : 0);
    }

    public String getFilename() {
        return filename;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getNumChunks() {
        return numChunks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileEntry fileEntry = (FileEntry) o;
        return Objects.equals(filename, fileEntry.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, fileSize, chunkSize, numChunks);
    }
}
