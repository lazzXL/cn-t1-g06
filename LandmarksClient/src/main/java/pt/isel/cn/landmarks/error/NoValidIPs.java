package pt.isel.cn.landmarks.error;

public class NoValidIPs extends ClientError {
    public NoValidIPs() {
        super("No valid IPs found");
    }
}
