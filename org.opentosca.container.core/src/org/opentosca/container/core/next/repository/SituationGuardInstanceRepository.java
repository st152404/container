package org.opentosca.container.core.next.repository;

import org.opentosca.container.core.next.model.SituationGuardInstance;

public class SituationGuardInstanceRepository extends JpaRepository<SituationGuardInstance> {
	
	public SituationGuardInstanceRepository() {
		super(SituationGuardInstance.class);
	}

}
