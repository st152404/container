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

public class BPELOTATypePluginHandler implements IoTTypePluginHandler<BPELPlanContext> {

    private static final Logger LOG = LoggerFactory.getLogger(BPELOTATypePluginHandler.class);

    private final BPELInvokerPlugin invokerPlugin = new BPELInvokerPlugin();

    private BPELProcessFragments planBuilderFragments;

    public BPELOTATypePluginHandler() {
        try {
            planBuilderFragments = new BPELProcessFragments();
        }
        catch (final ParserConfigurationException e) {
            BPELOTATypePluginHandler.LOG.error("Couldn't initialize planBuilderFragments class");
            e.printStackTrace();
        }
    }

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

            internalExternalPropsOutput.put("deviceID", deviceID);
            internalExternalPropsOutput.put("distributionSet", distributionSet);
            internalExternalPropsOutput.put("assignedDS", assignedDS);


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
            final String templateId = otaManager.getId();
            final boolean isNodeTemplate = true;

            final String operation = context.getTemplateBuildPlan().getBuildPlan().getTOSCAOperationName();

            String operationName = "";
            final String interfaceName = "devicemanagement";
            final String callbackAddressVarName = "planCallbackAddress_invoker";
            final Map<String, Variable> internalExternalPropsInput = new HashMap<>();
            final Map<String, Variable> internalExternalPropsOutput = new HashMap<>();
            final BPELScopePhaseType phase = BPELScopePhaseType.PROVISIONING;

            switch (operation) {
                case "addBinary":
                    operationName = "uploadBinary";
                    internalExternalPropsInput.put("tenant", context.getPropertyVariable(otaManager, "tenant"));
                    internalExternalPropsInput.put("user", context.getPropertyVariable(otaManager, "user"));
                    internalExternalPropsInput.put("password", context.getPropertyVariable(otaManager, "password"));
                    internalExternalPropsInput.put("host", context.getPropertyVariable(otaManager, "host"));
                    internalExternalPropsInput.put("distributionSetName",
                                                   context.getPropertyVariable(otaManager, "distributionSetName"));
                    internalExternalPropsInput.put("urlToBinary",
                                                   context.getPropertyVariable(otaManager, "urlToBinary"));
                    break;
                case "updateDevice":
                    operationName = "updateDevice";

                    internalExternalPropsInput.put("tenant", context.getPropertyVariable(otaManager, "tenant"));
                    internalExternalPropsInput.put("user", context.getPropertyVariable(otaManager, "user"));
                    internalExternalPropsInput.put("password", context.getPropertyVariable(otaManager, "password"));
                    internalExternalPropsInput.put("host", context.getPropertyVariable(otaManager, "host"));
                    internalExternalPropsInput.put("distributionSetName",
                                                   context.getPropertyVariable(otaManager, "distributionSetName"));
                    internalExternalPropsInput.put("deviceName", context.getPropertyVariable(otaManager, "deviceName"));
                    break;
            }

            LOG.debug("Calling the Invoker for MANAGE");
            invokerPlugin.handle(context, templateId, isNodeTemplate, operationName, interfaceName,
                                 callbackAddressVarName, internalExternalPropsInput, internalExternalPropsOutput,
                                 phase);
            LOG.debug("Invoker was called for MANAGE");
            return true;
        }
    }


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
