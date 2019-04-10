package org.opentosca.bus.management.deployment.plugin.camunda.util;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";
    public static String DeploymentPluginCamunda_types;
    public static String DeploymentPluginCamunda_capabilities;

    static {
        // initialize resource bundle
        NLS.initializeMessages(Messages.BUNDLE_NAME, Messages.class);
    }

    private Messages() {}
}
