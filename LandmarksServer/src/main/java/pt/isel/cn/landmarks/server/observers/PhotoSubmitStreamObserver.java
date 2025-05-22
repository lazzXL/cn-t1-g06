package pt.isel.cn.landmarks.server.observers;

import io.grpc.stub.StreamObserver;
import landmarks.SubmitIdentifier;
import landmarks.SubmitPhotoRequest;
import org.apache.commons.codec.binary.Hex;
import pt.isel.cn.landmarks.domain.Either;
import pt.isel.cn.landmarks.server.error.PhotoSubmitError;
import pt.isel.cn.landmarks.server.services.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

public class PhotoSubmitStreamObserver implements StreamObserver<SubmitPhotoRequest> {
    private String photoName;
    private final StreamObserver<SubmitIdentifier> responseObserver;
    private final ByteArrayOutputStream resultBytes;
    private final MessageDigest messageDigest;;
    private final Service service;

    private static final Logger logger = Logger.getLogger(PhotoSubmitStreamObserver.class.getName());

    public PhotoSubmitStreamObserver(StreamObserver<SubmitIdentifier> responseObserver, Service service) {
        this.service = service;
        this.resultBytes = new ByteArrayOutputStream();
        this.responseObserver = responseObserver;
        messageDigest = createSha256Digest();
    }

    @Override
    public void onNext(SubmitPhotoRequest submitPhotoRequest) {
        try {
            resultBytes.write(submitPhotoRequest.getPhoto().toByteArray());
            messageDigest.update(submitPhotoRequest.getPhoto().toByteArray());
            if (photoName == null) {
                photoName = submitPhotoRequest.getPhotoName();
            }
        } catch (IOException e) {
            logger.severe("Error writing photo bytes: " + e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        logger.severe("Error in stream: " + throwable.getMessage());
        responseObserver.onError(throwable);
    }

    @Override
    public void onCompleted() {
        String photoId = Hex.encodeHexString(messageDigest.digest());

        Either<PhotoSubmitError, String> result = service.submitPhoto(photoId, photoName, resultBytes.toByteArray());

        if (result.isLeft()) {
            logger.severe("Error submitting photo: " + result.getLeft());
            responseObserver.onError(new RuntimeException(result.getLeft().getMessage()));
            return;
        }

        SubmitIdentifier response = SubmitIdentifier
                .newBuilder()
                .setIdentifier(result.getRight())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        logger.info("Photo submitted successfully with ID: " + result.getRight());
    }


    private static MessageDigest createSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
