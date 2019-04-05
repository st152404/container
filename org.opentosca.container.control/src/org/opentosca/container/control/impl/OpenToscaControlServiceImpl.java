package org.opentosca.container.control.impl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.opentosca.bus.management.service.impl.ManagementBusServiceImpl;
import org.opentosca.container.control.IOpenToscaControlService;
import org.opentosca.container.core.common.Settings;
import org.opentosca.container.core.common.SystemException;
import org.opentosca.container.core.common.UserException;
import org.opentosca.container.core.engine.IToscaEngineService;
import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.model.deployment.process.DeploymentProcessState;
import org.opentosca.container.core.service.ICoreDeploymentTrackerService;
import org.opentosca.container.core.service.ICoreEndpointService;
import org.opentosca.container.core.service.ICoreFileService;
import org.opentosca.container.core.service.IPlanInvocationEngine;
import org.opentosca.container.core.tosca.extension.TPlanDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The instance of this interface is used by org.opentosca.container.api which invokes each step in
 * the deployment process. For handling the states of processing of each CSAR, this component uses
 * the org.opentosca.core.deployment.tracker.service.ICoreDeploymentTrackerService to read and set
 * the current state of a certain CSAR and provides a HashSet with the possible process invocations
 * for a certain CSAR.
 */
public class OpenToscaControlServiceImpl implements IOpenToscaControlService {

    private static ICoreFileService fileService = null;
    private static IToscaEngineService toscaEngine = null;
    private static ICoreDeploymentTrackerService coreDeploymentTracker = null;
    private static ICoreEndpointService endpointService = null;
    private static IPlanInvocationEngine planInvocationEngine = null;

    private final Logger LOG = LoggerFactory.getLogger(OpenToscaControlServiceImpl.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean invokeTOSCAProcessing(final CSARID csarID) {

        this.LOG.debug("Start the resolving of the ServiceTemplates of the CSAR \"" + csarID + "\".");
        OpenToscaControlServiceImpl.coreDeploymentTracker.storeDeploymentState(csarID,
                                                                               DeploymentProcessState.TOSCAPROCESSING_ACTIVE);

        // start the resolving and store the state according to success
        if (OpenToscaControlServiceImpl.toscaEngine.resolveDefinitions(csarID)) {
            this.LOG.info("Processing of the Definitions completed.");
            OpenToscaControlServiceImpl.coreDeploymentTracker.storeDeploymentState(csarID,
                                                                                   DeploymentProcessState.TOSCA_PROCESSED);
        } else {
            this.LOG.error("Processing of the Definitions failed!");
            OpenToscaControlServiceImpl.coreDeploymentTracker.storeDeploymentState(csarID,
                                                                                   DeploymentProcessState.STORED);
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedEncodingException
     */
    @Override
    public String invokePlanInvocation(final CSARID csarID, final QName serviceTemplateId, final long csarInstanceID,
                                       final TPlanDTO plan) throws UnsupportedEncodingException {

        this.LOG.info("Invoke Plan Invocation!");

        final String correlationID =
            OpenToscaControlServiceImpl.planInvocationEngine.createCorrelationId(csarID, serviceTemplateId,
                                                                                 csarInstanceID, plan);

        if (null != correlationID) {
            this.LOG.info("The Plan Invocation was successfull!!!");
            OpenToscaControlServiceImpl.planInvocationEngine.invokePlan(csarID, serviceTemplateId, csarInstanceID, plan,
                                                                        correlationID);
        } else {
            this.LOG.error("The Plan Invocation was not successfull!!!");
            return null;
        }

        return correlationID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> deleteCSAR(final CSARID csarID) {

        final List<String> errors = new ArrayList<>();

        // TODO following code should be active again
        // if
        // (!OpenToscaControlServiceImpl.instanceManagement.getInstancesOfCSAR(csarID).isEmpty())
        // {
        // // There are instances, thus deletion is not legal.
        // LOG.error("CSAR \"{}\" has instances.", csarID);
        // errors.add("CSAR has instances.");
        // return errors;
        // }

        if (!ManagementBusServiceImpl.deleteAllPlans(csarID)) {
            this.LOG.warn("It was not possible to undeploy all plans of the CSAR \"" + csarID + ".");
            errors.add("Could not undeploy all plans.");
        }

        // Delete operation is legal, thus continue.
        if (!OpenToscaControlServiceImpl.toscaEngine.clearCSARContent(csarID)) {
            this.LOG.error("It was not possible to delete all content of the CSAR \"" + csarID
                + "\" inside the ToscaEngine.");
            errors.add("Could not delete TOSCA data.");
        }

        OpenToscaControlServiceImpl.coreDeploymentTracker.deleteDeploymentState(csarID);

        // Delete all plan endpoints related to this CSAR. IA endpoints are undeployed and deleted
        // by the Management Bus.
        OpenToscaControlServiceImpl.endpointService.removePlanEndpoints(Settings.OPENTOSCA_CONTAINER_HOSTNAME, csarID);

        try {
            OpenToscaControlServiceImpl.fileService.deleteCSAR(csarID);
        }
        catch (SystemException | UserException e) {
            this.LOG.error("The file service could not delete all data of the CSAR \"{}\". ", csarID, e);
            errors.add("Could not delete CSAR files.");
        }

        if (errors.isEmpty()) {
            this.LOG.info("Contents of CSAR \"" + csarID + "\" deleted.");
        } else {
            String errorList = "";
            for (final String err : errors) {
                errorList = errorList + err + "\\n";
            }
            this.LOG.error("Errors while deleting: " + errorList);
        }

        return errors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean setDeploymentProcessStateStored(final CSARID csarID) {
        this.LOG.trace("Setting CSAR {} to state \"{}\"", csarID, DeploymentProcessState.STORED.name());
        return OpenToscaControlServiceImpl.coreDeploymentTracker.storeDeploymentState(csarID,
                                                                                      DeploymentProcessState.STORED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeploymentProcessState getDeploymentProcessState(final CSARID csarID) {
        return OpenToscaControlServiceImpl.coreDeploymentTracker.getDeploymentState(csarID);
    }

    protected void bindFileService(final ICoreFileService service) {
        if (service == null) {
            this.LOG.error("Service FileService is null.");
        } else {
            this.LOG.debug("Bind of the FileService.");
            OpenToscaControlServiceImpl.fileService = service;
        }
    }

    protected void unbindFileService(final ICoreFileService service) {
        this.LOG.debug("Unbind of the FileService.");
        OpenToscaControlServiceImpl.fileService = null;
    }

    protected void bindToscaEngine(final IToscaEngineService service) {
        if (service == null) {
            this.LOG.error("Service ToscaEngine is null.");
        } else {
            this.LOG.debug("Bind of the ToscaEngine.");
            OpenToscaControlServiceImpl.toscaEngine = service;
        }
    }

    protected void unbindToscaEngine(final IToscaEngineService service) {
        this.LOG.debug("Unbind of the ToscaEngine.");
        OpenToscaControlServiceImpl.toscaEngine = null;
    }

    protected void bindDeploymentTrackerService(final ICoreDeploymentTrackerService service) {
        if (service == null) {
            this.LOG.error("Service CoreDeploymentTracker is null.");
        } else {
            this.LOG.debug("Bind of the Core Deployment Tracker.");
            OpenToscaControlServiceImpl.coreDeploymentTracker = service;
        }
    }

    protected void unbindDeploymentTrackerService(final ICoreDeploymentTrackerService service) {
        this.LOG.debug("Unbind of the Core Deployment Tracker.");
        OpenToscaControlServiceImpl.coreDeploymentTracker = null;
    }

    protected void bindEndpointService(final ICoreEndpointService service) {
        if (service == null) {
            this.LOG.error("Service ICoreEndpointService is null.");
        } else {
            this.LOG.debug("Bind of the ICoreEndpointService.");
            OpenToscaControlServiceImpl.endpointService = service;
        }
    }

    protected void unbindEndpointService(final ICoreEndpointService service) {
        this.LOG.debug("Unbind of the ICoreEndpointService.");
        OpenToscaControlServiceImpl.endpointService = null;
    }

    protected void bindPlanInvocationEngine(final IPlanInvocationEngine service) {
        if (service == null) {
            this.LOG.error("Service planInvocationEngine is null.");
        } else {
            this.LOG.debug("Bind of the planInvocationEngine.");
            OpenToscaControlServiceImpl.planInvocationEngine = service;
        }
    }

    protected void unbindPlanInvocationEngine(final IPlanInvocationEngine service) {
        this.LOG.debug("Unbind of the planInvocationEngine.");
        OpenToscaControlServiceImpl.planInvocationEngine = null;
    }
}
