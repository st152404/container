package org.opentosca.bus.management.api.osgievent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.opentosca.bus.management.header.MBHeader;
import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.next.model.PlanLanguage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event handler of the Management Bus-OSGi-Event-API.<br>
 * <br>
 *
 * Copyright 2019 IAAS University of Stuttgart <br>
 * <br>
 *
 * Handles the events (receive and sent) of the Management Bus-OSGi-Event-API.
 */
public class OsgiEventHandler implements EventHandler {

    private static Logger LOG = LoggerFactory.getLogger(OsgiEventHandler.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    private EventAdmin eventAdmin;

    @Override
    public void handleEvent(final Event event) {

        // Handle plan invoke requests
        if ("org_opentosca_plans/requests".equals(event.getTopic())) {
            LOG.debug("Process event of topic \"org_opentosca_plans/requests\".");

            final CSARID csarID = (CSARID) event.getProperty(OsgiEventProperties.CSARID.toString());
            final String planLanguage = (String) event.getProperty(OsgiEventProperties.PLANLANGUAGE.toString());

            if (planLanguage.startsWith(PlanLanguage.BPMN.toString())
                || planLanguage.startsWith(PlanLanguage.BPEL.toString())) {
                LOG.debug("Plan invocation with plan language: {}", planLanguage);

                final String operationName = (String) event.getProperty(OsgiEventProperties.OPERATIONNAME.toString());
                final String messageID = (String) event.getProperty(OsgiEventProperties.MESSAGEID.toString());
                final Object message = event.getProperty(OsgiEventProperties.BODY.toString());

                // create the headers for the Exchange which is send to the Management Bus
                final Map<String, Object> headers = new HashMap<>();
                headers.put(MBHeader.CSARID.toString(), csarID);
                headers.put(MBHeader.OPERATIONNAME_STRING.toString(), operationName);
                headers.put(MBHeader.PLANCORRELATIONID_STRING.toString(), messageID);
                headers.put(OsgiEventRoute.OPERATION_HEADER, OsgiEventOperations.INVOKE_PLAN.getHeaderValue());

                LOG.debug("Correlation id: {}", messageID);
                LOG.debug("Sending following message to the Management Bus: {}", message);

                // forward request to the Management Bus
                Activator.producer.sendBodyAndHeaders("direct:invoke", message, headers);

                // Threaded reception of response
                this.executor.submit(() -> {

                    Object response = null;

                    try {
                        final ConsumerTemplate consumer = Activator.camelContext.createConsumerTemplate();
                        consumer.start();
                        final Exchange exchange = consumer.receive("direct:response" + messageID);
                        response = exchange.getIn().getBody();
                        consumer.stop();
                    }
                    catch (final Exception e) {
                        LOG.error("Error occured: {}", e.getMessage(), e);
                        return;
                    }

                    LOG.debug("Received response for request with id {}.", messageID);

                    final Map<String, Object> responseMap = new HashMap<>();
                    responseMap.put(OsgiEventProperties.RESPONSE.toString(), response);
                    responseMap.put(OsgiEventProperties.MESSAGEID.toString(), messageID);
                    responseMap.put(OsgiEventProperties.PLANLANGUAGE.toString(), planLanguage);

                    LOG.debug("Posting response as OSGi event.");
                    this.eventAdmin.postEvent(new Event("org_opentosca_plans/responses", responseMap));
                });

            } else {
                LOG.warn("Unsupported plan language: {}", planLanguage);
            }
        }

        // Handle IA invoke requests
        if ("org_opentosca_ia/requests".equals(event.getTopic())) {
            LOG.debug("Process event of topic \"org_opentosca_ia/requests\".");

            // TODO when needed.
            // Adapt 'MBEventHandler - component.xml' to receive messages from this topic too...
        }
    }

    public void bindEventAdmin(final EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unbindEventAdmin(final EventAdmin eventAdmin) {
        try {
            this.executor.shutdown();
            this.executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (final InterruptedException e) {
            // Ignore
        }
        finally {
            this.executor.shutdownNow();
        }
        this.eventAdmin = null;
    }
}
