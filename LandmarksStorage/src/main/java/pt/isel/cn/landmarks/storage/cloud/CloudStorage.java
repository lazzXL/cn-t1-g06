package pt.isel.cn.landmarks.storage.cloud;

/**
 * This interface represents a cloud storage system.
 * <p>
 * It can be implemented by various cloud storage providers.
 */
public interface CloudStorage {
    /**
     * Uploads a file to the cloud storage.
     *
     * @param bucketName The name of the bucket where the file will be stored.
     * @param blobName The unique identifier for the file.
     * @param contentType The content type of the file (e.g., "image/png").
     * @param data The byte array representing the file data.
     */
    public void upload(String bucketName, String blobName, String contentType, byte[] data);

    /**
     * Downloads a file from the cloud storage.
     *
     * @param bucketName The name of the bucket where the file is stored.
     * @param blobName The unique identifier for the file.
     */
    public byte[] download(String bucketName, String blobName);

    /**
     * Makes a file public in the cloud storage.
     * <p>
     * @param bucketName The name of the bucket where the file is stored.
     * @param blobName The unique identifier for the file.
     */
    public void makePublic(String bucketName, String blobName);

    /**
     * Gets the public URL of a file in the cloud storage.
     *
     * @param bucketName The name of the bucket where the file is stored.
     * @param blobName The unique identifier for the file.
     * @return The public URL of the file.
     */
    public String getPublicUrl(String bucketName, String blobName);
}
