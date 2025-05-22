import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.cloud.pubsub.v1.Subscriber;



public class LandmarksReceiver implements MessageReceiver {
    private LandmarksProcessor landmarksProcessor;

    public LandmarksReceiver(LandmarksProcessor landmarksProcessor) {
        this.landmarksProcessor = landmarksProcessor;
    }

    @Override
    public void receiveMessage(PubsubMessage pubsubMessage, AckReplyConsumer ackReplyConsumer) {
        String requestId = pubsubMessage.getAttributesOrDefault("requestId", "");
        String photoId = pubsubMessage.getAttributesOrDefault("photoId", "");
        String photoName = pubsubMessage.getAttributesOrDefault("photoName", "");
        String blobName = pubsubMessage.getAttributesOrDefault("blobName", "");
        String bucketName = pubsubMessage.getAttributesOrDefault("bucketName", "");

        landmarksProcessor.processMessage(requestId, photoId, photoName, blobName, bucketName);

        ackReplyConsumer.ack();
    }
}