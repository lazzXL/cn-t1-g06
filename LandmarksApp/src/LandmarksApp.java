import com.google.cloud.vision.v1.*;
import com.google.type.LatLng;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;




public class LandmarkApp {
    private final LandmarksSubscriber landmarksSubscriber = new LandmarksSubscriber();
    private final FirestoreMetadataStorage = new FirestoreMetadataStorage();
    private final GoogleCloudStorage googleCloudStorage = new GoogleCloudStorage();
    private final VisionService = new VisionService();


    public static void main(String[] args) throws IOException {
        landmarksSubscriber.subscribe((requestId, photoId, photoName, blobName, bucketName) -> {
            public AnalysisMetadata metadata = new AnalysisMetadata(photoId, photoName, Status.PENDING, null);
            FirestoreMetadataStorage.saveAnalysisMetadata(requestId, metadata);
            public String URL = GoogleCloudStorage.getPublicUrl(bucketName, blobName);
            public List<Landmarks> landmarks = VisionService.analyzePhoto(URL);
            FirestoreMetadataStorage.updateAnalysisMetadata(requestId, landmarks, Status.SUCCESS);
        });
    }

}