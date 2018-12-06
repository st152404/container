package org.opentosca.bus.management.discovery.plugin.raspberrypi.model;

/**
 * Pair of one MAC address and one IP address which can be extracted from a topology consisting of a
 * RaspbianJessie NodeTemplate and a RaspberryPi3 NodeTemplate. These addresses can be used to
 * identify a physical Raspberry Pi.
 */
public class MacIpPair {

    private final String mac;
    private final String ip;

    public MacIpPair(final String mac, final String ip) {
        this.mac = mac;
        this.ip = ip;
    }

    public String getMac() {
        return this.mac;
    }

    public String getIp() {
        return this.ip;
    }
}
