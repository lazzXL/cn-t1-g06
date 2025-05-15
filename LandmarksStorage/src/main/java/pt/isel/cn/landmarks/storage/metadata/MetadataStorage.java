package pt.isel.cn.landmarks.storage.metadata;

import pt.isel.cn.landmarks.domain.AnalysisMetadata;
import pt.isel.cn.landmarks.domain.LandmarkMetadata;
import pt.isel.cn.landmarks.domain.Status;

/**
 * This interface defines the contract for the metadata storage
 */
public interface MetadataStorage {
    /**
     * Saves the analysis metadata.
     *
     * @param metadata The metadata to save.
     */
    public void saveAnalysisMetadata(AnalysisMetadata metadata);

    /**
     * Updates the analysis metadata.
     *
     * @param photoId The unique identifier for the photo.
     * @param landmarks An array of LandmarkMetadata objects representing the detected landmarks.
     * @param status The status of the analysis.
     */
    public void updateAnalysisMetadata(String photoId, LandmarkMetadata[] landmarks, Status status);

    /**
     * Retrieves the analysis metadata by photo ID.
     *
     * @param photoId The unique identifier for the photo.
     * @return The analysis metadata for the specified photo ID.
     */
    public AnalysisMetadata getAnalysisMetadata(String photoId);

    /**
     * Retrieves all analysis metadata above a certain confidence threshold.
     *
     * @return An array of all analysis metadata.
     */
    public AnalysisMetadata[] getAnalysisMetadataByConfidenceThreshold(
        double confidenceThreshold
    );
}
