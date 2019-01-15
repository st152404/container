package org.opentosca.container.core.next.model;

import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Entity
@Table(name = SituationGuard.TABLE_NAME)
public class SituationGuard extends PersistenceObject {

	private static final long serialVersionUID = -7318757574469678781L;

	public static final String TABLE_NAME = "SITUATION_GUARD";

    @OneToMany()
    @JoinColumn(name = "SITUATION_ID")
    private Collection<Situation> situations;

    @Column(nullable = false)
    private boolean triggerOnActivation;


    @OneToOne
    @JoinColumn(name = "SERVICE_TEMPLATE_INSTANCE_ID", nullable = true)
    private ServiceTemplateInstance serviceInstance;

    @OneToOne
    @JoinColumn(name = "NODE_TEMPLATE_INSTANCE_ID", nullable = true)
    private NodeTemplateInstance nodeInstance;

    @Column(nullable = false)
    private String interfaceName;

    @Column(nullable = false)
    private String operationName;

    @OrderBy("createdAt DESC")
    @OneToMany(mappedBy = "situationGuard")
    private Collection<SituationGuardInstance> situationGuardInstances;

    public Collection<Situation> getSituations() {
        return this.situations;
    }

    public void setSituations(final Collection<Situation> situation) {
        this.situations = situation;
    }


    public boolean isTriggerOnActivation() {
        return this.triggerOnActivation;
    }

    public void setTriggerOnActivation(final boolean triggerOnActivation) {
        this.triggerOnActivation = triggerOnActivation;
    }

    public ServiceTemplateInstance getServiceInstance() {
        return this.serviceInstance;
    }

    public void setServiceInstance(final ServiceTemplateInstance serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public NodeTemplateInstance getNodeInstance() {
        return this.nodeInstance;
    }

    public void setNodeInstance(final NodeTemplateInstance nodeInstance) {
        this.nodeInstance = nodeInstance;
    }

    public String getInterfaceName() {
        return this.interfaceName;
    }

    public void setInterfaceName(final String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getOperationName() {
        return this.operationName;
    }

    public void setOperationName(final String operationName) {
        this.operationName = operationName;
    }


    public Collection<SituationGuardInstance> getSituationGuardInstances() {
        return this.situationGuardInstances;
    }

    public void setSituationGuardInstances(final Collection<SituationGuardInstance> situationGuardInstances) {
        this.situationGuardInstances = situationGuardInstances;
    }
	
}
