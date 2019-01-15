/*******************************************************************************
 * Copyright (c) 2013-2017 Contributors to the Eclipse Foundation
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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * Class to represent Node Template Groups
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tGroups", propOrder = {
    "group"
})
public class TGroups {

    @XmlElement(name = "Group", required = true)
    protected List<TGroup> group;
    @XmlAttribute(name = "targetNamespace")
    @XmlSchemaType(name = "anyURI")
    protected String targetNamespace;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TGroups)) return false;
        TGroups tPlans = (TGroups) o;
        return Objects.equals(group, tPlans.group) &&
            Objects.equals(targetNamespace, tPlans.targetNamespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, targetNamespace);
    }

    public List<TGroup> getGroup() {
        if (group == null) {
            group = new ArrayList<TGroup>();
        }
        return this.group;
    }
    
    public String getTargetNamespace() {
        return targetNamespace;
    }

    public void setTargetNamespace(String value) {
        this.targetNamespace = value;
    }
}
