package org.opentosca.bus.management.deployment.plugin.camunda;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.opentosca.bus.management.deployment.plugin.IManagementBusDeploymentPluginService;
import org.opentosca.bus.management.deployment.plugin.camunda.iaenginecopies.CopyOfIAEnginePluginWarTomcatServiceImpl;
import org.opentosca.bus.management.deployment.plugin.camunda.util.Messages;
import org.opentosca.bus.management.header.MBHeader;
import org.opentosca.bus.management.utils.MBUtils;
import org.opentosca.container.core.engine.IToscaEngineService;
import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.service.IHTTPService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Management Bus-Plug-in for the deployment of BPMN plans on the camunda engine.<br>
 * <br>
 *
 * Copyright 2019 IAAS University of Stuttgart <br>
 * <br>
 *
 * This Plug-in is able to deploy and undeploy BPMN plans on the camunda engine. It gets a camel
 * exchange object from the Management Bus which contains all information that is needed for the
 * deployment/undeployment. <br>
 * <br>
 *
 * TODO: Should be rewritten if it is kept in the project (remove duplicate code, configure via
 * config.ini and no hard coded addresses, implement undeployment logic, ...)
 */
public class ManagementBusDeploymentPluginCamunda implements IManagementBusDeploymentPluginService {

    // In messages.properties defined plugin types and capabilities
    private static final String TYPES = Messages.DeploymentPluginCamunda_types;
    private static final String CAPABILITIES = Messages.DeploymentPluginCamunda_capabilities;

    private static final Logger LOG = LoggerFactory.getLogger(ManagementBusDeploymentPluginCamunda.class);

    private IToscaEngineService toscaEngineService = null;

    @Override
    public Exchange invokeDeployment(final Exchange exchange) {

        LOG.debug("Trying to deploy BPMN plan.");
        final Message message = exchange.getIn();

        @SuppressWarnings("unchecked")
        final List<String> artifactReferences =
            message.getHeader(MBHeader.ARTIFACTREFERENCES_LISTSTRING.toString(), List.class);
        final CSARID csarID = message.getHeader(MBHeader.CSARID.toString(), CSARID.class);
        final QName planID = message.getHeader(MBHeader.PLANID_QNAME.toString(), QName.class);

        // get location of the BPEL zip file
        if (Objects.nonNull(artifactReferences) && !artifactReferences.isEmpty()) {
            final String planLocation = artifactReferences.get(0);

            // download and store BPMN WAR temporarily
            final File bpmnFile = MBUtils.getFile(planLocation, "war");
            if (Objects.isNull(bpmnFile)) {
                LOG.error("Retrieved file is null.");
                return exchange;
            }
            LOG.debug("Successfully retrieved BPMN file from URL");

            // ##################################################################################################################################################
            // ### dirty copy of IAEngine War Tomcat Plugin
            // ### TODO make this pretty
            // ##################################################################################################################################################

            final CopyOfIAEnginePluginWarTomcatServiceImpl deployer = new CopyOfIAEnginePluginWarTomcatServiceImpl();
            deployer.deployImplementationArtifact(csarID, bpmnFile);
            // POST http://localhost:8080/engine-rest/process-definition/{id}/start
            URI endpointURI = null;
            try {
                final String planName = this.toscaEngineService.getPlanName(csarID, planID);
                final int retries = 100;

                for (int iteration = retries; iteration > 0; iteration--) {
                    endpointURI = searchForEndpoint(planName);

                    if (null == endpointURI) {
                        try {
                            LOG.debug("Endpoint not set yet, Camunda might be still processing it.");
                            Thread.sleep(1000);
                        }
                        catch (final InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        break;
                    }
                }
                LOG.debug("Endpoint URI is {}", endpointURI.getPath());
            }
            catch (final URISyntaxException e) {
                e.printStackTrace();
            }
            catch (final NullPointerException e) {

            }

            // ##################################################################################################################################################
            // ##################################################################################################################################################


            // set needed headers to store and access plan endpoint
            message.setHeader(MBHeader.ENDPOINT_URI.toString(), endpointURI);
            message.setHeader(MBHeader.PORTTYPE_QNAME.toString(), planID);
        } else {
            LOG.error("No artifact reference defined for plan deployment. Aborting...");
        }

        return exchange;
    }

    @Override
    public Exchange invokeUndeployment(final Exchange exchange) {

        LOG.debug("Trying to undeploy BPMN plan.");
        final Message message = exchange.getIn();

        // set operation state to false and only change after successful undeployment
        message.setHeader(MBHeader.OPERATIONSTATE_BOOLEAN.toString(), false);

        LOG.warn("The undeploy method for the Camunda plan engine is not implemented yet.");

        return exchange;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public List<String> getSupportedTypes() {
        LOG.debug("Getting Types: {}.", ManagementBusDeploymentPluginCamunda.TYPES);
        final List<String> types = new ArrayList<>();

        for (final String type : ManagementBusDeploymentPluginCamunda.TYPES.split("[,;]")) {
            types.add(type.trim());
        }
        return types;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public List<String> getCapabilties() {
        LOG.debug("Getting Plugin-Capabilities: {}.", ManagementBusDeploymentPluginCamunda.CAPABILITIES);
        final List<String> capabilities = new ArrayList<>();

        for (final String capability : ManagementBusDeploymentPluginCamunda.CAPABILITIES.split("[,;]")) {
            capabilities.add(capability.trim());
        }
        return capabilities;
    }

    private URI searchForEndpoint(final String planName) throws URISyntaxException {
        URI endpointURI;
        LOG.debug("Search for Plan Endpoint");

        final String processDefinitions = "http://localhost:8080/engine-rest/process-definition/";

        IHTTPService httpService;
        final BundleContext context = Activator.getContext();
        final ServiceReference<IHTTPService> tmpHttpService = context.getServiceReference(IHTTPService.class);
        httpService = context.getService(tmpHttpService);

        HttpResponse response;
        String output = null;

        LOG.debug("Retrieve list of deployed plans");
        try {
            response = httpService.Get(processDefinitions);
            output = EntityUtils.toString(response.getEntity(), "UTF-8");
            output = output.substring(1, output.length() - 1);
        }
        catch (final IOException e) {
            LOG.error("An error occured while retrieving the deployed plan list from camunda: ",
                      e.getLocalizedMessage());
            e.printStackTrace();
            return null;
        }
        final String json = output;

        LOG.trace("Response json: {}", json);

        final String[] list = json.split("\\{");

        final HashMap<String, String> ids = new HashMap<>();

        for (final String entry : list) {
            if (null != entry && !entry.equals("")) {
                final String[] fields = entry.split(",");

                final String id = fields[0].substring(6, fields[0].length() - 1);
                final String key = fields[1].substring(7, fields[1].length() - 1);

                ids.put(id, key);
                LOG.trace("ID {} KEY {}", id, key);
            }
        }

        String planID = "";

        if (ids.containsValue(planName)) {
            for (final String id : ids.keySet()) {
                if (ids.get(id).equals(planName)) {
                    planID = ids.get(id);
                }
            }
        }

        if (planID.equals("")) {
            LOG.warn("No endpoint found for plan {}!", planName);
            return null;
        }

        endpointURI = new URI(processDefinitions + "key/" + planID + "/start");
        return endpointURI;
    }

    /**
     * Bind method for IToscaEngineService
     *
     * @param toscaEngineService the toscaEngineService to bind
     */
    public void registerFileAccessService(final IToscaEngineService toscaEngineService) {
        LOG.debug("Registering IToscaEngineService {}", toscaEngineService.toString());
        if (Objects.nonNull(toscaEngineService)) {
            this.toscaEngineService = toscaEngineService;
        }
    }

    /**
     * Unbind method for IToscaEngineService
     *
     * @param toscaEngineService the toscaEngineService to unbind
     */
    protected void unregisterFileAccessService(final IToscaEngineService toscaEngineService) {
        LOG.debug("Unregistering IToscaEngineService {}", toscaEngineService.toString());
        this.toscaEngineService = null;
    }
}
