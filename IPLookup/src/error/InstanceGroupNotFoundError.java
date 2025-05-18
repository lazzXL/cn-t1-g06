public class InstanceGroupNotFoundError extends IPLookupError {
    public InstanceGroupNotFoundError() {
        super("Could not find instance group");
    }
}