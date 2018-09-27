package org.opentosca.planbuilder.importer.context.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.oasis_open.docs.tosca.ns._2011._12.TPlan;
import org.oasis_open.docs.tosca.ns._2011._12.TPlans;
import org.oasis_open.docs.tosca.ns._2011._12.TServiceTemplate;
import org.oasis_open.docs.tosca.ns._2011._12.TTag;
import org.opentosca.planbuilder.model.tosca.AbstractBoundaryDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractGroup;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractTopologyTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * <p>
 * This class implements a TOSCA ServiceTemplate, in particular an AbstractServiceTemplate
 * </p>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 *
 * @author Kalman Kepes - kepeskn@studi.informatik.uni-stuttgart.de
 *
 */
public class ServiceTemplateImpl extends AbstractServiceTemplate {

    private final static Logger LOG = LoggerFactory.getLogger(ServiceTemplateImpl.class);

    private TServiceTemplate serviceTemplate = null;
    private AbstractTopologyTemplate topologyTemplate = null;
    private DefinitionsImpl definitions = null;
    private List<AbstractGroup> groups;


    /**
     * Constructor
     *
     * @param serviceTemplate a JAXB TServiceTemplate
     * @param definitionsImpl a DefinitionsImpl
     */
    public ServiceTemplateImpl(final TServiceTemplate serviceTemplate, final DefinitionsImpl definitionsImpl) {
        this.serviceTemplate = serviceTemplate;
        this.definitions = definitionsImpl;
        setUpTopologyTemplate();
        setUpGroups();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractTopologyTemplate getTopologyTemplate() {
        return this.topologyTemplate;
    }

    /**
     * Sets the TopologyTemplate of this ServiceTemplate
     *
     * @param topologyTemplate an AbstractTopologyTemplate
     */
    public void setTopologyTemplate(final AbstractTopologyTemplate topologyTemplate) {
        this.topologyTemplate = topologyTemplate;
    }

    /**
     * Initializes the internal TopologyTemplate of this ServiceTemplate
     */
    private void setUpTopologyTemplate() {
        this.topologyTemplate = new TopologyTemplateImpl(this.serviceTemplate.getTopologyTemplate(), this.definitions);
    }

    private void setUpGroups() {
        final List<Object> obj = this.serviceTemplate.getAny();

        this.groups = new ArrayList<>();

        for (final Object object : obj) {
            if (object instanceof Element) {
                final Element element = (Element) object;
                if (element.getLocalName().equals("Groups")) {
                    for (int index = 0; index < element.getChildNodes().getLength(); index++) {
                        if (element.getChildNodes().item(index) instanceof Element
                            && ((Element) element.getChildNodes().item(index)).getLocalName().equals("Group")) {
                            this.groups.add(new GroupImpl(this.topologyTemplate,
                                (Element) element.getChildNodes().item(index)));
                        }

                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTargetNamespace() {
        if (this.serviceTemplate.getTargetNamespace() == null) {
            ServiceTemplateImpl.LOG.warn("TargetNamespace of ServiceTemplate  {} is null!", getId());
        }
        return this.serviceTemplate.getTargetNamespace();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        if (this.serviceTemplate.getId() == null) {
            ServiceTemplateImpl.LOG.warn("Id of ServiceTemplate is null");
        }
        return this.serviceTemplate.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.serviceTemplate.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QName getQName() {
        String namespace = getTargetNamespace();
        if (namespace == null) {
            namespace = this.definitions.getTargetNamespace();
        }
        final String id = getId();
        return new QName(namespace, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractBoundaryDefinitions getBoundaryDefinitions() {
        if (this.serviceTemplate.getBoundaryDefinitions() != null) {
            return new BoundaryDefinitionsImpl(this.serviceTemplate.getBoundaryDefinitions());
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasBuildPlan() {
        if (this.serviceTemplate.getPlans() != null) {
            final TPlans plans = this.serviceTemplate.getPlans();
            final List<TPlan> plans2 = plans.getPlan();
            ServiceTemplateImpl.LOG.debug("Checking whether ServiceTemplate {} has no BuildPlan",
                                          getQName().toString());
            for (final TPlan plan : plans.getPlan()) {
                ServiceTemplateImpl.LOG.debug("Checking Plan {} of Type {}", plan.getId(), plan.getPlanType());
                if (plan.getPlanType().trim()
                        .equals("http://docs.oasis-open.org/tosca/ns/2011/12/PlanTypes/BuildPlan")) {
                    return true;
                }
            }

        }
        return false;
    }

    @Override
    public boolean hasTerminationPlan() {
        if (this.serviceTemplate.getPlans() != null) {
            final TPlans plans = this.serviceTemplate.getPlans();
            final List<TPlan> plans2 = plans.getPlan();
            ServiceTemplateImpl.LOG.debug("Checking whether ServiceTemplate {} has no TerminationPlan",
                                          getQName().toString());
            for (final TPlan plan : plans.getPlan()) {
                ServiceTemplateImpl.LOG.debug("Checking Plan {} of Type {}", plan.getId(), plan.getPlanType());
                if (plan.getPlanType().trim()
                        .equals("http://docs.oasis-open.org/tosca/ns/2011/12/PlanTypes/TerminationPlan")) {
                    return true;
                }
            }

        }
        return false;
    }

    @Override
    public Map<String, String> getTags() {
        final Map<String, String> tags = new HashMap<>();

        if (this.serviceTemplate.getTags() == null) {
            return tags;
        } else if (this.serviceTemplate.getTags().getTag() == null) {
            return tags;
        }

        for (final TTag tag : this.serviceTemplate.getTags().getTag()) {
            tags.put(tag.getName(), tag.getValue());
        }

        return tags;
    }

    @Override
    public List<AbstractGroup> getGroups() {

        return null;
    }

}
