package org.opentosca.planbuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.opentosca.planbuilder.model.plan.ANodeTemplateActivity;
import org.opentosca.planbuilder.model.plan.AbstractActivity;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.AbstractPlan.Link;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractPolicy;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractTopologyTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.w3c.dom.Node;

public abstract class AbstractTestPlanBuilder extends AbstractPlanBuilder {

    private class NodeLink {
        private AbstractNodeTemplate source;
        private AbstractNodeTemplate target;

        public NodeLink(final AbstractNodeTemplate source, final AbstractNodeTemplate target) {
            this.source = source;
            this.target = target;
        }

        void setSource(final AbstractNodeTemplate source) {
            this.source = source;
        }

        void setTarget(final AbstractNodeTemplate target) {
            this.target = target;
        }

        AbstractNodeTemplate getSource() {
            return this.source;
        }

        AbstractNodeTemplate getTarget() {
            return this.target;
        }

        @Override
        public boolean equals(final Object otherTuple) {
            if (otherTuple instanceof NodeLink) {
                return this.source.equals(((NodeLink) otherTuple).getSource())
                    && this.target.equals(((NodeLink) otherTuple).getTarget());
            }
            return false;
        }

        @Override
        public String toString() {
            return "NodeLink with source " + this.source + " and target " + this.target + " ";
        }
    }


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
        final List<AbstractActivity> activities = new ArrayList<>();
        final Map<AbstractNodeTemplate, AbstractActivity> node2activityMap = new HashMap<>();
        final AbstractTopologyTemplate topology = serviceTemplate.getTopologyTemplate();

        for (final AbstractNodeTemplate nodeTemplate : topology.getNodeTemplates()) {
            // Only add nodes that have tests defined
            if (nodeTemplateHasTests(nodeTemplate)) {
                final ANodeTemplateActivity activity =
                    new ANodeTemplateActivity(nodeTemplate.getId() + "__test_activity", "TEST", nodeTemplate);
                activities.add(activity);
                node2activityMap.put(nodeTemplate, activity);
            }
        }

        final Set<Link> links =
            createOG(node2activityMap, topology.getNodeTemplates(), topology.getRelationshipTemplates());

        final AbstractPlan abstractTestPlan =
            new AbstractPlan(id, AbstractPlan.PlanType.TEST, definitions, serviceTemplate, activities, links) {};

        return abstractTestPlan;
    }

    /**
     * Links the NodeTemplates according to their hierarchie in the template
     *
     * @param nodeTemplates
     * @return
     */
    private Set<Link> createOG(final Map<AbstractNodeTemplate, AbstractActivity> relevantNodeActivityMapping,
                               final Collection<AbstractNodeTemplate> allNodeTemplates,
                               final Collection<AbstractRelationshipTemplate> relationshipTemplates) {
        final HashSet<Link> links = new HashSet<>();
        final Set<NodeLink> srcTargetMapping = new HashSet<>();
        // the nodes that have at least one test assigned
        final Set<AbstractNodeTemplate> relevantNodes = relevantNodeActivityMapping.keySet();

        for (final AbstractRelationshipTemplate relationshipTemplate : relationshipTemplates) {
            final AbstractNodeTemplate targetNode = relationshipTemplate.getTarget();
            final AbstractNodeTemplate sourceNode = relationshipTemplate.getSource();

            // find relationships that connect at least one node which has defined tests
            if (relevantNodes.contains(targetNode) || relevantNodes.contains(sourceNode)) {
                srcTargetMapping.add(getNodeLink(sourceNode, targetNode, relationshipTemplate));
            }
        }

        final Set<NodeLink> toAdd = new HashSet<>();
        final Set<NodeLink> toRemove = new HashSet<>();
        for (final NodeLink srcTargetTuple : srcTargetMapping) {
            if (relevantNodes.contains(srcTargetTuple.getSource())
                && relevantNodes.contains(srcTargetTuple.getTarget())) {
                continue;
            } else if (relevantNodes.contains(srcTargetTuple.getSource())) {
                // source is relevant, target is not.
                toAdd.addAll(findRelevantTargetNodes(srcTargetTuple, relationshipTemplates, relevantNodes));

                // srcTargetTuple is a tuple containing at least one irrelevant node and thus needs to be removed
                toRemove.add(srcTargetTuple);
            }
        }
        srcTargetMapping.addAll(toAdd);
        srcTargetMapping.removeAll(toRemove);

        System.err.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        srcTargetMapping.forEach(e -> System.err.println(e.toString()));
        System.err.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");

        return links;
    }


    /**
     * Checks if the node template has at least one policy which defines a test
     *
     * @param nodeTemplate
     * @return
     */
    protected final boolean nodeTemplateHasTests(final AbstractNodeTemplate nodeTemplate) {
        final List<AbstractPolicy> policies = nodeTemplate.getPolicies();
        for (final AbstractPolicy policy : policies) {
            // TODO:HUGE work around since there is no way to get the namespaces of a policy yet
            try {
            final Node policyPropertyDOM = policy.getTemplate().getProperties().getDOMElement().getFirstChild();
            final String namespace = policyPropertyDOM.getNamespaceURI();
            final String foundPrefixForTestNS = policyPropertyDOM.lookupPrefix(OPEN_TOSCA_TEST_NAMESPACE);
            if (foundPrefixForTestNS != null || namespace.equals(OPEN_TOSCA_TEST_NAMESPACE)) {
                return true;
            }
            //TODO: delete, workaround cause tests are being defined in topologies that are not existing yet
            } catch(final NullPointerException e) {
                return false;
            }

        }
        return false;
    }

    /**
     * Finds all relevant target nodes that the sourceNode from the irrelevantTargetNodeTuple needs to
     * be connected to Relationship Types are not considered, because if e.g. B hosted on A and B
     * connects to C
     *
     * @param irrelevantTargetNodeTuple
     * @param relationshipTemplates
     * @param relevantNodes
     * @return
     */
    private final Set<NodeLink> findRelevantTargetNodes(final NodeLink irrelevantTargetNodeTuple,
                                                        final Collection<AbstractRelationshipTemplate> relationshipTemplates,
                                                        final Set<AbstractNodeTemplate> relevantNodes) {
        final Set<NodeLink> retrievedLinks = new HashSet<>();
        final List<NodeLink> targetIrrelevantTuples = new ArrayList<>();
        targetIrrelevantTuples.add(irrelevantTargetNodeTuple);
        while (!targetIrrelevantTuples.isEmpty()) {
            final Set<NodeLink> toAdd = new HashSet<>();
            for (final NodeLink targetIrrelevantTuple : targetIrrelevantTuples) {
                for (final AbstractRelationshipTemplate relTemplate : relationshipTemplates) {
                    final AbstractNodeTemplate relationSourceNode = relTemplate.getSource();
                    final AbstractNodeTemplate relationTargetNode = relTemplate.getTarget();
                    // is a relationship from the irrelevant target to some other node
                    if (relationSourceNode.equals(targetIrrelevantTuple.getTarget())) {
                        // target of retrieved relation is relevant
                        if (relevantNodes.contains(relationTargetNode)) {
                            retrievedLinks.add(getNodeLink(targetIrrelevantTuple.getSource(), relationTargetNode, relTemplate));

                        } else {
                            // retrieved target node is also not relevant but might have relevant targets itself that
                            // need to be connected to the original sourceNode and
                            // thus needs to be checked in a next iteration
                            toAdd.add(new NodeLink(targetIrrelevantTuple.getTarget(), relationTargetNode));
                        }
                    }
                }
            }
            targetIrrelevantTuples.clear();
            targetIrrelevantTuples.addAll(toAdd);
            toAdd.clear();
        }

        return retrievedLinks;
    }

    private NodeLink getNodeLink(final AbstractNodeTemplate originalSource, final AbstractNodeTemplate originalTarget, final AbstractRelationshipTemplate relationship) {
        final QName baseType = ModelUtils.getRelationshipBaseType(relationship);
        if (baseType.equals(ModelUtils.TOSCABASETYPE_CONNECTSTO)) {
            return new NodeLink(originalSource, originalTarget);
        } else if (baseType.equals(ModelUtils.TOSCABASETYPE_DEPENDSON)
            | baseType.equals(ModelUtils.TOSCABASETYPE_HOSTEDON)
            | baseType.equals(ModelUtils.TOSCABASETYPE_DEPLOYEDON)) {
            return new NodeLink(originalTarget, originalSource);
        }
        return null;
    }

}
