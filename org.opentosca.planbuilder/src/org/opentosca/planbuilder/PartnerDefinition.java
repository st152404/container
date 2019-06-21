package org.opentosca.planbuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.opentosca.container.core.tosca.convention.Types;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractTopologyTemplate;

public class PartnerDefinition {
    /**
     * The name of the Partner/Label
     */
    private final String name;

    /**
     * The Top Node of the Stack eg: (PHP) --> hostedOn --> (Apache) ...
     */
    private AbstractNodeTemplate headNode;

    /**
     * The NodeTemplates of the connected partner which are connected via a connectsTo.
     */
    private final List<AbstractNodeTemplate> partnerNodeTemplates = new ArrayList<>();

    /**
     * All nodes of this partner, with the parter node
     */
    private List<AbstractNodeTemplate> nodes = new ArrayList<>();

    /**
     * All relationships of this partner
     */
    private final List<AbstractRelationshipTemplate> relationships = new ArrayList<>();

    /**
     * The complete topology
     */
    private final AbstractTopologyTemplate abstractTopologyTemplate;

    /**
     * Generates the Partner Definitions
     *
     * @param name The name of this partner
     * @param topology The complete topology which is used to extract the nodes and partner node
     */
    public PartnerDefinition(final String name, final AbstractTopologyTemplate topology) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        this.name = name;
        this.abstractTopologyTemplate = topology;
        this.nodes = findNodes();
        findHeadNode();
        findRelationships(this.headNode, this.name, this.relationships);
        findPartnerNodes(this.headNode);
    }

    private static void findRelationships(final AbstractNodeTemplate startNode, final String name,
                                          final List<AbstractRelationshipTemplate> relations) {
        final Optional<String> splitLabel = startNode.getSplitLabel();

        if (!splitLabel.isPresent() || !splitLabel.get().equals(name)) {
            return;
        }


        final List<AbstractRelationshipTemplate> outgoingRelations = startNode.getOutgoingRelations();
        for (final AbstractRelationshipTemplate abstractRelationshipTemplate : outgoingRelations) {
            final QName type = abstractRelationshipTemplate.getType();
            final AbstractNodeTemplate target = abstractRelationshipTemplate.getTarget();
            final boolean childHasSameLabel =
                target.getSplitLabel().isPresent() && target.getSplitLabel().get().equals(name);
            if (type.equals(Types.hostedOnRelationType) && childHasSameLabel) {
                relations.add(abstractRelationshipTemplate);
                PartnerDefinition.findRelationships(target, name, relations);
            }

            if (type.equals(Types.connectsToRelationType) && !childHasSameLabel) {
                relations.add(abstractRelationshipTemplate);
            }
        }
    }

    /**
     * Reads a {@link AbstractTopologyTemplate} and returns all the nodes where the label is the name
     *
     * @param topologyTemplate A {@link AbstractTopologyTemplate}
     * @param label The label of this partner
     * @return All the nodes who are marked with the label where the label is the name of this partner
     */
    private List<AbstractNodeTemplate> findNodes() {
        final List<AbstractNodeTemplate> nodeTemplates = this.abstractTopologyTemplate.getNodeTemplates();
        final List<AbstractNodeTemplate> result = new ArrayList<>();
        for (final AbstractNodeTemplate abstractNodeTemplate : nodeTemplates) {
            abstractNodeTemplate.getSplitLabel().filter(this.name::equals)
                                .ifPresent(unused -> result.add(abstractNodeTemplate));
        }

        return result;
    }

    /**
     * Finds the Head node of this partner. A head node means no ingoing relations of type hostedOn
     *
     * @param nodes All the nodes of this partner
     */
    private void findHeadNode() {
        for (final AbstractNodeTemplate abstractNodeTemplate : this.nodes) {
            final List<AbstractRelationshipTemplate> ingoingRelations = abstractNodeTemplate.getIngoingRelations();
            final boolean emptyRelations = ingoingRelations.isEmpty();
            final boolean hasNoHostedOnRelations =
                ingoingRelations.stream().noneMatch(rs -> rs.getType().equals(Types.hostedOnRelationType));

            if (emptyRelations || hasNoHostedOnRelations) {
                this.headNode = abstractNodeTemplate;
            }
        }
    }

    /**
     * Using the {@see PartnerDefinition#headNode} node we can search for the partner nodes. Partner
     * nodes are nodes who are connected to any node with a connectsTo and have a different label/name.
     * We also check if the the
     */
    private void findPartnerNodes(final AbstractNodeTemplate headNode) {
        final List<AbstractRelationshipTemplate> outgoingRelations = headNode.getOutgoingRelations();
        for (final AbstractRelationshipTemplate abstractRelationshipTemplate : outgoingRelations) {
            final QName type = abstractRelationshipTemplate.getType();
            final AbstractNodeTemplate target = abstractRelationshipTemplate.getTarget();
            if (type.equals(Types.connectsToRelationType)) {
                this.partnerNodeTemplates.add(target);
            } else if (type.equals(Types.hostedOnRelationType)) {
                findPartnerNodes(target);
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public AbstractNodeTemplate getHeadNode() {
        return this.headNode;
    }

    public List<AbstractNodeTemplate> getPartnerNodeTemplates() {
        return this.partnerNodeTemplates;
    }

    public List<AbstractNodeTemplate> getNodes() {
        return this.nodes;
    }

    public List<AbstractRelationshipTemplate> getRelationships() {
        return this.relationships;
    }



}
