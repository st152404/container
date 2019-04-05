package org.opentosca.container.core.impl.service.internal;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.model.deployment.process.DeploymentProcessInfo;
import org.opentosca.container.core.model.deployment.process.DeploymentProcessState;
import org.opentosca.container.core.next.jpa.EntityManagerProvider;
import org.opentosca.container.core.service.internal.ICoreInternalDeploymentTrackerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the deployment process of CSAR files, Implementations Artifacts and Plans by providing
 * methods for storing and getting it deployment states. It is used by OpenTOSCA Control to allowing
 * only a subset of all provided operations in a certain deployment state of a CSAR file.
 */
public class CoreInternalDeploymentTrackerServiceImpl implements ICoreInternalDeploymentTrackerService,
                                                      CommandProvider {

    private final static Logger LOG = LoggerFactory.getLogger(CoreInternalDeploymentTrackerServiceImpl.class);

    private EntityManager em;


    public CoreInternalDeploymentTrackerServiceImpl() {}

    /**
     * Initializes JPA.
     */
    private void init() {
        if (this.em == null) {
            this.em = EntityManagerProvider.createEntityManager();
        }
    }

    /**
     * This method is called when the garbage collector destroys the class. We will then manually
     * close the EntityManager / Factory and pass control back.
     */
    @Override
    protected void finalize() throws Throwable {
        this.em.close();
        super.finalize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean storeDeploymentState(final CSARID csarID, final DeploymentProcessState deploymentState) {
        init();
        CoreInternalDeploymentTrackerServiceImpl.LOG.info("Storing deployment state {} for CSAR \"{}\"...",
                                                          deploymentState, csarID);
        this.em.getTransaction().begin();
        // check if deployment state for this CSAR already exists
        final DeploymentProcessInfo deploymentInfo = getDeploymentProcessInfo(csarID);
        if (deploymentInfo != null) {
            // CoreInternalDeploymentTrackerServiceImpl.LOG.debug("Deployment
            // state for CSAR \"{}\" already exists. Existent state will be
            // overwritten!", csarID);
            deploymentInfo.setDeploymentProcessState(deploymentState);
            this.em.persist(deploymentInfo);
        } else {
            // CoreInternalDeploymentTrackerServiceImpl.LOG.debug("Deployment
            // state for CSAR \"{}\" did not already exist.", csarID);
            this.em.persist(new DeploymentProcessInfo(csarID, deploymentState));
        }
        this.em.getTransaction().commit();
        CoreInternalDeploymentTrackerServiceImpl.LOG.info("Storing deployment state {} for CSAR \"{}\" completed.",
                                                          deploymentState, csarID);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeploymentProcessState getDeploymentState(final CSARID csarID) {

        CoreInternalDeploymentTrackerServiceImpl.LOG.info("Retrieving deployment state for CSAR \"{}\"...", csarID);
        init();
        final DeploymentProcessInfo info = getDeploymentProcessInfo(csarID);

        DeploymentProcessState deploymentState = null;

        if (info == null) {
            CoreInternalDeploymentTrackerServiceImpl.LOG.error("No deployment state for CSAR \"{}\" stored!", csarID);
        } else {
            deploymentState = info.getDeploymentProcessState();
            CoreInternalDeploymentTrackerServiceImpl.LOG.info("Deployment state of CSAR \"{}\": {}.", csarID,
                                                              deploymentState);
        }

        return deploymentState;

    }

    @Override
    public void deleteDeploymentState(final CSARID csarID) {
        CoreInternalDeploymentTrackerServiceImpl.LOG.debug("Retrieving DeploymentProcessInfo for {}", csarID);
        final DeploymentProcessInfo info = getDeploymentProcessInfo(csarID);
        if (info != null) {
            CoreInternalDeploymentTrackerServiceImpl.LOG.debug("Beginning Transaction for removing DeploymentProcessInfo {}",
                                                               csarID);
            this.em.getTransaction().begin();
            CoreInternalDeploymentTrackerServiceImpl.LOG.debug("Removing DeploymentProcessInfo {}", csarID);

            final Query queryRestEndpoints =
                this.em.createQuery("DELETE FROM DeploymentProcessInfo e where e.csarID = :csarID");
            queryRestEndpoints.setParameter("csarID", csarID);

            // this.em.remove(info);
            CoreInternalDeploymentTrackerServiceImpl.LOG.debug("Commiting Transaction");
            this.em.getTransaction().commit();
        }
    }

    /**
     * Gets the deployment process information of a CSAR file.
     *
     * @param csarID that uniquely identifies a CSAR file
     * @return the deployment process information, if the CSAR with <code>csarID</code> exists,
     *         otherwise <code>null</code>
     */
    private DeploymentProcessInfo getDeploymentProcessInfo(final CSARID csarID) {

        init();
        CoreInternalDeploymentTrackerServiceImpl.LOG.debug("Retrieving deployment process info for CSAR \"{}\"...",
                                                           csarID);
        final Query getDeploymentProcessInfo =
            this.em.createNamedQuery(DeploymentProcessInfo.getDeploymentProcessInfoByCSARID).setParameter("csarID",
                                                                                                          csarID);

        @SuppressWarnings("unchecked")
        final List<DeploymentProcessInfo> results = getDeploymentProcessInfo.getResultList();

        if (results.isEmpty()) {
            CoreInternalDeploymentTrackerServiceImpl.LOG.debug("No deployment process info for CSAR \"{}\" stored.",
                                                               csarID);
            return null;
        } else {
            CoreInternalDeploymentTrackerServiceImpl.LOG.debug("Deployment process info for CSAR \"{}\" exists.",
                                                               csarID);
            return results.get(0);
        }
    }

    /**
     * Prints the available OSGi commands.
     */
    @Override
    public String getHelp() {
        return null;
    }
}
