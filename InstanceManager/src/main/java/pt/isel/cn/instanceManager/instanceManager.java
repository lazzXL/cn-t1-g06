package pt.isel.cn.instanceManager;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.cloud.compute.v1.*;

import java.io.FileInputStream;
import java.util.List;

public class instanceManager {

    private static final String PROJECT_ID = "cn2425-t1-g06";
    private static final String ZONE = "europe-southwest1-a";
    private static Compute compute;

    public static void main(String[] args) throws Exception {
        authenticate();
        gRPCServerInstanceGroup();
        landmarksAppInstanceGroup();
    }

    private static void authenticate() throws Exception {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream("aaaaaaaaaaaaa"))     //meter path do json da key p autenticacao
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

        compute = new Compute.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("gcp-java-mig")
                .build();
    }

    public static void gRPCServerInstanceGroup() throws Exception {
        String templateName = "grpc-server-template";
        String groupName = "grpc-server-group";
        String instanceNamePrefix = "grpc-server";

        InstanceTemplate instanceTemplate = new InstanceTemplate()
                .setName(templateName)
                .setProperties(new InstanceProperties()
                        .setMachineType("e2-medium")
                        .setDisks(List.of(new AttachedDisk()
                                .setBoot(true)
                                .setAutoDelete(true)
                                .setInitializeParams(new AttachedDiskInitializeParams()
                                        .setSourceImage("projects/debian-cloud/global/images/family/debian-11")
                                        .setDiskSizeGb(10L))))
                        .setNetworkInterfaces(List.of(new NetworkInterface()
                                .setNetwork("global/networks/default")))
                );

        compute.instanceTemplates().insert(PROJECT_ID, instanceTemplate).execute();

        InstanceGroupManager instanceGroupManager = new InstanceGroupManager()
                .setName(groupName)
                .setBaseInstanceName(instanceNamePrefix)
                .setInstanceTemplate(String.format("global/instanceTemplates/%s", templateName))
                .setTargetSize(1);

        compute.instanceGroupManagers().insert(PROJECT_ID, ZONE, instanceGroupManager).execute();

        Autoscaler autoscaler = new Autoscaler()
                .setName(groupName + "-autoscaler")
                .setTarget(String.format("zones/%s/instanceGroupManagers/%s", ZONE, groupName))
                .setAutoscalingPolicy(new AutoscalingPolicy()
                        .setMinNumReplicas(1)
                        .setMaxNumReplicas(3)
                        .setCpuUtilization(new AutoscalingPolicyCpuUtilization().setUtilizationTarget(0.6)));

        compute.autoscalers().insert(PROJECT_ID, ZONE, autoscaler).execute();

    }

    public static void landmarksAppInstanceGroup() throws Exception {
        String workerTemplateName = "landmarks-worker-template";
        String workerGroupName = "landmarks-worker-group";
        String workerInstancePrefix = "landmarks-worker";

        InstanceTemplate workerTemplate = new InstanceTemplate()
                .setName(workerTemplateName)
                .setProperties(new InstanceProperties()
                        .setMachineType("e2-medium")
                        .setDisks(List.of(new AttachedDisk()
                                .setBoot(true)
                                .setAutoDelete(true)
                                .setInitializeParams(new AttachedDiskInitializeParams()
                                        .setSourceImage("projects/debian-cloud/global/images/family/debian-11")
                                        .setDiskSizeGb(10L))))
                        .setNetworkInterfaces(List.of(new NetworkInterface()
                                .setNetwork("global/networks/default")))
                );

        compute.instanceTemplates().insert(PROJECT_ID, workerTemplate).execute();

        InstanceGroupManager workerGroupManager = new InstanceGroupManager()
                .setName(workerGroupName)
                .setBaseInstanceName(workerInstancePrefix)
                .setInstanceTemplate(String.format("global/instanceTemplates/%s", workerTemplateName))
                .setTargetSize(0);

        compute.instanceGroupManagers().insert(PROJECT_ID, ZONE, workerGroupManager).execute();

        Autoscaler workerAutoscaler = new Autoscaler()
                .setName(workerGroupName + "-autoscaler")
                .setTarget(String.format("zones/%s/instanceGroupManagers/%s", ZONE, workerGroupName))
                .setAutoscalingPolicy(new AutoscalingPolicy()
                        .setMinNumReplicas(0)
                        .setMaxNumReplicas(2)
                        .setCpuUtilization(new AutoscalingPolicyCpuUtilization().setUtilizationTarget(0.6)));

        compute.autoscalers().insert(PROJECT_ID, ZONE, workerAutoscaler).execute();
    }
}
