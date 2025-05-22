@FunctionalInterface
public interface LandmarksProcessor {
    void processMessage(String requestId, String message, String photoName, String blobName, String bucketName);
}