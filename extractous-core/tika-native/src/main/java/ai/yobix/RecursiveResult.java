package ai.yobix;

import org.apache.tika.metadata.Metadata;

import java.util.List;

public class RecursiveResult {

    private final List<Metadata> metadataList;
    private final byte status;
    private final String errorMessage;

    public RecursiveResult(List<Metadata> metadataList) {
        this.metadataList = metadataList;
        this.status = 0;
        this.errorMessage = null;
    }

    public RecursiveResult(byte status, String errorMessage) {
        this.metadataList = null;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    /**
     * Returns the list of Metadata for all documents (container + embedded)
     * The first element is the container document, followed by embedded documents
     * @return List of Metadata objects
     */
    public List<Metadata> getMetadataList() {
        return metadataList;
    }

    public boolean isError() {
        return status != 0;
    }

    /**
     * Returns the status of the call
     * @return
     * 0: OK
     * 1: IOException
     * 2: TikaException
     */
    public byte getStatus() {
        return status;
    }

    /**
     * Returns the error message in case of error
     * @return String representing the error message or
     * null if there is no error
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Bridge method to avoid JNI calls into java.util.List from native code.
     * Returns the number of metadata items (container + embedded).
     */
    public int size() {
        return metadataList != null ? metadataList.size() : 0;
    }

    /**
     * Bridge method to avoid JNI calls into java.util.List from native code.
     * Returns the Metadata at the given index.
     */
    public Metadata getMetadataAt(int index) {
        return metadataList.get(index);
    }

    /**
     * Optional bridge returning an array, which is also JNI-friendly.
     */
    public Metadata[] getMetadataArray() {
        return metadataList != null ? metadataList.toArray(new Metadata[0]) : new Metadata[0];
    }

    public String toString() {
        return "status:" + this.status + " error: " + this.errorMessage + 
               " documents: " + (metadataList != null ? metadataList.size() : 0);
    }
}
