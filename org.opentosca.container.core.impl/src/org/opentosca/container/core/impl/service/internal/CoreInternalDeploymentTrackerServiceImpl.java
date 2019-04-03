package org.opentosca.container.core.impl.service.internal;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.model.deployment.plan.PlanDeploymentInfo;
import org.opentosca.container.core.model.deployment.plan.PlanDeploymentState;
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
     * {@inheritDoc}
     */
    @Override
    public boolean storePlanDeploymentInfo(PlanDeploymentInfo planDeploymentInfo) {
        init();

        CoreInternalDeploymentTrackerServiceImpl.LOG.info("Storing deployment state {} for Plan \"{}\" of CSAR \"{}\"...",
                                                          new Object[] {planDeploymentInfo.getDeploymentState(),
                                                                        planDeploymentInfo.getRelPath(), planDeploymentInfo.getCSARID()});

        this.em.getTransaction().begin();

        // check if deployment info for this Plan already exists
        final PlanDeploymentInfo storedPlan =
            getPlanDeploymentInfo(planDeploymentInfo.getCSARID(), planDeploymentInfo.getRelPath());

        // deployment info already exists
        if (storedPlan != null) {

            CoreInternalDeploymentTrackerServiceImpl.LOG.debug("Plan deployment info for Plan \"{}\" of CSAR \"{}\" already exists. Existent deployment info will be overwritten!",
                                                               planDeploymentInfo.getRelPath(),
                                                               planDeploymentInfo.getCSARID());

            final PlanDeploymentState storedPlanDeployState = storedPlan.getDeploymentState();
            final PlanDeploymentState newPlanDeployState = planDeploymentInfo.getDeploymentState();

            // if Plan is deployed and will be now undeployed (deployment state
            // change to PLAN_UNDEPLOYING) reset the attempt counter to 0
            if (storedPlanDeployState.equals(PlanDeploymentState.PLAN_DEPLOYED)
                && newPlanDeployState.equals(PlanDeploymentState.PLAN_UNDEPLOYING)) {
                CoreInternalDeploymentTrackerServiceImpl.LOG.debug("Deployed Plan \"{}\" of CSAR \"{}\" is now undeploying. Attempt count will be reseted.",
                                                                   planDeploymentInfo.getRelPath(),
                                                                   planDeploymentInfo.getCSARID());
                storedPlan.setAttempt(0);
            }

            storedPlan.setDeploymentState(newPlanDeployState);
            planDeploymentInfo = storedPlan;
        }

        // if Plan is now deploying or undeploying (deployment state change to
        // PLAN_DEPLOYING / PLAN_UNDEPLOYING) increment attempt counter
        if (planDeploymentInfo.getDeploymentState().equals(PlanDeploymentState.PLAN_DEPLOYING)
            || planDeploymentInfo.getDeploymentState().equals(PlanDeploymentState.PLAN_UNDEPLOYING)) {
            CoreInternalDeploymentTrackerServiceImpl.LOG.debug("Plan \"{}\" of CSAR \"{}\" is now deploying / undeploying. Increase attempt count.",
                                                               planDeploymentInfo.getRelPath(),
                                                               planDeploymentInfo.getCSARID());
            planDeploymentInfo.setAttempt(planDeploymentInfo.getAttempt() + 1);
        }

        this.em.persist(planDeploymentInfo);
        this.em.getTransaction().commit();

        CoreInternalDeploymentTrackerServiceImpl.LOG.info("Storing deployment state {} for Plan \"{}\" of CSAR \"{}\" completed.",
                                                          new Object[] {planDeploymentInfo.getDeploymentState(),
                                                                        planDeploymentInfo.getRelPath(), planDeploymentInfo.getCSARID()});

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PlanDeploymentInfo getPlanDeploymentInfo(final CSARID csarID, final String planRelPath) {
        init();
        CoreInternalDeploymentTrackerServiceImpl.LOG.info("Retrieving Plan deployment info for Plan \"{}\" of CSAR \"{}\"...",
                                                          planRelPath, csarID);
        final Query getPlanDeploymentInfo =
            this.em.createNamedQuery(PlanDeploymentInfo.getPlanDeploymentInfoByCSARIDAndRelPath)
                   .setParameter("csarID", csarID).setParameter("planRelPath", planRelPath);
        @SuppressWarnings("unchecked")
        final List<PlanDeploymentInfo> results = getPlanDeploymentInfo.getResultList();
        if (results.isEmpty()) {
            CoreInternalDeploymentTrackerServiceImpl.LOG.error("No Plan deployment info for Plan \"{}\" of CSAR \"{}\" stored.",
                                                               planRelPath, csarID);
            return null;
        } else {
            CoreInternalDeploymentTrackerServiceImpl.LOG.info("Plan deployment info for Plan \"{}\" of CSAR \"{}\" exists.",
                                                              planRelPath, csarID);
            return results.get(0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlanDeploymentInfo> getPlanDeploymentInfos(final CSARID csarID) {
        init();
        CoreInternalDeploymentTrackerServiceImpl.LOG.info("Retrieving all Plan deployment infos of CSAR \"{}\"...",
                                                          csarID);
        final ArrayList<PlanDeploymentInfo> results = new ArrayList<>();
        final Query getIADeploymentInfo =
            this.em.createNamedQuery(PlanDeploymentInfo.getPlanDeploymentInfoByCSARID).setParameter("csarID", csarID);
        @SuppressWarnings("unchecked")
        final List<PlanDeploymentInfo> queryResults = getIADeploymentInfo.getResultList();
        for (final PlanDeploymentInfo ia : queryResults) {
            results.add(ia);
        }
        CoreInternalDeploymentTrackerServiceImpl.LOG.info("Plan deployment infos of {} Plan(s) of CSAR \"{}\" stored.",
                                                          results.size(), csarID);
        return results;
    }

    /**
     * Prints the available OSGi commands.
     */
    @Override
    public String getHelp() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean storePlanDeploymentInfo(final CSARID csarID, final String planRelPath,
                                           final PlanDeploymentState planDeploymentState) {
        final PlanDeploymentInfo planDeploymentInfo = new PlanDeploymentInfo(csarID, planRelPath, planDeploymentState);
        this.storePlanDeploymentInfo(planDeploymentInfo);
        return true;
    }

}
