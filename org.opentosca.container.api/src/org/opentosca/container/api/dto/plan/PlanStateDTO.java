package org.opentosca.container.api.dto.plan;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentosca.container.core.next.model.PlanInstanceState;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//@formatter:off
/**
 * <PlanState>
 *   <State>RUNNING</State>
 *   <Tasks>
 *     <Task id="someidval">
 *       <Predecessor>anotherid</Predecessor>
 *       <Name>My awesome name</Name>
 *       <State>RUNNING</State>
 *     </Task>
 *   </Tasks>
 * </PlanState>
 */
//@formatter:on
@XmlRootElement(name = "PlanState")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanStateDTO {

    @XmlElement(name = "State")
    private PlanInstanceState state;

    @XmlElement(name = "Task")
    @XmlElementWrapper(name = "Tasks")
    private List<PlanTaskDTO> tasks = new ArrayList<>();


    public PlanInstanceState getState() {
        return this.state;
    }

    public void setState(final PlanInstanceState state) {
        this.state = state;
    }

    public List<PlanTaskDTO> getTasks() {
        return this.tasks;
    }

    public void setTasks(final List<PlanTaskDTO> tasks) {
        this.tasks = tasks;
    }
}
