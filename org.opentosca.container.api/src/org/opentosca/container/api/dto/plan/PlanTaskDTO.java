package org.opentosca.container.api.dto.plan;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentosca.container.core.next.model.PlanInstanceState;
import org.opentosca.container.core.next.model.PlanTask;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//@formatter:off
/**
 * <Task id="someidval">
 *   <Predecessor>anotherid</Predecessor>
 *   <Name>My awesome name</Name>
 *   <State>RUNNING</State>
 * </Task>
 */
//@formatter:on
@XmlRootElement(name = "Task")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanTaskDTO {

    @XmlAttribute(name = "id")
    private String id;

    @XmlElement(name = "Predecessor")
    private String predecessor;

    @XmlElement(name = "Name")
    private String name;

    @XmlElement(name = "State")
    private PlanInstanceState state;


    public String getId() {
        return this.id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getPredecessor() {
        return this.predecessor;
    }

    public void setPredecessor(final String predecessor) {
        this.predecessor = predecessor;
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

    public static final class Converter {

        public static PlanTask convert(final PlanTaskDTO object) {
            final PlanTask task = new PlanTask();
            task.setTaskId(object.getId());
            task.setPredecessorTaskId(object.getPredecessor());
            task.setName(object.getName());
            task.setState(object.getState());
            return task;
        }

        public static List<PlanTaskDTO> convert(final Collection<PlanTask> tasks) {
            return tasks.stream().map(i -> {
                return PlanTaskDTO.Converter.convert(i);
            }).collect(Collectors.toList());
        }

        private static PlanTaskDTO convert(final PlanTask i) {
            final PlanTaskDTO task = new PlanTaskDTO();
            task.setId(i.getTaskId());
            task.setName(i.getName());
            task.setPredecessor(i.getPredecessorTaskId());
            task.setState(i.getState());
            return task;
        }
    }
}
