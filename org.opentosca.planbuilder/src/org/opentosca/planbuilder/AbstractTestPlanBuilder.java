package org.opentosca.planbuilder;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

public abstract class AbstractTestPlanBuilder extends AbstractPlanBuilder {

    private final static String TESTING_ACTIVITY_SUFFIX = "_testing_activity";
    private final static String TESTING_ACTIVITY_TYPE = "TESTING";
    protected final static String TEST_INTERFACE_NAMESPACE = "http://opentosca.org/interfaces/tests";
    protected static final String OPEN_TOSCA_TEST_NAMESPACE = "http://opentosca.org/policytypes/annotations/tests";

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

        final Set<AbstractNodeTemplate> relevantNodes = new HashSet<>();
        final Set<AbstractActivity> relevantActivities = new HashSet<>();
        final AbstractTopologyTemplate topology = serviceTemplate.getTopologyTemplate();

        for (final AbstractNodeTemplate nodeTemplate : topology.getNodeTemplates()) {
            // Only add nodes that have tests defined
            if (nodeTemplateHasTests(nodeTemplate)) {
                relevantNodes.add(nodeTemplate);
                relevantActivities.add(new ANodeTemplateActivity(nodeTemplate.getId() + TESTING_ACTIVITY_SUFFIX,
                    TESTING_ACTIVITY_TYPE, nodeTemplate));
            }
        }

        final Set<Link> links = createOG(relevantNodes, topology.getRelationshipTemplates(), relevantActivities);

        final AbstractPlan abstractTestPlan = new AbstractPlan(id, AbstractPlan.PlanType.TEST, definitions,
            serviceTemplate, relevantActivities, links) {};

        System.out.println(abstractTestPlan.toString());

        return abstractTestPlan;
    }

    /**
     * Links the NodeTemplates according to their hierarchie in the template
     *
     * @param nodeTemplates
     * @return
     */
    private Set<Link> createOG(final Collection<AbstractNodeTemplate> relevantNodes,
                               final Collection<AbstractRelationshipTemplate> relationshipTemplates,
                               final Collection<AbstractActivity> relevantActivities) {

        final Set<Link> ogLinks = new HashSet<>();

        //find the predecessors for every relevant node
        for (final AbstractNodeTemplate targetNode : relevantNodes) {
            final ANodeTemplateActivity firstActivity = findActivityForNodeTemplate(targetNode, relevantActivities);
            final Set<ANodeTemplateActivity> relevantSourceActivities =
                findNextRelevantSourceNodes(targetNode, relationshipTemplates, relevantNodes,
                                            relevantActivities);
            for (final ANodeTemplateActivity relevantSourceActivity : relevantSourceActivities) {
                // add links to all sources (traversing from infrastructure to application)
                ogLinks.add(new Link(firstActivity, relevantSourceActivity));
            }
        }
        return ogLinks;
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
            final String namespace = policy.getType().getTargetNamespace();
            if (namespace.equals(OPEN_TOSCA_TEST_NAMESPACE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the next relevant sources of the relevant target in the irrelevantSourceNodeLink
     *
     * @param targetActivity
     * @param relationshipTemplates
     * @param relevantNodes
     * @return
     */
    private final Set<ANodeTemplateActivity> findNextRelevantSourceNodes(final AbstractNodeTemplate targetNode,
                                                                         final Collection<AbstractRelationshipTemplate> relationshipTemplates,
                                                                         final Collection<AbstractNodeTemplate> relevantNodes,
                                                                         final Collection<AbstractActivity> relevantActivities) {
        final Set<ANodeTemplateActivity> retrievedRelevantSources = new HashSet<>();
        final Set<AbstractNodeTemplate> nextIterationNodes = new HashSet<>();

        // first node thats predecessors are to be further investigated
        nextIterationNodes.add(targetNode);

        while (!nextIterationNodes.isEmpty()) {
            final Set<AbstractNodeTemplate> addToNextIteration = new HashSet<>();
            for (final AbstractNodeTemplate irrelevantSourceNode : nextIterationNodes) {
                // retrieve all nodes that are successors of the irrelevant source node
                final Set<AbstractNodeTemplate> sourceSources =
                    irrelevantSourceNode.getIngoingRelations().stream().map(relation -> relation.getSource())
                                        .collect(Collectors.toSet());

                for (final AbstractNodeTemplate sourceSource : sourceSources) {
                    if (relevantNodes.contains(sourceSource)) {
                        // the sourceSource is relevant
                        retrievedRelevantSources.add(findActivityForNodeTemplate(sourceSource, relevantActivities));
                    } else {
                        /*
                         * the sourceSource is irrelevant, add the new NodeLink to the set of NodeLinks that are to
                         * investigate because there might be a relevant source connected to the sourceSource
                         */
                        addToNextIteration.add(sourceSource);
                    }
                }
            }
            nextIterationNodes.clear();
            nextIterationNodes.addAll(addToNextIteration);
            addToNextIteration.clear();
        }

        return retrievedRelevantSources;
    }

    /**
     * Iterates a Collection of activities and returns the one belonging to the passed NodeTemplate
     *
     * @param template
     * @param activities
     * @return
     */
    private ANodeTemplateActivity findActivityForNodeTemplate(final AbstractNodeTemplate template,
                                                              final Collection<AbstractActivity> activities) {
        for (final AbstractActivity activity : activities) {
            if (activity instanceof ANodeTemplateActivity) {
                final String activityId = activity.getId();
                final String activityName = activityId.substring(0, activityId.indexOf(TESTING_ACTIVITY_SUFFIX));
                if (activityName.equals(template.getId())) {
                    System.out.println("Equals: " + activityId + " and " + template.getId());
                    return (ANodeTemplateActivity) activity;
                }
            }
        }
        return null;
    }
}
