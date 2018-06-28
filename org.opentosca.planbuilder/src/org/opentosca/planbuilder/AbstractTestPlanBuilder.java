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
import org.opentosca.planbuilder.model.plan.ActivityType;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractPolicy;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractTopologyTemplate;

public abstract class AbstractTestPlanBuilder extends AbstractPlanBuilder {

	private final static String TESTING_ACTIVITY_SUFFIX = "_testing-activity";
	private final static ActivityType TESTING_ACTIVITY_TYPE = ActivityType.TESTING;
	protected final static String TEST_INTERFACE_NAMESPACE = "http://opentosca.org/interfaces/tests";
	protected static final String TEST_POLICYTYPE_NAMESPACE = "http://opentosca.org/policytypes/tests";

	/**
	 * Generates an ordered graph of the nodes that are to be tested
	 *
	 * @param id
	 * @param definitions
	 * @param serviceTemplate
	 * @return
	 */
	protected AbstractPlan generateTestDAG(final String id, final AbstractDefinitions definitions,
			final AbstractServiceTemplate serviceTemplate) {

		final Set<AbstractNodeTemplate> relevantNodes = new HashSet<>();
		final Set<AbstractActivity> allActivities = new HashSet<>();
		final AbstractTopologyTemplate topology = serviceTemplate.getTopologyTemplate();

		for (final AbstractNodeTemplate nodeTemplate : topology.getNodeTemplates()) {
			// Only add nodes that have tests defined
			if (nodeTemplateHasTests(nodeTemplate)) {
				relevantNodes.add(nodeTemplate);
				allActivities.add(new ANodeTemplateActivity(nodeTemplate.getId() + TESTING_ACTIVITY_SUFFIX,
						TESTING_ACTIVITY_TYPE, nodeTemplate));
			} else {
				// this is a hack, but we might need access to properties of node templates that
				// are not "relevant" so we have to add them as a
				allActivities.add(
						new ANodeTemplateActivity(nodeTemplate.getId() + "_mock", TESTING_ACTIVITY_TYPE, nodeTemplate));
			}
		}

		final Set<Link> links = createDAG(relevantNodes, topology.getRelationshipTemplates(), allActivities);

		final AbstractPlan abstractTestPlan = new AbstractPlan(id, AbstractPlan.PlanType.TEST, definitions,
				serviceTemplate, allActivities, links) {
		};

		System.out.println(abstractTestPlan.toString());

		return abstractTestPlan;
	}

	/**
	 * Links the NodeTemplates according to their hierarchies in the template
	 *
	 * @param nodeTemplates
	 * @return
	 */
	private Set<Link> createDAG(final Collection<AbstractNodeTemplate> relevantNodes,
			final Collection<AbstractRelationshipTemplate> relationshipTemplates,
			final Collection<AbstractActivity> allActivities) {

		final Set<Link> ogLinks = new HashSet<>();

		// find the successor test node for every relevant node
		for (final AbstractNodeTemplate sourceNode : relevantNodes) {
			final ANodeTemplateActivity relevantSourceNodeActivity = findActivityForNodeTemplate(sourceNode,
					allActivities);
			if (relevantSourceNodeActivity == null) {
				continue;
			}
			final Set<ANodeTemplateActivity> relevantTargetActivities = findNextRelevantTargetNodes(sourceNode,
					relationshipTemplates, relevantNodes, allActivities);
			for (final ANodeTemplateActivity relevantTargetActivity : relevantTargetActivities) {
				// add links from relevant targetNodes to relevant sourceNodes (traversing from
				// infrastructure to application)
				ogLinks.add(new Link(relevantTargetActivity, relevantSourceNodeActivity));
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
			if (namespace.equals(TEST_POLICYTYPE_NAMESPACE)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all next relevant targetNodes of the relevant sourceNode
	 *
	 * @param sourceActivity
	 * @param relationshipTemplates
	 * @param relevantNodes
	 * @return
	 */
	private final Set<ANodeTemplateActivity> findNextRelevantTargetNodes(final AbstractNodeTemplate sourceNode,
			final Collection<AbstractRelationshipTemplate> relationshipTemplates,
			final Collection<AbstractNodeTemplate> relevantNodes,
			final Collection<AbstractActivity> relevantActivities) {
		final Set<ANodeTemplateActivity> retrievedRelevantTargetNodes = new HashSet<>();
		final Set<AbstractNodeTemplate> nextIterationNodes = new HashSet<>();

		// first node thats successors are to be further investigated
		nextIterationNodes.add(sourceNode);

		while (!nextIterationNodes.isEmpty()) {
			final Set<AbstractNodeTemplate> addToNextIteration = new HashSet<>();
			for (final AbstractNodeTemplate targetNode : nextIterationNodes) {
				// retrieve all nodes that are successors of the irrelevant source node
				final Set<AbstractNodeTemplate> targetTargetNodes = targetNode.getOutgoingRelations().stream()
						.map(relation -> relation.getTarget()).collect(Collectors.toSet());

				for (final AbstractNodeTemplate targetTargetNode : targetTargetNodes) {
					if (relevantNodes.contains(targetTargetNode)) {
						// the sourceSource is relevant
						retrievedRelevantTargetNodes
								.add(findActivityForNodeTemplate(targetTargetNode, relevantActivities));
					} else {
						/*
						 * the sourceSource is irrelevant, add the new NodeLink to the set of NodeLinks
						 * that are to investigate because there might be a relevant source connected to
						 * the sourceSource
						 */
						addToNextIteration.add(targetTargetNode);
					}
				}
			}
			nextIterationNodes.clear();
			nextIterationNodes.addAll(addToNextIteration);
			addToNextIteration.clear();
		}

		return retrievedRelevantTargetNodes;
	}

	/**
	 * Iterates a Collection of activities and returns the one belonging to the
	 * passed NodeTemplate
	 *
	 * @param template
	 * @param activities
	 * @return
	 */
	private ANodeTemplateActivity findActivityForNodeTemplate(final AbstractNodeTemplate template,
			final Collection<AbstractActivity> activities) {
		for (final AbstractActivity activity : activities) {
			if (activity instanceof ANodeTemplateActivity) {
				if (((ANodeTemplateActivity) activity).getNodeTemplate().equals(template)) {
					return (ANodeTemplateActivity) activity;
				}
			}
		}
		return null;
	}
}
