/*******************************************************************************
 * Copyright (c) 2013-2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/

package org.oasis_open.docs.tosca.ns._2011._12;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tGroup", propOrder = {
    "nodeTemplates"
})

public class TGroup extends TEntityTemplate {
  
   
    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    
    @XmlAttribute(name = "name")
    protected String name;
    
    @XmlElementWrapper(name = "NodeTemplates")
    @XmlElement(name="NodeTemplate")
    protected List<TNodeTemplate> nodeTemplates;
    
    public TGroup() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TGroup)) return false;
        if (!super.equals(o)) return false;
        TGroup tPlan = (TGroup) o;
        return Objects.equals(id, tPlan.id) &&
            Objects.equals(name, tPlan.name) &&
            Objects.equals(type, tPlan.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, name, type);
    }
    
    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }
    
    public String getFakeJacksonType() {
        return "group";
    }
    
    public TEntityTemplate.Properties getProperties() {
        return properties;
    }

    public void setProperties(TEntityTemplate.Properties properties) {
        this.properties = properties;
    }

    public List<TNodeTemplate> getNodeTemplates() {
        return nodeTemplates;
    }

    public void setNodeTemplates(List<TNodeTemplate> nodeTemplates) {
        this.nodeTemplates = nodeTemplates;
    }
}
