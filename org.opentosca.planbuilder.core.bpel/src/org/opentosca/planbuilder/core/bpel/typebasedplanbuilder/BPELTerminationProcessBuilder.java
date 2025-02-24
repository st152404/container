package org.opentosca.planbuilder.core.bpel.typebasedplanbuilder;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.Node;

import org.opentosca.container.core.tosca.convention.Interfaces;
import org.opentosca.planbuilder.AbstractTerminationPlanBuilder;
import org.opentosca.planbuilder.core.bpel.context.BPELPlanContext;
import org.opentosca.planbuilder.core.bpel.handlers.BPELFinalizer;
import org.opentosca.planbuilder.core.bpel.handlers.BPELPlanHandler;
import org.opentosca.planbuilder.core.bpel.handlers.CorrelationIDInitializer;
import org.opentosca.planbuilder.core.bpel.tosca.handlers.NodeRelationInstanceVariablesHandler;
import org.opentosca.planbuilder.core.bpel.tosca.handlers.PropertyVariableHandler;
import org.opentosca.planbuilder.core.bpel.tosca.handlers.SimplePlanBuilderServiceInstanceHandler;
import org.opentosca.planbuilder.core.bpel.typebasednodehandler.BPELPluginHandler;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.ActivityType;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELScope;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.opentosca.planbuilder.plugins.context.Property2VariableMapping;
import org.opentosca.planbuilder.plugins.typebased.IPlanBuilderPostPhasePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Kálmán Képes - kalman.kepes@iaas.uni-stuttgart.de
 *
 */
public class BPELTerminationProcessBuilder extends AbstractTerminationPlanBuilder {

    private final static Logger LOG = LoggerFactory.getLogger(BPELTerminationProcessBuilder.class);

    // handler for abstract buildplan operations
    private BPELPlanHandler planHandler;

    // class for initializing properties inside the build plan
    private final PropertyVariableHandler propertyInitializer;
    // adds serviceInstance Variable and instanceDataAPIUrl to buildPlans
    private SimplePlanBuilderServiceInstanceHandler serviceInstanceHandler;
    // adds nodeInstanceIDs to each templatePlan
    private NodeRelationInstanceVariablesHandler instanceVarsHandler;
    // class for finalizing build plans (e.g when some template didn't receive
    // some provisioning logic and they must be filled with empty elements)
    private final BPELFinalizer finalizer;

    private BPELPluginHandler bpelPluginHandler = new BPELPluginHandler();
    private CorrelationIDInitializer correlationHandler;

    public BPELTerminationProcessBuilder() {
        try {
            this.planHandler = new BPELPlanHandler();
            this.serviceInstanceHandler = new SimplePlanBuilderServiceInstanceHandler();
            this.instanceVarsHandler = new NodeRelationInstanceVariablesHandler(this.planHandler);
            this.correlationHandler = new CorrelationIDInitializer();
        }
        catch (final ParserConfigurationException e) {
            BPELTerminationProcessBuilder.LOG.error("Error while initializing BuildPlanHandler", e);
        }
        this.propertyInitializer = new PropertyVariableHandler(this.planHandler);
        this.finalizer = new BPELFinalizer();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opentosca.planbuilder.IPlanBuilder#buildPlan(java.lang.String,
     * org.opentosca.planbuilder.model.tosca.AbstractDefinitions, javax.xml.namespace.QName)
     */
    @Override
    public BPELPlan buildPlan(final String csarName, final AbstractDefinitions definitions,
                              final AbstractServiceTemplate serviceTemplate) {

        final String processName = ModelUtils.makeValidNCName(serviceTemplate.getId() + "_terminationPlan");
        final String processNamespace = serviceTemplate.getTargetNamespace() + "_terminationPlan";

        final AbstractPlan newAbstractTerminationPlan =
            generateTOG(new QName(processNamespace, processName).toString(), definitions, serviceTemplate);

        final BPELPlan newTerminationPlan =
            this.planHandler.createEmptyBPELPlan(processNamespace, processName, newAbstractTerminationPlan,
                                                 "terminate");

        newTerminationPlan.setTOSCAInterfaceName("OpenTOSCA-Lifecycle-Interface");
        newTerminationPlan.setTOSCAOperationname("terminate");

        this.planHandler.initializeBPELSkeleton(newTerminationPlan, csarName);

        this.instanceVarsHandler.addInstanceURLVarToTemplatePlans(newTerminationPlan, serviceTemplate);
        this.instanceVarsHandler.addInstanceIDVarToTemplatePlans(newTerminationPlan, serviceTemplate);

        final Property2VariableMapping propMap =
            this.propertyInitializer.initializePropertiesAsVariables(newTerminationPlan, serviceTemplate);

        // instanceDataAPI handling is done solely trough this extension
        this.planHandler.registerExtension("http://www.apache.org/ode/bpel/extensions/bpel4restlight", true,
                                           newTerminationPlan);

        // initialize instanceData handling, add
        // instanceDataAPI/serviceInstanceID into input, add global
        // variables to hold the value for plugins
        this.serviceInstanceHandler.addServiceInstanceHandlingFromInput(newTerminationPlan);
        String serviceTemplateURLVarName =
            this.serviceInstanceHandler.getServiceTemplateURLVariableName(newTerminationPlan);
        this.serviceInstanceHandler.appendInitPropertyVariablesFromServiceInstanceData(newTerminationPlan, propMap,
                                                                                       serviceTemplateURLVarName,
                                                                                       serviceTemplate);

        // fetch all nodeinstances that are running
        this.instanceVarsHandler.addNodeInstanceFindLogic(newTerminationPlan,
                                                          "?state=STARTED&amp;state=CREATED&amp;state=CONFIGURED",
                                                          serviceTemplate);
        this.instanceVarsHandler.addPropertyVariableUpdateBasedOnNodeInstanceID(newTerminationPlan, propMap,
                                                                                serviceTemplate);

        this.instanceVarsHandler.addRelationInstanceFindLogic(newTerminationPlan, "?state=CREATED&amp;state=INITIAL",
                                                              serviceTemplate);

        final List<BPELScope> changedActivities = runPlugins(newTerminationPlan, propMap, csarName);

        String serviceInstanceURLVarName =
            this.serviceInstanceHandler.findServiceInstanceUrlVariableName(newTerminationPlan);
        String serviceInstanceId = this.serviceInstanceHandler.findServiceInstanceIdVarName(newTerminationPlan);



        this.serviceInstanceHandler.appendSetServiceInstanceState(newTerminationPlan,
                                                                  newTerminationPlan.getBpelMainSequenceOutputAssignElement(),
                                                                  "DELETED", serviceInstanceURLVarName);

        this.correlationHandler.addCorrellationID(newTerminationPlan);

        this.finalizer.finalize(newTerminationPlan);

        // add for each loop over found node and relation instances to terminate each running
        // instance
        for (final BPELScope activ : changedActivities) {
            if (activ.getNodeTemplate() != null) {
                final BPELPlanContext context =
                    new BPELPlanContext(newTerminationPlan, activ, propMap, newTerminationPlan.getServiceTemplate(),
                        serviceInstanceURLVarName, serviceInstanceId, serviceTemplateURLVarName, csarName);
                this.instanceVarsHandler.appendCountInstancesLogic(context, activ.getNodeTemplate(),
                                                                   "?state=STARTED&amp;state=CREATED&amp;state=CONFIGURED");
            } else {
                final BPELPlanContext context =
                    new BPELPlanContext(newTerminationPlan, activ, propMap, newTerminationPlan.getServiceTemplate(),
                        serviceInstanceURLVarName, serviceInstanceId, serviceTemplateURLVarName, csarName);
                this.instanceVarsHandler.appendCountInstancesLogic(context, activ.getRelationshipTemplate(),
                                                                   "?state=CREATED&amp;state=INITIAL");
            }
        }

        return newTerminationPlan;

    }

    /*
     * (non-Javadoc)
     *
     * @see org.opentosca.planbuilder.IPlanBuilder#buildPlans(java.lang.String,
     * org.opentosca.planbuilder.model.tosca.AbstractDefinitions)
     */
    @Override
    public List<AbstractPlan> buildPlans(final String csarName, final AbstractDefinitions definitions) {
        final List<AbstractPlan> plans = new ArrayList<>();
        for (final AbstractServiceTemplate serviceTemplate : definitions.getServiceTemplates()) {

            if (!serviceTemplate.hasBuildPlan()) {
                BPELTerminationProcessBuilder.LOG.debug("ServiceTemplate {} has no TerminationPlan, generating TerminationPlan",
                                                        serviceTemplate.getQName().toString());
                final BPELPlan newBuildPlan = buildPlan(csarName, definitions, serviceTemplate);

                if (newBuildPlan != null) {
                    BPELTerminationProcessBuilder.LOG.debug("Created TerminationPlan "
                        + newBuildPlan.getBpelProcessElement().getAttribute("name"));
                    plans.add(newBuildPlan);
                }
            } else {
                BPELTerminationProcessBuilder.LOG.debug("ServiceTemplate {} has TerminationPlan, no generation needed",
                                                        serviceTemplate.getQName().toString());
            }
        }
        return plans;
    }

    /**
     * This method will execute plugins on each TemplatePlan inside the given plan for termination of
     * each node and relation.
     *
     * @param plan the plan to execute the plugins on
     * @param serviceTemplate the serviceTemplate the plan belongs to
     * @param propMap a PropertyMapping from NodeTemplate to Properties to BPELVariables
     */
    private List<BPELScope> runPlugins(final BPELPlan plan, final Property2VariableMapping propMap, String csarName) {

        String serviceInstanceUrl = this.serviceInstanceHandler.findServiceInstanceUrlVariableName(plan);
        String serviceInstanceId = this.serviceInstanceHandler.findServiceInstanceIdVarName(plan);
        String serviceTemplateUrl = this.serviceInstanceHandler.findServiceTemplateUrlVariableName(plan);

        final List<BPELScope> changedActivities = new ArrayList<>();
        for (final BPELScope bpelScope : plan.getTemplateBuildPlans()) {
            // we handle only nodeTemplates..
            if (bpelScope.getNodeTemplate() != null) {

                final AbstractNodeTemplate nodeTemplate = bpelScope.getNodeTemplate();
                // .. that are VM nodeTypes
                // create context for the templatePlan
                final BPELPlanContext context = new BPELPlanContext(plan, bpelScope, propMap, plan.getServiceTemplate(),
                    serviceInstanceUrl, serviceInstanceId, serviceTemplateUrl, csarName);
                this.bpelPluginHandler.handleActivity(context, bpelScope, nodeTemplate);
            } else {
                AbstractRelationshipTemplate relationshipTempalte = bpelScope.getRelationshipTemplate();
                final BPELPlanContext context = new BPELPlanContext(plan, bpelScope, propMap, plan.getServiceTemplate(),
                    serviceInstanceUrl, serviceInstanceId, serviceTemplateUrl, csarName);
                this.bpelPluginHandler.handleActivity(context, bpelScope, relationshipTempalte);
            }

        }
        return changedActivities;
    }

}
