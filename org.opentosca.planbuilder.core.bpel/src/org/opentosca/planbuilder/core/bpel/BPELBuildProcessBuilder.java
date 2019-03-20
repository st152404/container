package org.opentosca.planbuilder.core.bpel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.opentosca.planbuilder.AbstractBuildPlanBuilder;
import org.opentosca.planbuilder.core.bpel.context.BPELPlanContext;
import org.opentosca.planbuilder.core.bpel.handlers.BPELPlanHandler;
import org.opentosca.planbuilder.core.bpel.helpers.BPELFinalizer;
import org.opentosca.planbuilder.core.bpel.helpers.EmptyPropertyToInputInitializer;
import org.opentosca.planbuilder.core.bpel.helpers.NodeRelationInstanceVariablesHandler;
import org.opentosca.planbuilder.core.bpel.helpers.PropertyMappingsToOutputInitializer;
import org.opentosca.planbuilder.core.bpel.helpers.PropertyVariableInitializer;
import org.opentosca.planbuilder.core.bpel.helpers.PropertyVariableInitializer.PropertyMap;
import org.opentosca.planbuilder.core.bpel.helpers.ServiceInstanceVariablesHandler;
import org.opentosca.planbuilder.core.bpel.helpers.SituationTriggerRegistration;
import org.opentosca.planbuilder.core.bpel.helpers.TopologySplitter;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.TopologyFragment;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELScopeActivity;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.opentosca.planbuilder.plugins.IPlanBuilderPostPhasePlugin;
import org.opentosca.planbuilder.plugins.IPlanBuilderTypePlugin;
import org.opentosca.planbuilder.plugins.context.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class represents the high-level algorithm of the concept in <a href=
 * "http://www2.informatik.uni-stuttgart.de/cgi-bin/NCSTRL/NCSTRL_view.pl?id=BCLR-0043&mod=0&engl=1&inst=FAK"
 * >Konzept und Implementierung eine Java-Komponente zur Generierung von WS-BPEL 2.0 BuildPlans fuer
 * OpenTOSCA</a>. It is responsible for generating the Build Plan Skeleton and assign plugins to
 * handle the different templates inside a TopologyTemplate.
 *
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 *
 * @author Kalman Kepes - kepeskn@studi.informatik.uni-stuttgart.de
 *
 */
public class BPELBuildProcessBuilder extends AbstractBuildPlanBuilder {

    final static Logger LOG = LoggerFactory.getLogger(BPELBuildProcessBuilder.class);

    // class for initializing properties inside the plan
    private final PropertyVariableInitializer propertyInitializer;

    // class for initializing output with boundary definitions of a serviceTemplate
    private final PropertyMappingsToOutputInitializer propertyOutputInitializer =
        new PropertyMappingsToOutputInitializer();;

    // adds serviceInstance Variable and instanceDataAPIUrl to buildPlans
    private ServiceInstanceVariablesHandler serviceInstanceInitializer;

    private SituationTriggerRegistration sitRegistrationPlugin;

    // class for finalizing build plans (e.g when some template didn't receive
    // some provisioning logic and they must be filled with empty elements)
    private final BPELFinalizer finalizer = new BPELFinalizer();;

    // accepted operations for provisioning
    protected final List<String> opNames = new ArrayList<>();

    private BPELPlanHandler planHandler;

    private NodeRelationInstanceVariablesHandler instanceInit;

    private final EmptyPropertyToInputInitializer emptyPropInit = new EmptyPropertyToInputInitializer();

    /**
     * Default Constructor
     */
    public BPELBuildProcessBuilder() {
        try {
            this.planHandler = new BPELPlanHandler();
            this.serviceInstanceInitializer = new ServiceInstanceVariablesHandler();
            this.instanceInit = new NodeRelationInstanceVariablesHandler(this.planHandler);
            this.sitRegistrationPlugin = new SituationTriggerRegistration();
        }
        catch (final ParserConfigurationException e) {
            LOG.error("Error while initializing BuildPlanHandler", e);
        }

        // TODO seems ugly
        this.propertyInitializer = new PropertyVariableInitializer(this.planHandler);
        this.opNames.add("install");
        this.opNames.add("configure");
        this.opNames.add("start");
        // this.opNames.add("connectTo");
        // this.opNames.add("hostOn");
    }

    @Override
    public List<AbstractPlan> buildPlansForServiceTemplate(final String csarName, final AbstractDefinitions definitions,
                                                           final QName serviceTemplateId) {

        // find service template for the plan generation
        final Optional<AbstractServiceTemplate> optional =
            definitions.getServiceTemplates().stream()
                       .filter(template -> Objects.isNull(template.getTargetNamespace())
                           && serviceTemplateId.getNamespaceURI().equals(definitions.getTargetNamespace())
                           || serviceTemplateId.getNamespaceURI().equals(template.getTargetNamespace()))
                       .filter(template -> serviceTemplateId.getLocalPart().equals(template.getId())).findFirst();

        if (!optional.isPresent()) {
            LOG.warn("Couldn't create BuildPlan for ServiceTemplate {} in Definitions {} of CSAR {}",
                     serviceTemplateId.toString(), definitions.getId(), csarName);
            return new ArrayList<>();
        }

        final AbstractServiceTemplate serviceTemplate = optional.get();

        final String processName = ModelUtils.makeValidNCName(serviceTemplate.getId() + "_buildPlan");
        final String processNamespace = serviceTemplate.getTargetNamespace() + "_buildPlan";

        // split the topology into different fragments which can be provisioned independently
        final List<TopologyFragment> fragments =
            TopologySplitter.splitTopologyHorizontally(definitions, serviceTemplate);

        // TODO: further splitting

        // generate a POG for each topology fragment
        final List<AbstractPlan> buildPlanFragments =
            generatePOGs(new QName(processNamespace, processName).toString(), definitions, serviceTemplate, fragments);

        // transform generated POGs to BPEL plans
        for (final AbstractPlan planFragment : buildPlanFragments) {
            LOG.debug("Generated the following abstract prov plan fragment:");
            LOG.debug(planFragment.toString());

            // TODO: transform POGs to BPEL plans
        }

        final AbstractPlan buildPlan =
            generatePOG(new QName(processNamespace, processName).toString(), definitions, serviceTemplate);

        LOG.debug("Generated the following abstract prov plan: ");
        LOG.debug(buildPlan.toString());

        final BPELPlan bpelPlan =
            this.planHandler.createEmptyBPELPlan(processNamespace, processName, buildPlan, "initiate");

        bpelPlan.setTOSCAInterfaceName("OpenTOSCA-Lifecycle-Interface");
        bpelPlan.setTOSCAOperationname("initiate");

        this.planHandler.initializeBPELSkeleton(bpelPlan, csarName);

        this.instanceInit.addInstanceURLVarToTemplatePlans(bpelPlan);
        this.instanceInit.addInstanceIDVarToTemplatePlans(bpelPlan);

        this.planHandler.registerExtension("http://www.apache.org/ode/bpel/extensions/bpel4restlight", true, bpelPlan);

        final PropertyMap propMap = this.propertyInitializer.initializePropertiesAsVariables(bpelPlan);
        this.propertyOutputInitializer.initializeBuildPlanOutput(definitions, bpelPlan, propMap);

        // instanceDataAPI handling is done solely trough this extension

        // initialize instanceData handling
        this.serviceInstanceInitializer.initializeInstanceDataFromInput(bpelPlan);

        this.emptyPropInit.initializeEmptyPropertiesAsInputParam(bpelPlan, propMap);

        runPlugins(bpelPlan, propMap);

        this.serviceInstanceInitializer.addCorrellationID(bpelPlan);

        this.serviceInstanceInitializer.appendSetServiceInstanceState(bpelPlan, bpelPlan.getBpelMainFlowElement(),
                                                                      "CREATING");
        this.serviceInstanceInitializer.appendSetServiceInstanceState(bpelPlan,
                                                                      bpelPlan.getBpelMainSequenceOutputAssignElement(),
                                                                      "CREATED");

        this.sitRegistrationPlugin.handle(serviceTemplate, bpelPlan);

        this.finalizer.finalize(bpelPlan);
        LOG.debug("Created BuildPlan:");
        LOG.debug(ModelUtils.getStringFromDoc(bpelPlan.getBpelDocument()));

        // TODO: return all generated plan fragments
        final List<AbstractPlan> plans = new ArrayList<>();
        plans.add(bpelPlan);
        return plans;
    }

    @Override
    public List<AbstractPlan> buildPlansForCSAR(final String csarName, final AbstractDefinitions definitions) {
        final List<AbstractPlan> plans = new ArrayList<>();
        for (final AbstractServiceTemplate serviceTemplate : definitions.getServiceTemplates()) {
            final QName serviceTemplateId = ModelUtils.getServiceTemplateQName(definitions, serviceTemplate);

            if (!serviceTemplate.hasBuildPlan()) {
                LOG.debug("ServiceTemplate {} has no Build Plan, generating Build Plans", serviceTemplateId.toString());
                final List<AbstractPlan> serviceTemplatePlans =
                    buildPlansForServiceTemplate(csarName, definitions, serviceTemplateId);

                if (Objects.nonNull(serviceTemplatePlans) && !serviceTemplatePlans.isEmpty()) {
                    LOG.debug("Created a List of Build Plans:");
                    for (final AbstractPlan plan : serviceTemplatePlans) {
                        LOG.debug("Created BuildPlan " + plan.getId());
                    }
                    plans.addAll(serviceTemplatePlans);
                } else {
                    LOG.warn("Build Plan generation was not successful");
                }
            } else {
                LOG.debug("ServiceTemplate {} has Build Plan, no generation needed", serviceTemplateId.toString());
            }
        }
        return plans;
    }

    /**
     * This method assigns plug-ins to the already initialized Build Plan and its
     * TemplateBuildPlans. First it will be checked if any generic plug-in can handle a template. If
     * not the template is handled by the Provisioning Chain.
     *
     * @param buildPlan a BuildPlan which is already initialized
     * @param map a PropertyMap which contains mappings from Template to Property and to variable
     *        name of inside the BuidlPlan
     */
    protected void runPlugins(final BPELPlan buildPlan, final PropertyMap map) {

        for (final BPELScopeActivity templatePlan : buildPlan.getTemplateBuildPlans()) {
            final BPELPlanContext context = new BPELPlanContext(templatePlan, map, buildPlan.getServiceTemplate());

            if (Objects.nonNull(templatePlan.getNodeTemplate())) {
                runPluginsOnNodeTemplate(context, templatePlan);
            } else if (Objects.nonNull(templatePlan.getRelationshipTemplate())) {
                runPluginsOnRelationshipTemplate(context, templatePlan);
            } else {
                LOG.error("BPELScopeActivity has neither a NodeTemplate nor a RelationshipTemplate defined");
            }
        }
    }

    /**
     * Handle the provisioning of a NodeTemplates by a generic plug-in if available and perform the
     * provisioning by the Provisioning Chain otherwise.
     *
     * @param context the context of the BPEL plan
     * @param templatePlan the template for which the provisioning shall be done
     */
    protected void runPluginsOnNodeTemplate(final BPELPlanContext context, final BPELScopeActivity templatePlan) {
        final AbstractNodeTemplate nodeTemplate = templatePlan.getNodeTemplate();

        if (!isRunning(context, nodeTemplate)) {
            LOG.debug("Trying to handle NodeTemplate " + nodeTemplate.getId());

            // check if we have a generic plugin to handle the template
            // Note: if a generic plugin fails during execution the TemplateBuildPlan is broken!
            final IPlanBuilderTypePlugin<BPELPlanContext> plugin = this.findTypePlugin(nodeTemplate);
            if (Objects.isNull(plugin)) {

                LOG.debug("Handling NodeTemplate {} with ProvisioningChain", nodeTemplate.getId());
                final OperationChain chain = BPELScopeBuilder.createOperationChain(nodeTemplate, this.opNames);
                if (Objects.isNull(chain)) {
                    LOG.warn("Couldn't create ProvisioningChain for NodeTemplate {}", nodeTemplate.getId());
                } else {
                    LOG.debug("Created ProvisioningChain for NodeTemplate {}", nodeTemplate.getId());
                    chain.executeIAProvisioning(context);
                    chain.executeDAProvisioning(context);
                    chain.executeOperationProvisioning(context, this.opNames);
                }
            } else {
                LOG.info("Handling NodeTemplate {} with generic plugin", nodeTemplate.getId());
                plugin.handle(context);
            }
        } else {
            LOG.debug("Skipping the provisioning of NodeTemplate " + templatePlan.getNodeTemplate().getId()
                + "  beacuse state=running is set.");
        }

        // run post phase plug-ins
        for (final IPlanBuilderPostPhasePlugin postPhasePlugin : this.pluginRegistry.getPostPlugins()) {
            if (postPhasePlugin.canHandle(templatePlan.getNodeTemplate())) {
                postPhasePlugin.handle(context, templatePlan.getNodeTemplate());
            }
        }
    }

    /**
     * Handle the provisioning of a RelationshipTemplates by a generic plug-in if available and
     * perform the provisioning by the Provisioning Chain otherwise.
     *
     * @param context the context of the BPEL plan
     * @param templatePlan the template for which the provisioning shall be done
     */
    protected void runPluginsOnRelationshipTemplate(final BPELPlanContext context,
                                                    final BPELScopeActivity templatePlan) {
        final AbstractRelationshipTemplate relationshipTemplate = templatePlan.getRelationshipTemplate();

        // check if we have a generic plugin to handle the template
        // Note: if a generic plugin fails during execution the TemplateBuildPlan is broken here!
        // TODO implement fallback
        if (canGenericPluginHandle(relationshipTemplate)) {
            LOG.info("Handling RelationshipTemplate {} with generic plugin", relationshipTemplate.getId());
            handleWithTypePlugin(context, relationshipTemplate);
        } else {
            LOG.debug("Handling RelationshipTemplate {} with ProvisioningChains", relationshipTemplate.getId());
            final OperationChain sourceChain = BPELScopeBuilder.createOperationChain(relationshipTemplate, true);
            final OperationChain targetChain = BPELScopeBuilder.createOperationChain(relationshipTemplate, false);

            // first execute provisioning on target, then on source
            if (Objects.nonNull(targetChain)) {
                targetChain.executeIAProvisioning(context);
                targetChain.executeOperationProvisioning(context, this.opNames);
            }

            if (Objects.nonNull(sourceChain)) {
                sourceChain.executeIAProvisioning(context);
                sourceChain.executeOperationProvisioning(context, this.opNames);
            }
        }

        for (final IPlanBuilderPostPhasePlugin postPhasePlugin : this.pluginRegistry.getPostPlugins()) {
            if (postPhasePlugin.canHandle(templatePlan.getRelationshipTemplate())) {
                postPhasePlugin.handle(context, templatePlan.getRelationshipTemplate());
            }
        }
    }

    /**
     * Checks whether there is any generic plugin, that can handle the given RelationshipTemplate
     *
     * @param relationshipTemplate an AbstractRelationshipTemplate denoting a RelationshipTemplate
     * @return true if there is any generic plugin which can handle the given RelationshipTemplate,
     *         else false
     */
    protected boolean canGenericPluginHandle(final AbstractRelationshipTemplate relationshipTemplate) {
        return this.pluginRegistry.getGenericPlugins().stream().filter(plugin -> plugin.canHandle(relationshipTemplate))
                                  .findFirst().isPresent();
    }

    private boolean isRunning(final BPELPlanContext context, final AbstractNodeTemplate nodeTemplate) {
        final Variable state = context.getPropertyVariable(nodeTemplate, "State");
        return state != null && BPELPlanContext.getVariableContent(state, context).equals("Running");
    }
}
