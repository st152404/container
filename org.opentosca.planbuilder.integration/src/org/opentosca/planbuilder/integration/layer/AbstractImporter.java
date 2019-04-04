package org.opentosca.planbuilder.integration.layer;

import java.util.ArrayList;
import java.util.List;

import org.opentosca.planbuilder.AbstractPlanBuilder;
import org.opentosca.planbuilder.core.bpel.BPELBuildProcessBuilder;
import org.opentosca.planbuilder.core.bpel.BPELDefrostProcessBuilder;
import org.opentosca.planbuilder.core.bpel.BPELFreezeProcessBuilder;
import org.opentosca.planbuilder.core.bpel.BPELScaleOutProcessBuilder;
import org.opentosca.planbuilder.core.bpel.BPELTerminationProcessBuilder;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;

/**
 * <p>
 * This abstract class is used to define importers
 * </p>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 *
 * @author Kalman Kepes - kepeskn@studi.informatik.uni-stuttgart.de
 *
 */
public abstract class AbstractImporter {

    /**
     * Generates Plans for ServiceTemplates inside the given Definitions document
     *
     * @param defs an AbstractDefinitions
     * @param csarName the FileName of the CSAR the given Definitions is contained in
     * @return a List of Plans
     */
    public List<AbstractPlan> buildPlans(final AbstractDefinitions defs, final String csarName) {
        final List<AbstractPlan> plans = new ArrayList<>();

        final AbstractPlanBuilder buildPlanBuilder = new BPELBuildProcessBuilder();

        // FIXME: This does not work for me (Michael W. - 2018-02-19)
        // Because policies must be enforced when they are set on the the topology, if
        // the planbuilder doesn't understand them it doesn't generate a plan -> doesn't
        // work for you
        //
        // if (!this.hasPolicies(defs)) {
        // buildPlanBuilder = new BPELBuildProcessBuilder();
        // } else {
        // buildPlanBuilder = new PolicyAwareBPELBuildProcessBuilder();
        // }

        final AbstractPlanBuilder terminationPlanBuilder = new BPELTerminationProcessBuilder();
        final AbstractPlanBuilder scalingPlanBuilder = new BPELScaleOutProcessBuilder();
        final AbstractPlanBuilder freezePlanBuilder = new BPELFreezeProcessBuilder();
        final AbstractPlanBuilder defreezePlanBuilder = new BPELDefrostProcessBuilder();

        plans.addAll(scalingPlanBuilder.buildPlansForCSAR(csarName, defs));
        plans.addAll(buildPlanBuilder.buildPlansForCSAR(csarName, defs));
        plans.addAll(terminationPlanBuilder.buildPlansForCSAR(csarName, defs));
        // TODO: add plans again
        // plans.addAll(freezePlanBuilder.buildPlansForCSAR(csarName, defs));
        // plans.addAll(defreezePlanBuilder.buildPlansForCSAR(csarName, defs));

        return plans;
    }

    private boolean hasPolicies(final AbstractDefinitions defs) {
        for (final AbstractServiceTemplate serv : defs.getServiceTemplates()) {
            for (final AbstractNodeTemplate nodeTemplate : serv.getTopologyTemplate().getNodeTemplates()) {
                if (!nodeTemplate.getPolicies().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
