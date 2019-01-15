/**
 * 
 */
package org.opentosca.container.core.next.repository;

import org.opentosca.container.core.next.model.SituationGuard;

/**
 * @author kalmankepes
 *
 */
public class SituationGuardRepository extends JpaRepository<SituationGuard> {

	public SituationGuardRepository() {
		super(SituationGuard.class);
	}

}
