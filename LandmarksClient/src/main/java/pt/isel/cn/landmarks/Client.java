package pt.isel.cn.landmarks;

import com.google.gson.Gson;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import landmarks.*;
import pt.isel.cn.landmarks.domain.Either;
import pt.isel.cn.landmarks.error.NoValidIPs;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;



public class Client {
    private static String svcIP = null;
    private static int svcPort = 8000;
    private static List<String> lookupIPs = new ArrayList<>(List.of());
    private static ManagedChannel channel;
    private static LandmarksServiceGrpc.LandmarksServiceBlockingStub blockingStub;
    private static LandmarksServiceGrpc.LandmarksServiceStub noBlockStub;

    private static final String groupName = "landmarks-instance-group";
    private static final String zone = "europe-west1-b";
    private static final String ipLookupURL = "https://europe-west1-cn2425-t1-g06.cloudfunctions.net/funcIPLookup";

    public static void main(String[] args) {
        try {
            do {
                Either<NoValidIPs, String> newIP = getNewIP();
                if(newIP.isRight()) {
                    break;
                } else {
                    System.out.println("No valid IPs found, retrying...");
                    //Timeout 1 second
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("Error during sleep: " + e.getMessage());
                    }
                }
            } while (true);

            System.out.println("Connecting to " + svcIP + ":" + svcPort);
            channel = ManagedChannelBuilder.forAddress(svcIP, svcPort)
                    .usePlaintext()
                    .build();
            blockingStub = LandmarksServiceGrpc.newBlockingStub(channel);
            noBlockStub = LandmarksServiceGrpc.newStub(channel);

            Scanner scanner = new Scanner(System.in);
            boolean end = false;

            while (!end) {
                try {
                    int option = menu(scanner);
                    switch (option) {
                        case 1 -> submitPhoto(scanner);
                        case 2 -> getResults(scanner);
                        case 3 -> getPhotos(scanner);
                        case 99 -> System.exit(0);
                    }
                } catch (Exception ex) {
                    System.out.println("Execution call Error  !");
                    ex.printStackTrace();
                }
            }
            read("prima enter to end", new Scanner(System.in));
        } catch (Exception ex) {
            System.out.println("Unhandled exception");
            ex.printStackTrace();
        }
    }

    private static void submitPhoto(Scanner scanner) {
        try {
            System.out.print("Enter path to photo file: ");
            String path = scanner.nextLine();

            File file = new File(path);
            String photoName = file.getName();
            FileInputStream fis = new FileInputStream(file);
            byte[] data = fis.readAllBytes();
            fis.close();

            StreamObserver<SubmitIdentifier> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(SubmitIdentifier id) {
                    System.out.println("Photo submitted with ID: " + id.getIdentifier());
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("Error during submitPhoto: " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    System.out.println("Photo submission completed.");
                }
            };

            StreamObserver<SubmitPhotoRequest> requestObserver = noBlockStub.submitPhoto(responseObserver);

            SubmitPhotoRequest request = SubmitPhotoRequest.newBuilder()
                    .setPhotoName(photoName)
                    .setPhoto(com.google.protobuf.ByteString.copyFrom(data))
                    .build();
            requestObserver.onNext(request);
            requestObserver.onCompleted();

        } catch (Exception e) {
            System.err.println("Exception in submitPhoto: " + e.getMessage());
        }
    }

    static void getResults(Scanner scanner) {
        try {
            System.out.print("ID: ");
            String id = scanner.nextLine();

            SubmitIdentifier request = SubmitIdentifier.newBuilder().setIdentifier(id).build();
            LookupResults results = blockingStub.lookupResults(request);

            List<Landmark> landmarks = results.getLandmarksList();
            if (landmarks.isEmpty()) {
                System.out.println("No landmarks identified.");
            } else {
                System.out.println("Landmarks identified:");
                for (Landmark lm : landmarks) {
                    System.out.printf("- %s (%.6f, %.6f) Confidence: %.2f\n",
                            lm.getName(), lm.getLatitude(), lm.getLongitude(), lm.getConfidence());
                }
            }

            if (results.getMap().size() > 0) {
                String filename = "map-" + UUID.randomUUID() + ".png";
                java.nio.file.Files.write(java.nio.file.Path.of(filename), results.getMap().toByteArray());
                System.out.println("Static map saved: " + filename);
            }

        } catch (Exception e) {
            System.err.println("Error obtaining results: " + e.getMessage());
        }
    }

    static void getPhotos(Scanner scanner) {
        try {
            System.out.print("Minimum confidence [0.0 - 1.0]: ");
            float threshold = scanner.nextFloat();
            scanner.nextLine();

            ConfidenceThreshold req = ConfidenceThreshold.newBuilder()
                    .setConfidenceThreshold(threshold)
                    .build();

            GetPhotosResponse response = blockingStub.getPhotos(req);
            List<Photo> photos = response.getPhotosList();

            if (photos.isEmpty()) {
                System.out.println("No photo with confidence >= " + threshold);
            } else {
                System.out.println("Photos found:");
                for (Photo p : photos) {
                    System.out.printf("- %s: %s (confidence: %.2f)\n",
                            p.getPhotoName(), p.getLandmarkName(), p.getConfidence());
                }
            }

        } catch (Exception e) {
            System.err.println("Error obtaining photos: " + e.getMessage());
        }
    }


    private static Either<NoValidIPs, String> getNewIP() {
        if(svcIP != null && !lookupIPs.isEmpty()) {
            // Get new IP from the list
            String ip = lookupIPs.remove(0);
            return Either.right(ip);
        } else {
            // Obtain new IPs from the service
            try {
                String urlStr = ipLookupURL + "?zone=" + zone + "&groupName=" + groupName;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2000); // 2s timeout
                conn.setReadTimeout(2000);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    return Either.left(new NoValidIPs());
                }

                try (Scanner scanner = new Scanner(conn.getInputStream())) {
                    StringBuilder jsonBuilder = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        jsonBuilder.append(scanner.nextLine());
                    }

                    String json = jsonBuilder.toString();
                    Gson gson = new Gson();
                    lookupIPs = gson.fromJson(json, List.class);

                    if (lookupIPs != null && !lookupIPs.isEmpty()) {
                        String ip = lookupIPs.remove(0); // use the first valid IP
                        return Either.right(ip);
                    }

                    return Either.left(new NoValidIPs());
                }
            } catch (Exception e) {
                System.out.println("Error obtaining IP: " + e.getMessage());
                return Either.left(new NoValidIPs());
            }
        }
    }


    private static int menu(Scanner scanner) {
        int op;
        do {
            System.out.println();
            System.out.println("    MENU");
            System.out.println(" 1 - Submit photo");
            System.out.println(" 2 - Lookup results");
            System.out.println(" 3 - Get photos");
            System.out.println("99 - Exit");
            System.out.println();
            System.out.println("Choose an Option?");
            op = scanner.nextInt();
            scanner.nextLine();
        } while (!((op >= 1 && op <= 3) || op == 99));
        return op;
    }

    private static String read(String msg, Scanner input) {
        System.out.println(msg);
        return input.nextLine();
    }



}