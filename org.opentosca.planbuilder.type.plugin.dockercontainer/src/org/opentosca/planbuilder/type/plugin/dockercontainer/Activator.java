package org.opentosca.planbuilder.type.plugin.dockercontainer;

import org.opentosca.planbuilder.plugins.IPlanBuilderPolicyAwareTypePlugin;
import org.opentosca.planbuilder.plugins.IPlanBuilderTypePlugin;
import org.opentosca.planbuilder.type.plugin.dockercontainer.bpel.BPELDockerContainerTypePlugin;
import org.opentosca.planbuilder.type.plugin.dockercontainer.bpel.BPELOpenMTCDockerContainerTypePlugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private static BundleContext context;

    private ServiceRegistration<?> registrationDockerContainerPlugin;
    private ServiceRegistration<?> registrationOpenMTCDockerContainerPlugin;
    private ServiceRegistration<?> registrationPolicyAwarePlugin;

    static BundleContext getContext() {
        return Activator.context;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext )
     */
    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        Activator.context = bundleContext;
        this.registrationDockerContainerPlugin =
            Activator.context.registerService(IPlanBuilderTypePlugin.class.getName(),
                                              new BPELDockerContainerTypePlugin(), null);
        this.registrationOpenMTCDockerContainerPlugin =
            Activator.context.registerService(IPlanBuilderTypePlugin.class.getName(),
                                              new BPELOpenMTCDockerContainerTypePlugin(), null);
        this.registrationPolicyAwarePlugin =
            Activator.context.registerService(IPlanBuilderPolicyAwareTypePlugin.class.getName(),
                                              new BPELDockerContainerTypePlugin(), null);

    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(final BundleContext bundleContext) throws Exception {
        Activator.context = null;
        this.registrationDockerContainerPlugin.unregister();
        this.registrationOpenMTCDockerContainerPlugin.unregister();
        this.registrationPolicyAwarePlugin.unregister();

    }

}
