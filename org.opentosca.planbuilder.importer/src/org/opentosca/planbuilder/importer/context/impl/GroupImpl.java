package org.opentosca.planbuilder.importer.context.impl;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.opentosca.planbuilder.model.tosca.AbstractGroup;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractProperties;
import org.opentosca.planbuilder.model.tosca.AbstractTopologyTemplate;
import org.w3c.dom.Element;

public class GroupImpl extends AbstractGroup {

    private final AbstractTopologyTemplate topologyTemplate;
    private final Element element;
    private String id;
    private String name;
    private QName type;
    private AbstractProperties properties;
    private List<AbstractNodeTemplate> nodeTemplates;


    public GroupImpl(final AbstractTopologyTemplate topologyTemplate, final Element element) {
        this.topologyTemplate = topologyTemplate;
        this.element = element;
        setUpGroup();
    }

    @Override
    public AbstractProperties getProperties() {
        return this.properties;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public QName getType() {
        return this.type;
    }

    @Override
    public List<AbstractNodeTemplate> getNodeTemplates() {
        return this.nodeTemplates;
    }

    private void setUpGroup() {
        this.id = this.element.getAttribute("id");
        this.name = this.element.getAttribute("name");

        final String typeDecl = this.element.getAttribute("type");;
        final String[] typeDeclSplit = typeDecl.split(":");
        final String localName = typeDeclSplit[1];
        final String prefix = typeDeclSplit[0];
        final String namespace = findNamespaceFromPrefix(this.element, prefix);

        this.type = new QName(namespace, localName);

        // setup properties and nodeTemplates
        for (int index = 0; index < this.element.getChildNodes().getLength(); index++) {
            if (this.element.getChildNodes().item(index) instanceof Element) {
                final Element element = (Element) this.element.getChildNodes().item(index);
                if (element.getLocalName().equals("Properties")) {
                    setUpProperties(element);
                } else if (element.getLocalName().equals("NodeTemplates")) {
                    setUpNodeTemplates(element);
                }
            }
        }

    }

    private void setUpNodeTemplates(final Element nodeTemplatesElement) {
        this.nodeTemplates = new ArrayList<>();
        for (int index = 0; index < nodeTemplatesElement.getChildNodes().getLength(); index++) {
            if (nodeTemplatesElement.getChildNodes().item(index) instanceof Element) {
                final Element element = (Element) this.element.getChildNodes().item(index);
                if (element.getLocalName().equals("NodeTemplate")) {
                    final String id = element.getAttribute("id");
                    this.nodeTemplates.add(getNodeTemplate(id));
                }
            }
        }
    }

    private AbstractNodeTemplate getNodeTemplate(final String id) {
        final List<AbstractNodeTemplate> nodeTemplates = this.topologyTemplate.getNodeTemplates();

        for (final AbstractNodeTemplate nodeTemplate : nodeTemplates) {
            if (nodeTemplate.getId().equals(id)) {
                return nodeTemplate;
            }
        }
        return null;
    }

    private void setUpProperties(final Element element) {
        this.properties = new PropertiesImpl(element);
    }

    private String findNamespaceFromPrefix(final Element element, final String prefix) {
        return element.getAttribute("xmlns:" + prefix);
    }

}
