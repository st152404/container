/**
 * 
 */
package org.opentosca.planbuilder.importer.context.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.oasis_open.docs.tosca.ns._2011._12.TGroup;
import org.oasis_open.docs.tosca.ns._2011._12.TNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractGroup;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractProperties;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;

/**
 * @author kalmankepes
 *
 */
public class GroupImpl extends AbstractGroup {

	private TGroup group;
	private AbstractServiceTemplate servTemplate;
	private DefinitionsImpl defs;

	public GroupImpl(DefinitionsImpl defs, AbstractServiceTemplate servTemplate, TGroup group) {
		this.group = group;
		this.servTemplate = servTemplate;
		this.defs = defs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.planbuilder.model.tosca.AbstractGroup#getId()
	 */
	@Override
	public String getId() {
		return this.group.getId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.planbuilder.model.tosca.AbstractGroup#getTargetNamespace()
	 */
	@Override
	public String getTargetNamespace() {
		return this.servTemplate.getTargetNamespace();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.planbuilder.model.tosca.AbstractGroup#getQName()
	 */
	@Override
	public QName getQName() {
		return new QName(this.getTargetNamespace(), this.getId());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.planbuilder.model.tosca.AbstractGroup#getType()
	 */
	@Override
	public QName getType() {
		return this.group.getType();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.planbuilder.model.tosca.AbstractGroup#getProperties()
	 */
	@Override
	public AbstractProperties getProperties() {
		return new PropertiesImpl(this.group.getProperties().getAny());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.planbuilder.model.tosca.AbstractGroup#getNodeTemplates()
	 */
	@Override
	public List<AbstractNodeTemplate> getNodeTemplates() {
		List<AbstractNodeTemplate> nodes = new ArrayList<AbstractNodeTemplate>();

		for (TNodeTemplate node : this.group.getNodeTemplates()) {
			nodes.add(new NodeTemplateImpl(node, this.defs));
		}

		return nodes;
	}

}
