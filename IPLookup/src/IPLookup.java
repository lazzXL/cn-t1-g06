import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

/**
 * This class implements a Google Cloud Function that retrieves the IP addresses of instances in a given instance group.
 */
public class IPLookupFunction implements HttpFunction {
    private static final String PROJECT_ID = "CN2425-T1-G06";
    private static final String INSTANCE_GROUP = "";
    private static final String REGION = "europe-";

    /**
     * This method is called when the function is invoked.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        String projectID = PROJECT_ID;
        String zone = REGION + request.getParameter("zone");
        String groupName = INSTANCE_GROUP + request.getParameter("groupName");

        try {
            List<String> ipList = listIpInstancesFromGroup(projectID, zone, groupName);
            response.setContentType("application/json");
            response.getWriter().write(new Gson().toJson(ipList));
        } catch (IOException e) {
            response.setStatusCode(500);
            response.getWriter().write("Error: " + e.getMessage());
        }
    }
    /**
     * This method lists all the IP addresses of instances in a given instance group.
     *
     * @param projectID The project ID.
     * @param zone The zone where the instances are located.
     * @param groupName The name of the instance group.
     * @return A list of IP addresses of the instances in the group.
     */
    void List<String> listIpInstancesFromGroup(String projectID, String zone, String groupName) {
        private List<String> ipList = new ArrayList<>();
        try (InstancesClient client = InstancesClient.create()) {
            for (Instance curInst : client.list(projectID, zone).iterateAll()) {
                if (curInst.getName().contains(groupName)) {
                    String ip = curInst.getNetworkInterfaces(0).getAccessConfigs(0).getNatIP();
                    ipList.add(ip);
                }
            }
        } catch (IOException e) {
            throw e
        }
        return ipList;
    }

}