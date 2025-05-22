import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.ProjectSubscriptionName;
i

public class LandmarksSubscriber {
    private final String PROJECT_ID = "cn2425-t1-g06";
    private final String subscriptionID = "landmarks-photos-sub";

    public void subscribe(LandmarksProcessor subscriber) {
        ProjectSubscriptionName subscriptionName =
                ProjectSubscriptionName.of(PROJECT_ID, subscriptionID);
        ExecutorProvider executorProvider = InstantiatingExecutorProvider
                .newBuilder()
                .setExecutorThreadCount(1)
                .build();
        Subscriber sub = Subscriber.newBuilder(subscriptionName, new LandmarksReceiver(subscriber))
                .setExecutorProvider(executorProvider)
                .build();
        sub.startAsync().awaitTerminated();
    }
}