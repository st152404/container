package org.opentosca.container.core.next.model;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = SituationGuardInstance.TABLE_NAME)
public class SituationGuardInstance extends PersistenceObject {

	private static final long serialVersionUID = 1032192139830291789L;

	public static final String TABLE_NAME = "SITUATION_GUARD_INSTANCE";

	@OneToOne()
	@JoinColumn(name = "SITUATION_TRIGGER_ID")
	private SituationGuard situationGuard;

	public SituationGuard getSituationGuard() {
		return this.situationGuard;
	}

	public void setSituationGuard(final SituationGuard situationGuard) {
		this.situationGuard = situationGuard;
	}

}
