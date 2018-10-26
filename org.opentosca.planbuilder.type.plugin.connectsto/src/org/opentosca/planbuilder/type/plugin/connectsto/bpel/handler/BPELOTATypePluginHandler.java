package org.opentosca.planbuilder.type.plugin.connectsto.bpel.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.opentosca.planbuilder.core.bpel.context.BPELPlanContext;
import org.opentosca.planbuilder.core.bpel.fragments.BPELProcessFragments;
import org.opentosca.planbuilder.model.plan.AbstractPlan.PlanType;
import org.opentosca.planbuilder.model.plan.bpel.BPELScopeActivity.BPELScopePhaseType;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractParameter;
import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.opentosca.planbuilder.plugins.context.Variable;
import org.opentosca.planbuilder.provphase.plugin.invoker.bpel.BPELInvokerPlugin;
import org.opentosca.planbuilder.type.plugin.connectsto.core.IoTTypePluginConstants;
import org.opentosca.planbuilder.type.plugin.connectsto.core.handler.IoTTypePluginHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains the methods to create and execute the BPEL-Plangenerator Plugin for
 * IoT-OTA-Node-Types
 *
 * @author Marc Schmid
 *
 */
public class BPELOTATypePluginHandler implements IoTTypePluginHandler<BPELPlanContext> {

    private static final Logger LOG = LoggerFactory.getLogger(BPELOTATypePluginHandler.class);
    private final BPELInvokerPlugin invokerPlugin = new BPELInvokerPlugin();
    private BPELProcessFragments planBuilderFragments;

    /**
     * Constructor for the plugin handler
     */
    public BPELOTATypePluginHandler() {
        try {
            planBuilderFragments = new BPELProcessFragments();
        }
        catch (final ParserConfigurationException e) {
            BPELOTATypePluginHandler.LOG.error("Couldn't initialize planBuilderFragments class");
            e.printStackTrace();
        }
    }

    /**
     * For the IoT-Use-Case there have been a fixed set of operations that needed to be supported. This
     * operations are: install, addBinary, updateDevice and syncTOSCAwithRollout
     *
     * For the fiven BPELPlanContext this method checks, if one of the methods should be called and
     * creates the correct BPEL-Syntax of the operations and calls than the plugin invoker
     *
     */
    @Override
    public boolean handle(final BPELPlanContext context) {
        if (context.getNodeTemplate() == null) {
            LOG.warn("Appending logic to relationshipTemplate plan is not possible by this plugin");
            return false;
        }

        if (context.getPlanType().equals(PlanType.BUILD)) {
            final List<AbstractNodeTemplate> nodeTemplates = context.getNodeTemplates();
            AbstractNodeTemplate otaManager = null;

            for (int i = 0; i < nodeTemplates.size(); i++) {
                final AbstractNodeTemplate tempNodeTemplate = nodeTemplates.get(i);
                if (ModelUtils.getNodeTypeHierarchy(tempNodeTemplate.getType())
                              .contains(IoTTypePluginConstants.OTA_Manager_NODETYPE)) {
                    otaManager = tempNodeTemplate;
                }
            }

            if (otaManager == null) {
                return false;
            }

            final String templateId = otaManager.getId();
            final boolean isNodeTemplate = true;
            final String operationName = "install";
            final String interfaceName = "http://opentosca.org/interfaces/lifecycle";
            final String callbackAddressVarName = "planCallbackAddress_invoker";
            final Map<String, Variable> internalExternalPropsInput = new HashMap<>();
            final Map<String, Variable> internalExternalPropsOutput = new HashMap<>();
            final BPELScopePhaseType phase = BPELScopePhaseType.PROVISIONING;

            internalExternalPropsInput.put("tenant", context.getPropertyVariable(otaManager, "tenant"));
            internalExternalPropsInput.put("user", context.getPropertyVariable(otaManager, "user"));
            internalExternalPropsInput.put("password", context.getPropertyVariable(otaManager, "password"));
            internalExternalPropsInput.put("host", context.getPropertyVariable(otaManager, "host"));
            internalExternalPropsInput.put("nameOfGroup", context.getPropertyVariable(otaManager, "nameOfGroup"));
            internalExternalPropsInput.put("deviceName", context.getPropertyVariable(otaManager, "deviceName"));

            Variable deviceID = context.getPropertyVariable("deviceID", true);
            if (deviceID == null) {
                deviceID = context.getPropertyVariable("deviceID");
            }

            Variable distributionSet = context.getPropertyVariable("distributionSet", true);
            if (distributionSet == null) {
                distributionSet = context.getPropertyVariable("distributionSet");
            }

            Variable assignedDS = context.getPropertyVariable("assignedDS", true);
            if (assignedDS == null) {
                assignedDS = context.getPropertyVariable("assignedDS");
            }

            Variable groupName = context.getPropertyVariable("groupName", true);
            if (groupName == null) {
                groupName = context.getPropertyVariable("groupName");
            }

            Variable deviceList = context.getPropertyVariable("deviceList", true);
            if (deviceList == null) {
                deviceList = context.getPropertyVariable("deviceList");
            }

            internalExternalPropsOutput.put("deviceID", deviceID);
            internalExternalPropsOutput.put("distributionSet", distributionSet);
            internalExternalPropsOutput.put("assignedDS", assignedDS);
            internalExternalPropsOutput.put("groupName", groupName);
            internalExternalPropsOutput.put("deviceList", deviceList);

            LOG.debug("Calling the Invoker for BUILD");
            invokerPlugin.handle(context, templateId, isNodeTemplate, operationName, interfaceName,
                                 callbackAddressVarName, internalExternalPropsInput, internalExternalPropsOutput,
                                 phase);
            LOG.debug("Invoker was called for BUILD");
            return true;
        } else {
            final List<AbstractNodeTemplate> nodeTemplates = context.getNodeTemplates();
            AbstractNodeTemplate otaManager = null;

            for (int i = 0; i < nodeTemplates.size(); i++) {
                final AbstractNodeTemplate tempNodeTemplate = nodeTemplates.get(i);
                if (ModelUtils.getNodeTypeHierarchy(tempNodeTemplate.getType())
                              .contains(IoTTypePluginConstants.OTA_Manager_NODETYPE)) {
                    otaManager = tempNodeTemplate;
                }
            }

            if (otaManager == null) {
                return false;
            }

            final String operation = context.getTemplateBuildPlan().getBuildPlan().getTOSCAOperationName();

            String operationName = "";
            String interfaceName = "";
            Map<AbstractParameter, Variable> internalExternalPropsInput = new HashMap<>();

            switch (operation) {
                case "addBinary":
                    operationName = "uploadBinary";
                    interfaceName = "devicemanagement_binaries";
                    internalExternalPropsInput = createCredentials(context, otaManager);
                    internalExternalPropsInput.put(createParameter("distributionSetName"),
                                                   context.getPropertyVariable(otaManager, "distributionSetName"));
                    internalExternalPropsInput.put(createParameter("urlToBinary"),
                                                   context.getPropertyVariable(otaManager, "urlToBinary"));
                    break;
                case "updateDevice":
                    operationName = "updateDevice";
                    interfaceName = "devicemanagement";
                    internalExternalPropsInput = createCredentials(context, otaManager);
                    internalExternalPropsInput.put(createParameter("distributionSetName"),
                                                   context.getPropertyVariable(otaManager, "distributionSetName"));
                    internalExternalPropsInput.put(createParameter("deviceName"),
                                                   context.getPropertyVariable(otaManager, "deviceName"));
                    break;
                case "syncTOSCAwithRollout":
                    operationName = "syncTOSCAwithRollout";
                    interfaceName = "devicemanagement_sync";
                    internalExternalPropsInput = createCredentials(context, otaManager);
                    break;
                case "createGroup":
                    operationName = "createGroup";
                    interfaceName = "groupmanagement_creategroup";
                    internalExternalPropsInput.put(createParameter("nameOfGroup"),
                                                   context.getPropertyVariable(otaManager, "nameOfGroup"));
                    internalExternalPropsInput.put(createParameter("deviceName"),
                                                   context.getPropertyVariable(otaManager, "deviceName"));
                    break;
                case "addDeviceToGroup":
                    operationName = "addDeviceToGroup";
                    interfaceName = "groupmanagement_addDevice";
                    internalExternalPropsInput.put(createParameter("nameOfGroup"),
                                                   context.getPropertyVariable(otaManager, "nameOfGroup"));
                    internalExternalPropsInput.put(createParameter("deviceName"),
                                                   context.getPropertyVariable(otaManager, "deviceName"));
                    break;
                case "updateGroup":
                    operationName = "updateGroup";
                    interfaceName = "groupmanagement_update";
                    internalExternalPropsInput = createCredentials(context, otaManager);
                    internalExternalPropsInput.put(createParameter("nameOfGroup"),
                                                   context.getPropertyVariable(otaManager, "nameOfGroup"));
                    internalExternalPropsInput.put(createParameter("distributionSetName"),
                                                   context.getPropertyVariable(otaManager, "distributionSetName"));
                    break;
            }

            LOG.debug("Calling the Invoker for MANAGE");
            context.executeOperation(otaManager, interfaceName, operationName, internalExternalPropsInput);
            LOG.debug("Invoker was called for MANAGE");
            return true;
        }
    }

    /**
     *
     * @param context
     * @param otaManager
     * @return
     */
    private Map<AbstractParameter, Variable> createCredentials(final BPELPlanContext context,
                                                               final AbstractNodeTemplate otaManager) {
        final Map<AbstractParameter, Variable> internalExternalPropsInput = new HashMap<>();

        internalExternalPropsInput.put(createParameter("tenant"), context.getPropertyVariable(otaManager, "tenant"));
        internalExternalPropsInput.put(createParameter("user"), context.getPropertyVariable(otaManager, "user"));
        internalExternalPropsInput.put(createParameter("password"),
                                       context.getPropertyVariable(otaManager, "password"));
        internalExternalPropsInput.put(createParameter("host"), context.getPropertyVariable(otaManager, "host"));
        return internalExternalPropsInput;
    }

    /**
     * As we need AbstractParameter to executeOperation, here we convert a String to a AbstractParameter
     *
     * @param parameter the name of the AbstractParameter to set
     * @return the parameter set to an AbstractParameter
     */
    private AbstractParameter createParameter(final String parameter) {
        return new AbstractParameter() {

            @Override
            public boolean isRequired() {
                return false;
            }

            @Override
            public String getType() {
                return "xs:String";
            }

            @Override
            public String getName() {
                return parameter;
            }
        };
    }
}
