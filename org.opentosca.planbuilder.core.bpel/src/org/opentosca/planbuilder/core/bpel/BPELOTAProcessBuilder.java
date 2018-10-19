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
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELScopeActivity;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.opentosca.planbuilder.plugins.IPlanBuilderTypePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to create a BPELPlan for the Build and Managementoperations for IoT-Devices
 *
 * @author Marc Schmid
 *
 */
public class BPELOTAProcessBuilder extends AbstractOTAPlanBuilder {

    private final static Logger LOG = LoggerFactory.getLogger(BPELOTAProcessBuilder.class);
    private final PropertyVariableInitializer propertyInitializer;
    private ServiceInstanceVariablesHandler serviceInstanceInitializer;
    private final BPELFinalizer finalizer;
    private BPELPlanHandler planHandler;
    private NodeRelationInstanceVariablesHandler instanceInit;
    private final EmptyPropertyToInputInitializer emptyPropInit = new EmptyPropertyToInputInitializer();

    /**
     * Constructor of the BPELPlanBuilder
     */
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
    }

    /**
     * As we expect more the one plan to be generate, we do not support only one plan generation
     */
    @Override
    public BPELPlan buildPlan(final String csarName, final AbstractDefinitions definitions,
                              final QName serviceTemplateId) {
        throw new RuntimeException("A service Template can have multiple IoT plans, this method is not supported");
    }

    /**
     *
     * Method to generate a BPEL Plan for IoT-Build and Managementoperations Generates an empty plan,
     * fills the needed values and then check for the right plugins to call them and get the BPEL-Code
     * for the plan
     *
     * @param csarName the Name of the CSAR to generate the Plan of
     * @param definitions The TOSCA-Definitions used in the Service Template
     * @param serviceTemplateId the ID of the service Template
     * @param tagValue the value of one of the tags in the service template
     * @param serviceTemplate the modeled Service Template with IoT-Nodes
     * @return a BPELPlan for IoT-Management-Operations
     */
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
        runPlugins(newOTAPlan, propMap, nodetemplates);
        instanceInit.addNodeInstanceFindLogic(newOTAPlan, "?state=CREATED");
        finalizer.finalize(newOTAPlan);

        return newOTAPlan;
    }

    /**
     * Check if the Service Template of the TOSCA definitions has the right tags to need an
     * IoT-Managementplan
     */
    @Override
    public List<AbstractPlan> buildPlans(final String csarName, final AbstractDefinitions definitions) {
        final List<AbstractPlan> plans = new ArrayList<>();

        for (final AbstractServiceTemplate serviceTemplate : definitions.getServiceTemplates()) {
            final Map<String, String> tags = serviceTemplate.getTags();
            boolean doNotHandle = true;

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

    /**
     * Method to run the needed plugin for the given Node Templates
     *
     * @param plan The BPELPlan to set the Plugins in
     * @param map the Properties for the plugins
     * @param nodeTemplates the node templates to run the plugins on
     */
    private void runPlugins(final BPELPlan plan, final PropertyMap map,
                            final List<AbstractNodeTemplate> nodeTemplates) {
        for (final AbstractNodeTemplate node : nodeTemplates) {
            if (ModelUtils.getNodeTypeHierarchy(node.getType())
                          .contains(new QName("http://opentosca.org/nodetypes", "OTA_Manager_w1-wip1"))) {
                final BPELPlanContext context = new BPELPlanContext(
                    planHandler.getTemplateBuildPlanById(node.getId(), plan), map, plan.getServiceTemplate());
                final IPlanBuilderTypePlugin plugin = this.findTypePlugin(node);

                BPELOTAProcessBuilder.LOG.info("Handling NodeTemplate {} with generic plugin", node.getId());
                plugin.handle(context);
            }
        }
    }
}
