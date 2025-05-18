package pt.isel.cn.landmarks;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import landmarks.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;



public class Client {
    private static String svcIP = "localhost";
    private static int svcPort = 8000;
    private static List<String> lookupIPs = new ArrayList<>(List.of("localhost"));
    private static ManagedChannel channel;
    private static LandmarksServiceGrpc.LandmarksServiceBlockingStub blockingStub;
    private static LandmarksServiceGrpc.LandmarksServiceStub noBlockStub;

    public static void main(String[] args) {
        try {
            svcIP = getNewIP("");
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
                System.out.println("No monuments identified.");
            } else {
                System.out.println("Monuments identified:");
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
                    System.out.printf("- %s: %s (confiança: %.2f)\n",
                            p.getPhotoName(), p.getLandmarkName(), p.getConfidence());
                }
            }

        } catch (Exception e) {
            System.err.println("Error obtaining photos: " + e.getMessage());
        }
    }


    private static String getNewIP(String prevIP) {
        lookupIPs.remove(prevIP);
        if (lookupIPs.isEmpty()) {
            // TODO: Call IPLookup
        }
        Random random = new Random();
        int randomIndex = random.nextInt(lookupIPs.size());
        return lookupIPs.get(randomIndex);
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