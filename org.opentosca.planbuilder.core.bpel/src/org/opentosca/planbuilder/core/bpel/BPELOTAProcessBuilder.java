package org.opentosca.planbuilder.core.bpel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.opentosca.planbuilder.AbstractOTAPlanBuilder;
import org.opentosca.planbuilder.core.bpel.context.BPELPlanContext;
import org.opentosca.planbuilder.core.bpel.handlers.BPELPlanHandler;
import org.opentosca.planbuilder.core.bpel.helpers.BPELFinalizer;
import org.opentosca.planbuilder.core.bpel.helpers.EmptyPropertyToInputInitializer;
import org.opentosca.planbuilder.core.bpel.helpers.NodeRelationInstanceVariablesHandler;
import org.opentosca.planbuilder.core.bpel.helpers.PropertyVariableInitializer;
import org.opentosca.planbuilder.core.bpel.helpers.PropertyVariableInitializer.PropertyMap;
import org.opentosca.planbuilder.core.bpel.helpers.ServiceInstanceVariablesHandler;
import org.opentosca.planbuilder.model.plan.AbstractActivity;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.ActivityType;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELScopeActivity;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.opentosca.planbuilder.plugins.IPlanBuilderPostPhasePlugin;
import org.opentosca.planbuilder.plugins.IPlanBuilderTypePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BPELOTAProcessBuilder extends AbstractOTAPlanBuilder {

    private final static Logger LOG = LoggerFactory.getLogger(BPELOTAProcessBuilder.class);

    private final PropertyVariableInitializer propertyInitializer;
    private ServiceInstanceVariablesHandler serviceInstanceInitializer;
    private final BPELFinalizer finalizer;
    private final List<String> opNames = new ArrayList<>();
    private BPELPlanHandler planHandler;
    private NodeRelationInstanceVariablesHandler instanceInit;
    private final EmptyPropertyToInputInitializer emptyPropInit = new EmptyPropertyToInputInitializer();


    public BPELOTAProcessBuilder() {

        try {
            planHandler = new BPELPlanHandler();
            serviceInstanceInitializer = new ServiceInstanceVariablesHandler();
            instanceInit = new NodeRelationInstanceVariablesHandler(planHandler);
        }
        catch (final ParserConfigurationException e) {
            BPELOTAProcessBuilder.LOG.error("Error while initializing BuildPlanHandler", e);
        }
        propertyInitializer = new PropertyVariableInitializer(planHandler);
        finalizer = new BPELFinalizer();
        // opNames.add("install");
        // opNames.add("configure");
    }



    @Override
    public BPELPlan buildPlan(final String csarName, final AbstractDefinitions definitions,
                              final QName serviceTemplateId) {
        throw new RuntimeException("A service Template can have multiple scaling plans, this method is not supported");
    }

    private BPELPlan createPlan(final String csarName, final AbstractDefinitions definitions,
                                final QName serviceTemplateId, final String tagValue,
                                final AbstractServiceTemplate serviceTemplate) {

        final String processName = ModelUtils.makeValidNCName(serviceTemplate.getId() + "_otaplan_" + tagValue);
        final String processNamespace = serviceTemplate.getTargetNamespace() + "_otaplan";


        final List<AbstractNodeTemplate> nodetemplates = serviceTemplate.getTopologyTemplate().getNodeTemplates();
        final List<AbstractRelationshipTemplate> relationshiptemplates =
            serviceTemplate.getTopologyTemplate().getRelationshipTemplates();

        final AbstractPlan otaplan =
            generateOTA(new QName(processNamespace, processName).toString(), definitions, serviceTemplate);
        final BPELPlan newOTAPlan = planHandler.createEmptyBPELPlan(processNamespace, processName, otaplan, tagValue);

        newOTAPlan.setTOSCAInterfaceName("OpenTOSCA-IOT-Interface");
        newOTAPlan.setTOSCAOperationname(tagValue);

        planHandler.initializeBPELSkeleton(newOTAPlan, csarName);

        final PropertyMap propMap = propertyInitializer.initializePropertiesAsVariables(newOTAPlan);

        planHandler.registerExtension("http://www.apache.org/ode/bpel/extensions/bpel4restlight", true, newOTAPlan);
        serviceInstanceInitializer.addManagementPlanServiceInstanceVarHandlingFromInput(newOTAPlan);
        instanceInit.addInstanceURLVarToTemplatePlans(newOTAPlan);
        instanceInit.addInstanceIDVarToTemplatePlans(newOTAPlan);
        serviceInstanceInitializer.addCorrellationID(newOTAPlan);

        final List<BPELScopeActivity> provScopeActivities = new ArrayList<>();
        for (final BPELScopeActivity act : newOTAPlan.getAbstract2BPEL().values()) {
            if (act.getNodeTemplate() != null && nodetemplates.contains(act.getNodeTemplate())) {
                provScopeActivities.add(act);
            } else if (act.getRelationshipTemplate() != null
                && relationshiptemplates.contains(act.getRelationshipTemplate())) {
                provScopeActivities.add(act);
            }
        }

        emptyPropInit.initializeEmptyPropertiesAsInputParam(provScopeActivities, newOTAPlan, propMap);
        this.runProvisioningLogicGeneration(newOTAPlan, propMap, nodetemplates, relationshiptemplates);

        for (final AbstractActivity activ : newOTAPlan.getActivites()) {
            if (activ.getType().equals(ActivityType.PROVISIONING)) {
                addInstanceIdToOutput(newOTAPlan.getAbstract2BPEL().get(activ));
            }
        }

        finalizer.finalize(newOTAPlan);

        return newOTAPlan;
    }


    @Override
    public List<AbstractPlan> buildPlans(final String csarName, final AbstractDefinitions definitions) {
        final List<AbstractPlan> plans = new ArrayList<>();

        for (final AbstractServiceTemplate serviceTemplate : definitions.getServiceTemplates()) {
            final Map<String, String> tags = serviceTemplate.getTags();

            boolean doNotHandle = true;

            if (!tags.containsKey("iotplan_1")) {
                return plans;
            }

            for (int i = 0; i <= tags.size(); i++) {
                if (tags.containsKey("iotplan_" + i)) {
                    doNotHandle = false;
                }
            }

            if (doNotHandle) {
                return plans;
            }

            final List<String> tagValues = new ArrayList<>();
            for (final Map.Entry<String, String> entry : tags.entrySet()) {
                tagValues.add(entry.getValue());
            }

            for (int i = 0; i < tagValues.size(); i++) {
                plans.add(createPlan(csarName, definitions, serviceTemplate.getQName(), tagValues.get(i),
                                     serviceTemplate));
            }
        }

        return plans;
    }

    private void runProvisioningLogicGeneration(final BPELPlan plan, final PropertyMap map,
                                                final List<AbstractNodeTemplate> nodeTemplates,
                                                final List<AbstractRelationshipTemplate> relationshipTemplates) {
        for (final AbstractNodeTemplate node : nodeTemplates) {
            runProvisioningLogicGeneration(plan, node, map);
        }
        for (final AbstractRelationshipTemplate relation : relationshipTemplates) {
            runProvisioningLogicGeneration(plan, relation, map);
        }
    }

    private void runProvisioningLogicGeneration(final BPELPlan plan, final AbstractNodeTemplate nodeTemplate,
                                                final PropertyMap map) {
        final BPELPlanContext context = new BPELPlanContext(
            planHandler.getTemplateBuildPlanById(nodeTemplate.getId(), plan), map, plan.getServiceTemplate());
        final IPlanBuilderTypePlugin plugin = this.findTypePlugin(nodeTemplate);
        if (plugin == null) {
            BPELOTAProcessBuilder.LOG.debug("Handling NodeTemplate {} with ProvisioningChain", nodeTemplate.getId());
            final OperationChain chain = BPELScopeBuilder.createOperationChain(nodeTemplate);
            if (chain == null) {
                BPELOTAProcessBuilder.LOG.warn("Couldn't create ProvisioningChain for NodeTemplate {}",
                                               nodeTemplate.getId());
            } else {
                BPELOTAProcessBuilder.LOG.debug("Created ProvisioningChain for NodeTemplate {}", nodeTemplate.getId());
                chain.executeIAProvisioning(context);
                chain.executeDAProvisioning(context);
                chain.executeOperationProvisioning(context, opNames);
            }
        } else {
            BPELOTAProcessBuilder.LOG.info("Handling NodeTemplate {} with generic plugin", nodeTemplate.getId());
            plugin.handle(context);
        }

        for (final IPlanBuilderPostPhasePlugin postPhasePlugin : pluginRegistry.getPostPlugins()) {
            if (postPhasePlugin.canHandle(nodeTemplate)) {
                postPhasePlugin.handle(context, nodeTemplate);
            }
        }
    }

    private void runProvisioningLogicGeneration(final BPELPlan plan,
                                                final AbstractRelationshipTemplate relationshipTemplate,
                                                final PropertyMap map) {
        final BPELPlanContext context = createContext(relationshipTemplate, plan, map);
        if (this.findTypePlugin(relationshipTemplate) == null) {
            BPELOTAProcessBuilder.LOG.debug("Handling RelationshipTemplate {} with ProvisioningChains",
                                            relationshipTemplate.getId());
            final OperationChain sourceChain = BPELScopeBuilder.createOperationChain(relationshipTemplate, true);
            final OperationChain targetChain = BPELScopeBuilder.createOperationChain(relationshipTemplate, false);

            if (targetChain != null) {
                BPELOTAProcessBuilder.LOG.warn("Couldn't create ProvisioningChain for TargetInterface of RelationshipTemplate {}",
                                               relationshipTemplate.getId());
                targetChain.executeIAProvisioning(context);
                targetChain.executeOperationProvisioning(context, opNames);
            }

            if (sourceChain != null) {
                BPELOTAProcessBuilder.LOG.warn("Couldn't create ProvisioningChain for SourceInterface of RelationshipTemplate {}",
                                               relationshipTemplate.getId());
                sourceChain.executeIAProvisioning(context);
                sourceChain.executeOperationProvisioning(context, opNames);
            }
        } else {
            BPELOTAProcessBuilder.LOG.info("Handling RelationshipTemplate {} with generic plugin",
                                           relationshipTemplate.getId());
            handleWithTypePlugin(context, relationshipTemplate);
        }

        for (final IPlanBuilderPostPhasePlugin postPhasePlugin : pluginRegistry.getPostPlugins()) {
            if (postPhasePlugin.canHandle(relationshipTemplate)) {
                postPhasePlugin.handle(context, relationshipTemplate);
            }
        }
    }

    public BPELPlanContext createContext(final AbstractRelationshipTemplate relationshipTemplate, final BPELPlan plan,
                                         final PropertyMap map) {
        return new BPELPlanContext(planHandler.getTemplateBuildPlanById(relationshipTemplate.getId(), plan), map,
            plan.getServiceTemplate());
    }


    private boolean addInstanceIdToOutput(final BPELScopeActivity activ) {
        String outputName = "";
        if (activ.getNodeTemplate() != null) {
            outputName = "CreatedInstance_" + activ.getNodeTemplate().getId();
        } else {
            outputName = "CreatedInstance_" + activ.getRelationshipTemplate().getId();
        }

        planHandler.addStringElementToPlanResponse(outputName, activ.getBuildPlan());

        final String varName = instanceInit.findInstanceIdVarName(activ);
        planHandler.assginOutputWithVariableValue(varName, outputName, activ.getBuildPlan());

        return true;
    }

}
