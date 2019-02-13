package org.opentosca.bus.management.api.osgievent;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.core.osgi.OsgiServiceRegistry;
import org.apache.camel.impl.DefaultCamelContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator of the OSGiEvent-Management Bus-API.<br>
 * <br>
 *
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 *
 * The activator is needed to add and start the camel routes.
 *
 *
 * @author Michael Zimmermann - zimmerml@studi.informatik.uni-stuttgart.de
 */
public class Activator implements BundleActivator {

    protected static DefaultCamelContext camelContext;
    protected static ProducerTemplate producer;

    public static String apiID;

    final private static Logger LOG = LoggerFactory.getLogger(Activator.class);

    @Override
    public void start(final BundleContext bundleContext) throws Exception {

        Activator.apiID = bundleContext.getBundle().getSymbolicName();

        final OsgiServiceRegistry reg = new OsgiServiceRegistry(bundleContext);
        camelContext = new OsgiDefaultCamelContext(bundleContext, reg);
        camelContext.addRoutes(new OsgiEventRoute());
        camelContext.start();
        LOG.info("Management Bus-OSGI-Event API started!");

        producer = camelContext.createProducerTemplate();
    }

    @Override
    public void stop(final BundleContext arg0) throws Exception {
        camelContext = null;
        LOG.info("Management Bus-OSGI-Event API stopped!");
    }
}
