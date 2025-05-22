package pt.isel.cn.landmarks.server.services;

import pt.isel.cn.landmarks.domain.AnalysisMetadata;
import pt.isel.cn.landmarks.domain.Config;
import pt.isel.cn.landmarks.domain.Either;
import pt.isel.cn.landmarks.domain.Status;
import pt.isel.cn.landmarks.server.error.LookupError;
import pt.isel.cn.landmarks.server.error.LookupErrorType;
import pt.isel.cn.landmarks.server.error.PhotoSubmitError;
import pt.isel.cn.landmarks.server.error.PhotosByConfidenceError;
import pt.isel.cn.landmarks.server.publisher.LandmarksPublisher;
import pt.isel.cn.landmarks.storage.cloud.CloudStorage;
import pt.isel.cn.landmarks.storage.metadata.MetadataStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class Service {
    private final CloudStorage cloudStorage;
    private final MetadataStorage metadataStorage;
    private final LandmarksPublisher landmarksPublisher;

    private static final String PHOTOS_BUCKET = Config.PHOTOS_BUCKET;

    private static final Logger logger = Logger.getLogger(Service.class.getName());

    public Service(CloudStorage cloudStorage, MetadataStorage metadataStorage, LandmarksPublisher landmarksPublisher) {
        this.cloudStorage = cloudStorage;
        this.metadataStorage = metadataStorage;
        this.landmarksPublisher = landmarksPublisher;
    }

    /**
     * Submits a photo to the cloud if it doesn't already exist
     * and makes an analysis request.
     * <p>
     * The photo itself is only stored once in the blob storage. However,
     * there can be multiple analysis requests for the same photo,
     * each having its results metadata stored in the metadata storage.
     *
     * @param photoId The ID of the photo to submit.
     * @param photoName The name of the photo.
     * @param photoBytes The byte array of the photo.
     * @return Either a PhotoSubmitError or null if successful.
     */
    public Either<PhotoSubmitError, String> submitPhoto(String photoId, String photoName, byte[] photoBytes) {
        String requestId = UUID.randomUUID().toString();
        try {
            if (!cloudStorage.blobExists(PHOTOS_BUCKET, photoId)) {
                cloudStorage.upload(PHOTOS_BUCKET, photoId, "image/png", photoBytes);
                cloudStorage.makePublic(PHOTOS_BUCKET, photoId);
            }
            landmarksPublisher.publish(requestId, photoId, photoName);
            return Either.right(requestId);
        } catch (Exception e) {
            logger.severe("Error submitting photo: " + e.getMessage());
            return Either.left(new PhotoSubmitError());
        }
    }

    /**
     * Looks up the results of a photo analysis by the request ID.
     *
     * @param requestId The ID of the request.
     * @return Either a LookupError or the AnalysisMetadata if successful.
     */
    public Either<LookupErrorType, AnalysisMetadata> lookupResults(String requestId) {
        try {
            AnalysisMetadata analysisMetadata = metadataStorage.getAnalysisMetadata(requestId);

            if (analysisMetadata == null) {
                return Either.left(LookupErrorType.NOT_FOUND);
            }

            if (analysisMetadata.status() == Status.IN_PROGRESS) {
                return Either.left(LookupErrorType.PENDING);
            }

            if (analysisMetadata.status() == Status.FAILURE) {
                return Either.left(LookupErrorType.FAILED);
            }

            return Either.right(analysisMetadata);
        } catch (Exception e) {
            logger.severe("Error looking up photo: " + e.getMessage());
            return Either.left(LookupErrorType.UNKNOWN);
        }
    }

    /**
     * Retrieves all analysis metadata above a certain confidence threshold.
     *
     * @param confidenceThreshold The confidence threshold.
     * @return Either a PhotosByConfidenceError or a list of AnalysisMetadata if successful.
     */
    public Either<PhotosByConfidenceError, List<AnalysisMetadata>> getResultsByConfidenceThreshold(double confidenceThreshold) {
        try {
            AnalysisMetadata[] metadataArray = metadataStorage.getAnalysisMetadataByConfidenceThreshold(confidenceThreshold);
            if (metadataArray == null || metadataArray.length == 0) {
                return Either.right(new ArrayList<>());
            }
            return Either.right(Arrays
                    .stream(metadataArray)
                    .filter(metadata -> metadata.status() == Status.SUCCESS)
                    .toList());
        } catch (Exception e) {
            logger.severe("Error retrieving photos by confidence threshold: " + e.getMessage());
            return Either.left(new PhotosByConfidenceError());
        }
    }
}
