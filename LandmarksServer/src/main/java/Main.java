import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.storage.StorageOptions;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.isel.cn.landmarks.server.LandmarksServer;
import pt.isel.cn.landmarks.server.publisher.LandmarksPublisher;
import pt.isel.cn.landmarks.server.services.MapsService;
import pt.isel.cn.landmarks.server.services.Service;
import pt.isel.cn.landmarks.storage.cloud.CloudStorage;
import pt.isel.cn.landmarks.storage.cloud.GoogleCloudStorage;
import pt.isel.cn.landmarks.storage.metadata.FirestoreMetadataStorage;
import pt.isel.cn.landmarks.storage.metadata.MetadataStorage;


import java.io.IOException;

public class Main {
    private static final int PORT = 8000;

    public static void main(String[] args) throws IOException, InterruptedException {
        LandmarksPublisher publisher = new LandmarksPublisher();
        MetadataStorage metadataStorage = new FirestoreMetadataStorage(FirestoreOptions.getDefaultInstance().getService());
        CloudStorage cloudStorage = new GoogleCloudStorage(StorageOptions.getDefaultInstance().getService());
        Service service = new Service(cloudStorage, metadataStorage, publisher);

        Server server = ServerBuilder.forPort(PORT)
                .addService(new LandmarksServer(service, new MapsService()))
                .build()
                .start();

        System.out.println("Server started, listening on " + PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Shutting down gRPC server");
            server.shutdown();
            System.err.println("Server shut down");
        }));

        server.awaitTermination();
    }
}
