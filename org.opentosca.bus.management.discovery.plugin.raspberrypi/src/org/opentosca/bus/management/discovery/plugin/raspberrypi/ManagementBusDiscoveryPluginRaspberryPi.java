package org.opentosca.bus.management.discovery.plugin.raspberrypi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.opentosca.bus.management.discovery.plugin.IManagementBusDiscoveryPluginService;
import org.opentosca.container.core.common.Settings;
import org.opentosca.container.core.tosca.convention.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Management Bus Plug-in for the discovery of RaspberryPi nodes.<br>
 * <br>
 *
 * This plug-in can be used to discover devices that correspond to NodeTemplates of NodeType
 * RaspberryPI3. It needs the MAC-address property of the NodeTemplate as input and performs a
 * discovery of all MAC-addresses that are in the local network of the host where this OpenTOSCA
 * Container runs on.
 *
 * Copyright 2018 IAAS University of Stuttgart
 */
public class ManagementBusDiscoveryPluginRaspberryPi implements IManagementBusDiscoveryPluginService {

    static final private Logger LOG = LoggerFactory.getLogger(ManagementBusDiscoveryPluginRaspberryPi.class);

    // all NodeTypes that can be detected by this plug-in
    static final private List<QName> supportedTypes = Arrays.asList(Types.raspberryPI3NodeType);

    // expected properties for NodeTemplates of the supported NodeTypes
    static final private String macProperty = "MAC";
    static final private List<String> expectedProperties = Arrays.asList(macProperty);

    // prefix of the nmap response lines which contain MAC addresses
    static final private String NMAP_LINE_PREFIX_MAC = "MAC Address:";

    @Override
    public boolean invokeNodeTemplateDiscovery(final QName nodeType, final Map<String, String> properties) {
        LOG.debug("RaspberryPi discovery plug-in invoked for NodeType: {} and Properties: {}", nodeType, properties);

        if (!supportedTypes.contains(nodeType)) {
            LOG.error("Discovery plug-in invoked for invalid NodeType.");
            return false;
        }

        if (!allPropertiesAvailable(properties.keySet())) {
            LOG.error("Given properties do not contain all expected propteries.");
            return false;
        }

        LOG.debug("Performing device discovery with local IP: {}", Settings.OPENTOSCA_CONTAINER_HOSTNAME);

        // search for a device with a matching MAC address
        final List<String> macAddresses = getMacOfLocalDevices(Settings.OPENTOSCA_CONTAINER_HOSTNAME);
        if (macAddresses.contains(properties.get(macProperty))) {
            // TODO: check if MAC address belongs to a Pi required?

            LOG.debug("Found device with matching MAC address locally.");
            return true;
        } else {
            LOG.debug("No device with matching MAC address found locally.");
            return false;
        }
    }

    @Override
    public List<QName> getSupportedNodeTypes() {
        return supportedTypes;
    }

    /**
     * Checks if the given properties contain all expected properties of the plug-in. Passing more
     * properties than expected is allowed too.
     *
     * @param properties the properties to check
     * @return <tt>true</tt> if all expected properties are defined in the given properties,
     *         <tt>false</tt> otherwise.
     */
    private boolean allPropertiesAvailable(final Set<String> properties) {
        return expectedProperties.stream().filter(prop -> !properties.contains(prop)).collect(Collectors.toList())
                                 .isEmpty();
    }

    /**
     * Get a List of the MAC addresses of all devices in the local network which is identified by
     * the given IP.
     *
     * @param ip the IP address of the device running the JVM
     * @return A List of Strings which represent the MAC addresses of the discovered devices.
     */
    private static List<String> getMacOfLocalDevices(final String ip) {
        final List<String> macAddresses = new ArrayList<>();

        try {
            // Create subnet mask for the local network. Here we assume private networks of class C,
            // which means with a maximum of 256 hosts and therefore we use only a wildcard for the
            // last digit of the IP address.
            final String subnetMask = ip.substring(0, ip.lastIndexOf(".")) + ".*";
            LOG.debug("Subnet mask for device discovery in local network: {}", subnetMask);

            // run nmap to get all MAC addresses of devices in the local network
            final Process p = Runtime.getRuntime().exec(new String[] {"nmap", "-sn", subnetMask});
            final BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                // retrieve MAC from textual output of nmap
                if (line.startsWith(NMAP_LINE_PREFIX_MAC)) {
                    final String macAddress = line.split("\\s+")[2];
                    LOG.debug("Found MAC address: {}", macAddress);
                    macAddresses.add(macAddress);
                }
            }
            in.close();
        }
        catch (final Exception e) {
            LOG.error("Exception while retrieving the MAC addresses of all local devices: {}", e.getMessage());
        }

        return macAddresses;
    }
}
