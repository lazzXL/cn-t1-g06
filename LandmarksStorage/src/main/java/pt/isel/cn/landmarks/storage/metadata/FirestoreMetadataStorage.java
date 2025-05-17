package pt.isel.cn.landmarks.storage.metadata;

import com.google.cloud.firestore.Firestore;
import pt.isel.cn.landmarks.domain.AnalysisMetadata;
import pt.isel.cn.landmarks.domain.Config;
import pt.isel.cn.landmarks.domain.LandmarkMetadata;
import pt.isel.cn.landmarks.domain.Status;

public class FirestoreMetadataStorage implements MetadataStorage {
    private final Firestore firestore;

    public FirestoreMetadataStorage(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void saveAnalysisMetadata(AnalysisMetadata metadata) {
        firestore.collection(Config.METADATA_COLLECTION)
                .document(metadata.photoId())
                .set(metadata);
    }

    @Override
    public void updateAnalysisMetadata(String photoId, LandmarkMetadata[] landmarks, Status status) {
        firestore.collection(Config.METADATA_COLLECTION)
                .document(photoId)
                .update("landmarks", landmarks, "status", status);
    }

    @Override
    public AnalysisMetadata getAnalysisMetadata(String photoId) {
        try {
            return firestore.collection(Config.METADATA_COLLECTION)
                    .whereEqualTo("photoId", photoId)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .findFirst()
                    .map(doc -> doc.toObject(AnalysisMetadata.class))
                    .orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public AnalysisMetadata[] getAnalysisMetadataByConfidenceThreshold(double confidenceThreshold) {
        try {
            return firestore.collection(Config.METADATA_COLLECTION)
                    .whereGreaterThan("landmarks.confidence", confidenceThreshold)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.toObject(AnalysisMetadata.class))
                    .toArray(AnalysisMetadata[]::new);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
