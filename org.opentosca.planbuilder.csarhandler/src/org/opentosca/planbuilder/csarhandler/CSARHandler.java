package org.opentosca.planbuilder.csarhandler;

import org.opentosca.container.core.common.UserException;
import org.opentosca.container.core.model.csar.CSARContent;
import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.service.ICoreFileService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class is a small layer over the ICoreFileService of the OpenTOSCA Core
 * </p>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 *
 * @author Kalman Kepes - kepeskn@studi.informatik.uni-stuttgart.de
 *
 */
public class CSARHandler {

    final private static Logger LOG = LoggerFactory.getLogger(CSARHandler.class);

    /**
     * Returns a CSARContent Object for the given CSARID
     *
     * @param id a CSARID
     * @return the CSARContent for the given CSARID
     * @throws UserException is thrown when something inside the OpenTOSCA Core fails
     */
    public CSARContent getCSARContentForID(final CSARID id) throws UserException {
        LOG.debug("Fetching CSARContent for given ID");
        return fetchCoreFileService().getCSAR(id);
    }

    private ICoreFileService fetchCoreFileService() {
        CSARHandler.LOG.debug("Retrieving bundle context");
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

        if (bundleContext == null) {
            CSARHandler.LOG.debug("BundleContext from FrameworkUtil is null. Fallback to Activator.");
            bundleContext = Activator.bundleContext;
        }

        if (bundleContext != null) {
            CSARHandler.LOG.debug("Retrieving ServiceReference for ICoreFileService");
            final ServiceReference<?> fileServiceRef =
                bundleContext.getServiceReference(ICoreFileService.class.getName());
            CSARHandler.LOG.debug("Retrieving Service for ICoreFileService");
            final ICoreFileService fileService = (ICoreFileService) bundleContext.getService(fileServiceRef);
            return fileService;
        } else {
            LOG.debug("BundleContext still null. Fallback to ServiceRegistry");
            return ServiceRegistry.getCoreFileService();
        }
    }
}
