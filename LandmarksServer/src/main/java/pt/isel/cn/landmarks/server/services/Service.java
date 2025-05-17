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
import java.util.logging.Logger;

public class Service {
    private final CloudStorage cloudStorage;
    private final MetadataStorage metadataStorage;
    private final LandmarksPublisher landmarksPublisher;

    private static final Logger logger = Logger.getLogger(Service.class.getName());

    public Service(CloudStorage cloudStorage, MetadataStorage metadataStorage, LandmarksPublisher landmarksPublisher) {
        this.cloudStorage = cloudStorage;
        this.metadataStorage = metadataStorage;
        this.landmarksPublisher = landmarksPublisher;
    }

    /**
     * Pre-processes the photo by checking its metadata. If metadata exists and
     * the status is not FAILURE, then the photo is already present in the system and is
     * currently being processed.
     * <p>
     * If the metadata does not exist, we should proceed to the storage of the
     * photo in the cloud and the metadata in the database.
     *
     * @param photoId The ID of the photo to pre-process.
     * @return The photoId if processing is successful, null otherwise.
     */
    public String preProcessPhoto(String photoId) {
        AnalysisMetadata metadata = metadataStorage.getAnalysisMetadata(photoId);
        if (metadata == null) {
            return null;
        }
        if (metadata.status() == Status.FAILURE) {
            landmarksPublisher.publish(photoId, metadata.photoName());
        }
        return photoId;
    }

    /**
     * Submits a photo to the cloud and metadata storage and publishes it for processing.
     *
     * @param photoId The ID of the photo to submit.
     * @param photoName The name of the photo.
     * @param photoBytes The byte array of the photo.
     * @return Either a PhotoSubmitError or null if successful.
     */
    public Either<PhotoSubmitError, Void> submitPhoto(String photoId, String photoName, byte[] photoBytes) {
        try {
            cloudStorage.upload(Config.PHOTOS_BUCKET, photoId, "image/png", photoBytes);
            cloudStorage.makePublic(Config.PHOTOS_BUCKET, photoId);
            landmarksPublisher.publish(photoId, photoName);
            return Either.right(null);
        } catch (Exception e) {
            logger.severe("Error submitting photo: " + e.getMessage());
            return Either.left(new PhotoSubmitError());
        }
    }

    /**
     * Looks up a photo by its ID and retrieves its metadata.
     *
     * @param photoId The ID of the photo to look up.
     * @return Either a LookupError or the AnalysisMetadata if successful.
     */
    public Either<LookupError, AnalysisMetadata> lookupPhoto(String photoId) {
        try {
            AnalysisMetadata analysisMetadata = metadataStorage.getAnalysisMetadata(photoId);

            if (analysisMetadata == null) {
                return Either.left(new LookupError(LookupErrorType.NOT_FOUND));
            }

            if (analysisMetadata.status() == Status.IN_PROGRESS) {
                return Either.left(new LookupError(LookupErrorType.PENDING));
            }

            if (analysisMetadata.status() == Status.FAILURE) {
                return Either.left(new LookupError(LookupErrorType.FAILED));
            }

            return Either.right(analysisMetadata);
        } catch (Exception e) {
            logger.severe("Error looking up photo: " + e.getMessage());
            return Either.left(new LookupError(LookupErrorType.UNKNOWN));
        }
    }

    /**
     * Retrieves all photos that have a confidence score above the specified threshold.
     *
     * @param confidenceThreshold The confidence threshold.
     * @return Either a PhotosByConfidenceError or a list of AnalysisMetadata if successful.
     */
    public Either<PhotosByConfidenceError, List<AnalysisMetadata>> getPhotosByConfidenceThreshold(double confidenceThreshold) {
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
