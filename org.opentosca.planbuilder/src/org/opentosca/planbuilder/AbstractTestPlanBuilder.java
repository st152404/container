package org.opentosca.planbuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opentosca.planbuilder.model.plan.ANodeTemplateActivity;
import org.opentosca.planbuilder.model.plan.AbstractActivity;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.AbstractPlan.Link;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractPolicy;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractTopologyTemplate;
import org.w3c.dom.Node;

public abstract class AbstractTestPlanBuilder extends AbstractPlanBuilder {


    private static final String OPEN_TOSCA_TEST_NAMESPACE = "http://opentosca.org/policytypes/annotations/tests";

    /**
     * Generates an ordered graph of the nodes that are to be tested
     *
     * @param id
     * @param definitions
     * @param serviceTemplate
     * @return
     */
    protected AbstractPlan generateTestOG(final String id, final AbstractDefinitions definitions,
                                          final AbstractServiceTemplate serviceTemplate) {
        final Collection<AbstractActivity> activities = new ArrayList<>();


        final Map<AbstractNodeTemplate, AbstractActivity> node2activityMap = new HashMap<>();

        final AbstractTopologyTemplate topology = serviceTemplate.getTopologyTemplate();

        for (final AbstractNodeTemplate nodeTemplate : topology.getNodeTemplates()) {
            final ANodeTemplateActivity activity =
                new ANodeTemplateActivity(nodeTemplate.getId() + "__test_activity", "TEST", nodeTemplate);
            activities.add(activity);
            node2activityMap.put(nodeTemplate, activity);
        }

        final Set<Link> links = createOG(node2activityMap.keySet());

        final AbstractPlan abstractTestPlan =
            new AbstractPlan(id, AbstractPlan.PlanType.TEST, definitions, serviceTemplate, activities, links) {};

        return abstractTestPlan;
    }

    /**
     * Links the NodeTemplates according to the ExecutionSequenceNumber Property of their TestPolicy
     *
     * @param nodeTemplates
     * @return
     */
    private Set<Link> createOG(final Collection<AbstractNodeTemplate> nodeTemplates) {
        return new HashSet<>();
    }


    /**
     * Checks if the node template has at least one policy which defines a test
     *
     * @param nodeTemplate
     * @return
     */
    protected boolean nodeTemplateHasTests(final AbstractNodeTemplate nodeTemplate) {
        final List<AbstractPolicy> policies = nodeTemplate.getPolicies();
        for (final AbstractPolicy policy : policies) {
            // TODO:HUGE work around since there is no way to get the namespaces of a policy yet
            final Node policyPropertyDOM = policy.getTemplate().getProperties().getDOMElement().getFirstChild();
            final String namespace = policyPropertyDOM.getNamespaceURI();
            final String foundPrefixForTestNS = policyPropertyDOM.lookupPrefix(OPEN_TOSCA_TEST_NAMESPACE);
            if (foundPrefixForTestNS != null || namespace.equals(OPEN_TOSCA_TEST_NAMESPACE)) {
                return true;
            }
        }
        return false;
    }
}
