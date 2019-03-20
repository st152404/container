package org.opentosca.planbuilder.core.bpel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.opentosca.planbuilder.core.bpel.context.BPELPlanContext;
import org.opentosca.planbuilder.core.bpel.helpers.PropertyVariableInitializer.PropertyMap;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELScopeActivity;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractPolicy;
import org.opentosca.planbuilder.plugins.IPlanBuilderPolicyAwarePostPhasePlugin;
import org.opentosca.planbuilder.plugins.IPlanBuilderPolicyAwarePrePhasePlugin;
import org.opentosca.planbuilder.plugins.IPlanBuilderPolicyAwareTypePlugin;
import org.opentosca.planbuilder.plugins.IPlanBuilderPostPhasePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class extends the {@link BPELBuildProcessBuilder} by using Policy-aware Plan Builder
 * plug-ins to realize the Policy-aware provisioning of ServiceTemplates.
 * </p>
 *
 * Copyright 2019 IAAS University of Stuttgart
 */
public class PolicyAwareBPELBuildProcessBuilder extends BPELBuildProcessBuilder {

    final static Logger LOG = LoggerFactory.getLogger(PolicyAwareBPELBuildProcessBuilder.class);

    public PolicyAwareBPELBuildProcessBuilder() {
        super();
    }

    /**
     * <p>
     * This method assigns plug-ins to the already initialized Build Plan and its
     * TemplateBuildPlans. First it will be checked if a template has policies attached and if not
     * it is provisioned as usual. If there are attached policies it is evaluated if there are
     * compatible Policy-aware plug-ins and the provisioning is delegated to this plug-ins. The
     * provisioning can not be performed if policies without compatible plug-ins exist.
     * </p>
     *
     * @param buildPlan a BuildPlan which is already initialized
     * @param map a PropertyMap which contains mappings from Template to Property and to variable
     *        name inside the BuidlPlan
     */
    @Override
    protected void runPlugins(final BPELPlan buildPlan, final PropertyMap map) {
        LOG.debug("Running policy-aware plugin handling");
        for (final BPELScopeActivity templatePlan : buildPlan.getTemplateBuildPlans()) {
            final BPELPlanContext context = new BPELPlanContext(templatePlan, map, buildPlan.getServiceTemplate());

            if (Objects.nonNull(templatePlan.getNodeTemplate())) {
                final AbstractNodeTemplate nodeTemplate = templatePlan.getNodeTemplate();

                if (nodeTemplate.getPolicies().isEmpty()) {
                    // usual handling without policies
                    runPluginsOnNodeTemplate(context, templatePlan);
                } else {
                    // policy aware handling
                    LOG.debug("Policies defined for NodeTemplate. Handling with Policy-aware plug-ins...");

                    final IPlanBuilderPolicyAwareTypePlugin policyPlugin = findPolicyAwareTypePlugin(nodeTemplate);
                    if (Objects.isNull(policyPlugin)) {
                        LOG.debug("Handling NodeTemplate {} with ProvisioningChain", nodeTemplate.getId());
                        final OperationChain chain = BPELScopeBuilder.createOperationChain(nodeTemplate, this.opNames);
                        if (Objects.isNull(chain)) {
                            LOG.warn("Couldn't create ProvisioningChain for NodeTemplate {}", nodeTemplate.getId());
                        } else {
                            LOG.debug("Created ProvisioningChain for NodeTemplate {}", nodeTemplate.getId());

                            final List<AbstractPolicy> policies = nodeTemplate.getPolicies();
                            final Map<AbstractPolicy, IPlanBuilderPolicyAwarePrePhasePlugin<BPELPlanContext>> compatiblePrePlugins =
                                new HashMap<>();
                            final Map<AbstractPolicy, IPlanBuilderPolicyAwarePostPhasePlugin<BPELPlanContext>> compatiblePostPlugins =
                                new HashMap<>();

                            for (final AbstractPolicy policy : policies) {
                                boolean matched = false;
                                for (final IPlanBuilderPolicyAwarePrePhasePlugin<?> policyPrePhasePlugin : this.pluginRegistry.getPolicyAwarePrePhasePlugins()) {
                                    if (policyPrePhasePlugin.canHandle(nodeTemplate, policy)) {
                                        compatiblePrePlugins.put(policy,
                                                                 (IPlanBuilderPolicyAwarePrePhasePlugin<BPELPlanContext>) policyPrePhasePlugin);
                                        matched = true;
                                        break;
                                    }
                                }

                                if (matched) {
                                    continue;
                                }

                                for (final IPlanBuilderPolicyAwarePostPhasePlugin<?> policyPostPhasePlugin : this.pluginRegistry.getPolicyAwarePostPhasePlugins()) {
                                    if (policyPostPhasePlugin.canHandle(nodeTemplate, policy)) {
                                        compatiblePostPlugins.put(policy,
                                                                  (IPlanBuilderPolicyAwarePostPhasePlugin<BPELPlanContext>) policyPostPhasePlugin);
                                        matched = true;
                                        break;
                                    }
                                }
                            }

                            if (policies.size() == compatiblePrePlugins.keySet().size()
                                + compatiblePostPlugins.keySet().size()) {

                                for (final AbstractPolicy policy : compatiblePrePlugins.keySet()) {
                                    compatiblePrePlugins.get(policy).handle(context, nodeTemplate, policy);
                                }

                                for (final AbstractPolicy policy : compatiblePostPlugins.keySet()) {
                                    compatiblePostPlugins.get(policy).handle(context, nodeTemplate, policy);
                                }

                                chain.executeIAProvisioning(context);
                                chain.executeDAProvisioning(context);
                                chain.executeOperationProvisioning(context, this.opNames);
                            }
                        }
                    } else {
                        LOG.info("Handling NodeTemplate {} with generic policy aware plugin", nodeTemplate.getId());
                        policyPlugin.handlePolicyAware(context);
                    }

                    for (final IPlanBuilderPostPhasePlugin postPhasePlugin : this.pluginRegistry.getPostPlugins()) {
                        if (postPhasePlugin.canHandle(nodeTemplate)) {
                            postPhasePlugin.handle(context, nodeTemplate);
                        }
                    }
                }
            } else if (Objects.nonNull(templatePlan.getRelationshipTemplate())) {
                // relationship templates are not allowed to specify policies
                runPluginsOnRelationshipTemplate(context, templatePlan);
            } else {
                LOG.error("BPELScopeActivity has neither a NodeTemplate nor a RelationshipTemplate defined");
            }
        }
    }
}
