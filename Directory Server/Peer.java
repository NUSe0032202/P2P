import java.util.Objects;

public class Peer {
    private String hostname;
    private String publicIPAddress;
    private int publicPortNumber;
    private String privateIPAddress;
    private int privatePortNumber;

    private Peer () {}

    public Peer(String hostname, String publicIP, int publicPort, String privateIP, int privatePort) {
        this.hostname = hostname;
        this.publicIPAddress = publicIP;
        this.publicPortNumber = publicPort;
        this.privateIPAddress = privateIP;
        this.privatePortNumber = privatePort;
    }

    public Peer(String publicIP, int publicPort, String privateIP, int privatePort) {
        this.hostname = "";
        this.publicIPAddress = publicIP;
        this.publicPortNumber = publicPort;
        this.privateIPAddress = privateIP;
        this.privatePortNumber = privatePort;
    }

    public String getHostname() {
        return hostname;
    }

    public String getPublicIpAddress() {
        return publicIPAddress;
    }

    public int getPublicPortNumber() {
        return publicPortNumber;
    }

    public String getPrivateIpAddress() {
        return privateIPAddress;
    }

    public int getPrivatePortNumber() {
        return privatePortNumber;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer cmp = (Peer) o;
        return Objects.equals(publicIPAddress, cmp.publicIPAddress) &&
                 Objects.equals(publicPortNumber,cmp.publicPortNumber) &&
                   Objects.equals(privateIPAddress,cmp.privateIPAddress) &&
                      Objects.equals(privatePortNumber,cmp.privatePortNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, publicIPAddress, publicPortNumber, privateIPAddress, privatePortNumber);
    }
}
