package org.opentosca.planbuilder.model.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;

/**
 * This class represents a topology fragment for which a dedicated build plan is generated.<br>
 * <br>
 *
 * Copyright 2019 IAAS University of Stuttgart
 */
public class TopologyFragment {

    private final String id;

    private final AbstractDefinitions definitions;

    private final AbstractServiceTemplate serviceTemplate;

    // Collections of NodeTemplates/RelationshipTemplates which are part of this fragment. Need to
    // be subsets of the ServiceTemplates NodeTemplates/RelationshipTemplates.
    private final Collection<AbstractNodeTemplate> nodeTemplates;
    private final Collection<AbstractRelationshipTemplate> relationshipTemplates;

    // list of fragments which need to provisioned before this fragment
    private final List<TopologyFragment> precedingFragments = new ArrayList<>();

    public TopologyFragment(final String id, final AbstractDefinitions definitions,
                            final AbstractServiceTemplate serviceTemplate,
                            final Collection<AbstractNodeTemplate> nodeTemplates,
                            final Collection<AbstractRelationshipTemplate> relationshipTemplates) {
        this.id = id;
        this.definitions = definitions;
        this.serviceTemplate = serviceTemplate;
        this.nodeTemplates = nodeTemplates;
        this.relationshipTemplates = relationshipTemplates;
    }

    public String getID() {
        return this.id;
    }

    public AbstractDefinitions getDefinitions() {
        return this.definitions;
    }

    public AbstractServiceTemplate getServiceTemplate() {
        return this.serviceTemplate;
    }

    public Collection<AbstractNodeTemplate> getNodeTemplates() {
        return this.nodeTemplates;
    }

    public Collection<AbstractRelationshipTemplate> getRelationshipTemplates() {
        return this.relationshipTemplates;
    }

    public List<TopologyFragment> getPrecedingFragments() {
        return this.precedingFragments;
    }

    public void addPrecedingFragment(final TopologyFragment fragment) {
        this.precedingFragments.add(fragment);
    }
}
