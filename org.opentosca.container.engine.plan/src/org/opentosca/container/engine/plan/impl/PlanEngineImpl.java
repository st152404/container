package org.opentosca.container.engine.plan.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.namespace.QName;

import org.opentosca.container.core.model.capability.provider.ProviderType;
import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.service.ICoreCapabilityService;
import org.opentosca.container.core.tosca.model.TPlan;
import org.opentosca.container.engine.plan.IPlanEnginePluginService;
import org.opentosca.container.engine.plan.IPlanEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the interface {@link org.opentosca.planengine.service.IPlanEngineService}
 * and provides functionality for deployment/undeployment of plans.
 *
 * The implementation uses the OSGi Framework to look for plugins which implement the interface
 * {@link org.opentosca.container.engine.plan.plugin.IPlanEnginePlanPluginService}. The plans (of
 * class TPlan) are delegated to the compatible plugin for deployment/undeployment.
 *
 * Where the plans are deployed is business of the respective plugins. There should always be only
 * one plugin for plans written in the same language.
 */
public class PlanEngineImpl implements IPlanEngineService {

    // stores Plan Plugins
    private final Map<String, IPlanEnginePluginService> pluginList =
        Collections.synchronizedMap(new HashMap<String, IPlanEnginePluginService>());

    private ICoreCapabilityService capabilityService;
    private ICoreCapabilityService oldCapabilityService;

    final private static Logger LOG = LoggerFactory.getLogger(PlanEngineImpl.class);


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deployPlan(final TPlan plan, final String targetNamespace, final CSARID csarId) {
        final String language = plan.getPlanLanguage();

        final QName planId = new QName(targetNamespace, plan.getId());
        LOG.debug("Searching Plan Plugin for deploying plan {} written in language {}", plan.getId(), language);
        final IPlanEnginePluginService plugin = this.pluginList.get(language);
        if (Objects.nonNull(plugin)) {
            LOG.debug("Found Plan Plugin for plan {} ", plan.getId());
            return plugin.deployPlanReference(planId, plan.getPlanModelReference(), csarId);
        } else {
            LOG.warn("No Plan Plugin available for plan {} ", plan.getId());
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean undeployPlan(final TPlan plan, final String targetNamespace, final CSARID csarId) {
        final String language = plan.getPlanLanguage();

        final QName planId = new QName(targetNamespace, plan.getId());
        LOG.debug("Searching Plan Plugin for undeploying plan {} written in language {}", plan.getId(), language);
        final IPlanEnginePluginService plugin = this.pluginList.get(language);
        if (plugin != null) {
            LOG.debug("Found Plan Plugin for plan {} ", plan.getId());
            return plugin.undeployPlanReference(planId, plan.getPlanModelReference(), csarId);
        } else {
            LOG.warn("No Plan Plugin available for plan {} ", plan.getId());
            return false;
        }
    }

    /**
     * Bind method for Plan Plugins
     *
     * @param Plan Plugin to bind
     */
    protected void bindPlanPlugin(final IPlanEnginePluginService planPlugin) {
        if (Objects.nonNull(planPlugin)) {
            LOG.debug("Registering Plan Plugin {} for language {}", planPlugin.toString(),
                      planPlugin.getLanguageUsed());
            if (this.capabilityService != null) {
                this.capabilityService.storeCapabilities(planPlugin.getCapabilties(), planPlugin.toString(),
                                                         ProviderType.PLAN_PLUGIN);
            } else {
                LOG.debug("CapabilityService unavailable, couldn't store plugin capabilities, will do later");
            }
            this.pluginList.put(planPlugin.getLanguageUsed(), planPlugin);
            LOG.debug("Registered Plan Plugin {}", planPlugin.toString());
            LOG.debug("{} Plan plugins registered", this.pluginList.size());
        }
    }

    /**
     * Unbind method for Plan Plugins
     *
     * @param Plan Plugin to unbind
     */
    protected void unbindPlanPlugin(final IPlanEnginePluginService planPlugin) {
        if (Objects.nonNull(planPlugin)) {
            LOG.debug("Unregistered Plan Plugin {}", planPlugin.toString());
            if (this.capabilityService != null) {
                this.capabilityService.deleteCapabilities(planPlugin.toString());
            } else {
                LOG.warn("CapabilityService unavailable, couldn't delete plugin capabilities");
            }
            this.pluginList.remove(planPlugin.getLanguageUsed());
            LOG.debug("Unregistered Plan Plugin {}", planPlugin.toString());
        }
    }

    /**
     * Bind method for CapabilityService
     *
     * @param capabilityService the CapabilityService to bind
     */
    protected void bindCoreCapabilityService(final ICoreCapabilityService capabilityService) {
        if (Objects.nonNull(capabilityService)) {
            LOG.debug("Registering CapabilityService {}", capabilityService.toString());
            if (this.capabilityService == null) {
                this.capabilityService = capabilityService;
            } else {
                this.oldCapabilityService = capabilityService;
                this.capabilityService = capabilityService;
            }

            // storing capabilities of already registered plugins
            for (final IPlanEnginePluginService planRefPlugin : this.pluginList.values()) {
                this.capabilityService.storeCapabilities(planRefPlugin.getCapabilties(), planRefPlugin.toString(),
                                                         ProviderType.PLAN_PLUGIN);
            }

            LOG.debug("Registered CapabilityService {}", capabilityService.toString());
        }
    }

    /**
     * Unbind method for CapabilityService
     *
     * @param capabilityService the CapabilityService to unbind
     */
    protected void unbindCoreCapabilityService(final ICoreCapabilityService capabilityService) {
        LOG.debug("Unregistering CapabilityService {}", capabilityService.toString());
        if (this.oldCapabilityService == null) {
            this.capabilityService = null;
        } else {
            this.oldCapabilityService = null;
        }
        LOG.debug("Unregistered CapabilityService {}", capabilityService.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "openTOSCA PlanEngine v1.0";
    }
}
