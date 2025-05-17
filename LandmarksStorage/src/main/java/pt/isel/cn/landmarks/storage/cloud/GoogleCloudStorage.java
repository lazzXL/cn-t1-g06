package pt.isel.cn.landmarks.storage.cloud;

import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class GoogleCloudStorage implements CloudStorage {
    private final Storage storage;

    public GoogleCloudStorage(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void upload(String bucketName, String blobName, String contentType, byte[] data) {
        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();

        try (WriteChannel writer = storage.writer(blobInfo);
             InputStream input = new ByteArrayInputStream(data)
        ) {
            byte[] buffer = new byte[1024];
            int limit;
            while ((limit = input.read(buffer)) >= 0) {
                writer.write(ByteBuffer.wrap(buffer, 0, limit));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] download(String bucketName, String blobName) {
        BlobId blobId = BlobId.of(bucketName, blobName);
        Blob blob = storage.get(blobId);

        if (blob == null) {
            throw new IllegalArgumentException("Blob not found: " + blobName);
        }

        if (blob.getSize() < 1_000_000) {
            return blob.getContent();
        }

        try (ReadChannel readChannel = blob.reader(); WritableByteChannel writableByteChannel = Channels.newChannel(Files.newOutputStream(Paths.get(blobName)))) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (readChannel.read(buffer) > 0) {
                buffer.flip();
                writableByteChannel.write(buffer);
                buffer.clear();
            }
            return buffer.array();
        } catch (Exception e) {
            throw new RuntimeException("Error downloading blob: " + blobName, e);
        }
    }

    @Override
    public void makePublic(String bucketName, String blobName) {
        BlobId blobId = BlobId.of(bucketName, blobName);
        Blob blob = storage.get(blobId);

        if (blob == null) {
            throw new IllegalArgumentException("Blob not found: " + blobName);
        }

        Acl acl = Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER);
        blob.createAcl(acl);
    }

    @Override
    public String getPublicUrl(String bucketName, String blobName) {
        return "gs://" + bucketName + "/" + blobName;
    }
}
