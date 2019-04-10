package org.opentosca.bus.management.deployment.plugin.bpel;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.opentosca.bus.management.deployment.plugin.IManagementBusDeploymentPluginService;
import org.opentosca.bus.management.deployment.plugin.bpel.util.BPELRESTLightUpdater;
import org.opentosca.bus.management.deployment.plugin.bpel.util.Messages;
import org.opentosca.bus.management.deployment.plugin.bpel.util.ODEEndpointUpdater;
import org.opentosca.bus.management.header.MBHeader;
import org.opentosca.bus.management.utils.MBUtils;
import org.opentosca.container.connector.bps.BpsConnector;
import org.opentosca.container.connector.ode.OdeConnector;
import org.opentosca.container.core.common.Settings;
import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.service.IFileAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Management Bus-Plug-in for the deployment of BPEL plans.<br>
 * <br>
 *
 * Copyright 2019 IAAS University of Stuttgart <br>
 * <br>
 *
 * This Plug-in is able to deploy and undeploy BPEL plans on either BPS or ODE. It gets a camel
 * exchange object from the Management Bus which contains all information that is needed for the
 * deployment/undeployment. <br>
 * <br>
 *
 * <b>BPS/ODE config:</b> Engine location, username and password for this Plug-in are defined in the
 * class {@link Settings} or the corresponding config.ini file.
 */
public class ManagementBusDeploymentPluginBpel implements IManagementBusDeploymentPluginService {

    // In messages.properties defined plugin types and capabilities
    private static final String TYPES = Messages.DeploymentPluginBpel_types;
    private static final String CAPABILITIES = Messages.DeploymentPluginBpel_types;

    private static final Logger LOG = LoggerFactory.getLogger(ManagementBusDeploymentPluginBpel.class);

    public static final String BPS_ENGINE = "BPS";

    // credentials
    private static final String ENGINE = Settings.BPEL_ENGINE;
    private static final String ENGINE_URL = Settings.BPEL_ENGINE_URL;
    private static final String ENGINE_SERVICES_URL = Settings.BPEL_ENGINE_SERVICES_URL;
    private static final String ENGINE_USER = Settings.BPEL_ENGINE_USERNAME;
    private static final String ENGINE_PW = Settings.BPEL_ENGINE_PASSWORD;

    private IFileAccessService fileAccessService = null;

    @Override
    public Exchange invokeDeployment(final Exchange exchange) {

        LOG.debug("Trying to deploy BPEL plan.");
        final Message message = exchange.getIn();

        @SuppressWarnings("unchecked")
        final List<String> artifactReferences =
            message.getHeader(MBHeader.ARTIFACTREFERENCES_LISTSTRING.toString(), List.class);
        final CSARID csarID = message.getHeader(MBHeader.CSARID.toString(), CSARID.class);

        // get location of the BPEL zip file
        if (Objects.nonNull(artifactReferences) && !artifactReferences.isEmpty()) {
            final String planLocation = artifactReferences.get(0);

            // download and store BPEL zip temporarily
            final File bpelFile = MBUtils.getFile(planLocation, "zip");
            if (Objects.isNull(bpelFile)) {
                LOG.error("Retrieved file is null.");
                return exchange;
            }
            LOG.debug("Successfully retrieved BPEL file from URL");

            if (Objects.isNull(this.fileAccessService)) {
                LOG.error("FileAccessService is not available, can't create needed temporary space on disk");
                return exchange;
            }

            // unzip plan to temp directory for some endpoint updates
            final File tempDir = this.fileAccessService.getTemp();
            File tempPlan = new File(tempDir, bpelFile.getName());
            LOG.debug("Unzipping Plan '{}' to '{}'.", bpelFile.getName(), tempDir.getAbsolutePath());
            final List<File> planContents = this.fileAccessService.unzip(bpelFile, tempDir);

            // changing endpoints in WSDLs
            QName portType = null;
            try {
                final ODEEndpointUpdater odeUpdater = new ODEEndpointUpdater(ENGINE_SERVICES_URL, ENGINE);
                portType = odeUpdater.getPortType(planContents);
                if (!odeUpdater.changeEndpoints(planContents, csarID)) {
                    LOG.error("Not all endpoints used by the plan have been changed");
                }
            }
            catch (final WSDLException e) {
                LOG.error("Couldn't load ODEEndpointUpdater: {}", e);
            }

            // update the bpel and bpel4restlight elements (ex.: GET, PUT,..)
            try {
                final BPELRESTLightUpdater bpelRestUpdater = new BPELRESTLightUpdater();
                if (!bpelRestUpdater.changeEndpoints(planContents, csarID)) {
                    // we don't abort deployment here
                    LOG.warn("Could'nt change all endpoints inside BPEL4RESTLight Elements in the given process {}");
                }
            }
            catch (final TransformerConfigurationException | ParserConfigurationException e) {
                LOG.error("Couldn't load BPELRESTLightUpdater", e);
            }
            catch (final SAXException e) {
                LOG.error("ParseError: Couldn't parse .bpel file", e);
            }
            catch (final IOException e) {
                LOG.error("IOError: Couldn't access .bpel file", e);
            }

            // package the updated files
            try {
                if (tempPlan.createNewFile()) {
                    LOG.debug("Packaging plan to {} ", tempPlan.getAbsolutePath());
                    tempPlan = this.fileAccessService.zip(tempDir, tempPlan);
                } else {
                    LOG.error("Can't package temporary plan for deployment");
                    return exchange;
                }
            }
            catch (final IOException e) {
                LOG.error("Can't package temporary plan for deployment", e);
                return exchange;
            }

            // deploy process
            LOG.info("Deploying Plan: {}", tempPlan.getName());
            String processId = "";
            Map<String, URI> endpoints = Collections.emptyMap();
            try {
                if (ENGINE.equalsIgnoreCase(BPS_ENGINE)) {
                    final BpsConnector connector = new BpsConnector();

                    processId = connector.deploy(tempPlan, ENGINE_URL, ENGINE_USER, ENGINE_PW);
                    endpoints = connector.getEndpointsForPID(processId, ENGINE_URL, ENGINE_USER, ENGINE_PW);
                } else {
                    final OdeConnector connector = new OdeConnector();

                    processId = connector.deploy(tempPlan, ENGINE_URL);
                    endpoints = connector.getEndpointsForPID(processId, ENGINE_URL);
                }
            }
            catch (final Exception e) {
                e.printStackTrace();
            }

            // get endpoint of the deployed plan
            URI endpoint = null;
            if (endpoints.keySet().size() == 1) {
                endpoint = (URI) endpoints.values().toArray()[0];
            } else {
                for (final String partnerLink : endpoints.keySet()) {
                    if (partnerLink.equals("client")) {
                        endpoint = endpoints.get(partnerLink);
                    }
                }
            }

            if (Objects.isNull(endpoint)) {
                LOG.warn("No endpoint could be determined, container won't be able to instantiate it");
                return exchange;
            }

            // set needed headers to store and access plan endpoint
            message.setHeader(MBHeader.ENDPOINT_URI.toString(), endpoint);
            message.setHeader(MBHeader.PORTTYPE_QNAME.toString(), portType);
        } else {
            LOG.error("No artifact reference defined for plan deployment. Aborting...");
        }

        return exchange;
    }

    @Override
    public Exchange invokeUndeployment(final Exchange exchange) {

        LOG.debug("Trying to undeploy BPEL plan.");
        final Message message = exchange.getIn();

        // set operation state to false and only change after successful undeployment
        message.setHeader(MBHeader.OPERATIONSTATE_BOOLEAN.toString(), false);

        @SuppressWarnings("unchecked")
        final List<String> artifactReferences =
            message.getHeader(MBHeader.ARTIFACTREFERENCES_LISTSTRING.toString(), List.class);

        // get location of the BPEL zip file
        if (Objects.nonNull(artifactReferences) && !artifactReferences.isEmpty()) {
            final String planLocation = artifactReferences.get(0);

            // download and store BPEL zip temporarily
            final File bpelFile = MBUtils.getFile(planLocation, "zip");
            if (Objects.isNull(bpelFile)) {
                LOG.error("Retrieved file is null.");
                return exchange;
            }
            LOG.debug("Successfully retrieved BPEL file from URL");

            boolean wasUndeployed = false;
            if (ENGINE.equalsIgnoreCase(BPS_ENGINE)) {
                final BpsConnector connector = new BpsConnector();
                wasUndeployed = connector.undeploy(bpelFile, ENGINE_URL, ENGINE_USER, ENGINE_PW);
            } else {
                final OdeConnector connector = new OdeConnector();
                wasUndeployed = connector.undeploy(bpelFile, ENGINE_URL);
            }

            if (wasUndeployed) {
                LOG.debug("Undeployment successfully");
                message.setHeader(MBHeader.OPERATIONSTATE_BOOLEAN.toString(), true);
            } else {
                LOG.warn("Undeployment not successful");
            }
        } else {
            LOG.error("No artifact reference defined for undeployment. Aborting...");
        }

        return exchange;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public List<String> getSupportedTypes() {
        LOG.debug("Getting Types: {}.", ManagementBusDeploymentPluginBpel.TYPES);
        final List<String> types = new ArrayList<>();

        for (final String type : ManagementBusDeploymentPluginBpel.TYPES.split("[,;]")) {
            types.add(type.trim());
        }
        return types;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public List<String> getCapabilties() {
        LOG.debug("Getting Plugin-Capabilities: {}.", ManagementBusDeploymentPluginBpel.CAPABILITIES);
        final List<String> capabilities = new ArrayList<>();

        for (final String capability : ManagementBusDeploymentPluginBpel.CAPABILITIES.split("[,;]")) {
            capabilities.add(capability.trim());
        }
        return capabilities;
    }

    /**
     * Bind method for IFileAccessServices
     *
     * @param fileAccessService the fileAccessService to bind
     */
    public void registerFileAccessService(final IFileAccessService fileAccessService) {
        LOG.debug("Registering FileAccessService {}", fileAccessService.toString());
        if (Objects.nonNull(fileAccessService)) {
            this.fileAccessService = fileAccessService;
        }
    }

    /**
     * Unbind method for IFileAccessServices
     *
     * @param fileAccessService the fileAccessService to unbind
     */
    protected void unregisterFileAccessService(final IFileAccessService fileAccessService) {
        LOG.debug("Unregistering FileAccessService {}", fileAccessService.toString());
        this.fileAccessService = null;
    }
}
