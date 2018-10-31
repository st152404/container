package org.opentosca.container.core.next.model;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = PlanTask.TABLE_NAME)
public class PlanTask extends PersistenceObject {

    private static final long serialVersionUID = 1633090077594428752L;

    public static final String TABLE_NAME = "PLAN_TASK";

    @Column(nullable = false, unique = true)
    private String taskId;

    @Column(nullable = false)
    private String predecessorTaskId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PlanInstanceState state;

    @ManyToOne
    @JoinColumn(name = "PLAN_INSTANCE_ID")
    @JsonIgnore
    private PlanInstance planInstance;


    public PlanTask() {

    }


    public String getTaskId() {
        return this.taskId;
    }

    public void setTaskId(final String taskId) {
        this.taskId = taskId;
    }

    public String getPredecessorTaskId() {
        return this.predecessorTaskId;
    }

    public void setPredecessorTaskId(final String predecessorTaskId) {
        this.predecessorTaskId = predecessorTaskId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public PlanInstanceState getState() {
        return this.state;
    }

    public void setState(final PlanInstanceState state) {
        this.state = state;
    }

    public PlanInstance getPlanInstance() {
        return this.planInstance;
    }

    public void setPlanInstance(final PlanInstance planInstance) {
        this.planInstance = planInstance;
        if (!planInstance.getTasks().contains(this)) {
            planInstance.getTasks().add(this);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.taskId);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final PlanTask entity = (PlanTask) o;
        return Objects.equals(this.taskId, entity.taskId);
    }
}
