import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import pt.isel.cn.landmarks.domain.Either;
import pt.isel.cn.iplookup.error.IPLookupError;
import pt.isel.cn.iplookup.error.InvalidParameterError;
import pt.isel.cn.iplookup.error.InstanceGroupNotFoundError;

/**
 * This class implements a Google Cloud Function that retrieves the IP addresses of instances in a given instance group.
 */
public class IPLookupFunction implements HttpFunction {
    private static final String PROJECT_ID = "CN2425-T1-G06";
    private static final String INSTANCE_GROUP = "";
    private static final String REGION = "europe-";


    /**
     * This method validates the parameters from the HTTP request.
     *
     * @param request The HTTP request.
     * @return Either an error or a pair of zone and group name.
     */
    Either<InvalidParameterError, Pair<String, String>> validateParameters(HttpRequest request) {
        String zone = request.getParameter("zone");
        String groupName = request.getParameter("groupName");

        if (zone == null || groupName == null) {
            return Either.left(new InvalidParameterError());
        }

        return Either.right(new Pair<>(zone, groupName));
    }
    
    /**
     * This method is called when the function is invoked.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        Either<InvalidParameterError, Pair<String, String>> params = validateParameters(request);
        if(params.isLeft()) {
            response.setStatusCode(400);
            response.getWriter().write("Error: " + params.getLeft().getMessage());
            return;
        }

        String zone = params.getRight().getFirst();
        String groupName = params.getRight().getSecond();

        Either<IPLookupError, List<String>> result = listIpInstancesFromGroup(PROJECT_ID, fullZone, fullGroupName);

        if (result.isLeft()) {
            response.setStatusCode(500);
            response.getWriter().write("Error: " + result.getLeft().getMessage());
        } else {
            response.setContentType("application/json");
            response.getWriter().write(new Gson().toJson(result.getRight()));
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
    private Either<IPLookupError, List<String>> listIpInstancesFromGroup(String projectID, String zone, String groupName) {
        List<String> ipList = new ArrayList<>();
        try (InstancesClient client = InstancesClient.create()) {
            for (Instance curInst : client.list(projectID, zone).iterateAll()) {
                if (curInst.getName().contains(groupName)) {
                    String ip = curInst.getNetworkInterfaces(0).getAccessConfigs(0).getNatIP();
                    ipList.add(ip);
                }
            }
            if (ipList.isEmpty()) {
                return Either.left(new InstanceGroupNotFoundError());
            }
            return Either.right(ipList);
        } catch (Exception e) {
            return Either.left(new IPLookupError("Failed to retrieve instances: " + e.getMessage()));
        }
    }

}