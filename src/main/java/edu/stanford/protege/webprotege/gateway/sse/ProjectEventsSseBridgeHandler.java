package edu.stanford.protege.webprotege.gateway.sse;

import edu.stanford.protege.webprotege.event.EventTag;
import edu.stanford.protege.webprotege.ipc.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

/**
 * Bridges persisted project-change events onto the server-sent event streams. Consumes the
 * {@link SequencedPackagedProjectChangeEvent} that the event-history service republishes after it has
 * durably archived a bundle, and hands the event window to the {@link SseStreamRegistry} stamped with
 * the per-project sequence number as the SSE {@code id} (with event tags
 * {@code startTag = sequenceNumber - 1}, {@code endTag = sequenceNumber}).
 * <p>
 * Push delivery moved from WebSocket/STOMP to SSE in #308: this handler used to also send each bundle
 * to a STOMP {@code /topic} broker, but that transport has been retired and SSE is now the sole push
 * path.
 */
@Component
public class ProjectEventsSseBridgeHandler implements EventHandler<SequencedPackagedProjectChangeEvent> {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProjectEventsSseBridgeHandler.class);

    private final SseStreamRegistry sseStreamRegistry;

    public ProjectEventsSseBridgeHandler(SseStreamRegistry sseStreamRegistry) {
        this.sseStreamRegistry = sseStreamRegistry;
    }


    @Nonnull
    @Override
    public String getChannelName() {
        return SequencedPackagedProjectChangeEvent.CHANNEL;
    }

    @Nonnull
    @Override
    public String getHandlerName() {
        return this.getClass().getName();
    }

    @Override
    public Class<SequencedPackagedProjectChangeEvent> getEventClass() {
        return SequencedPackagedProjectChangeEvent.class;
    }

    @Override
    public void handleEvent(SequencedPackagedProjectChangeEvent event) {
        try {
            int sequenceNumber = event.sequenceNumber();
            ProjectEventsQueryResponse response = new ProjectEventsQueryResponse();
            response.events = new EventList(EventTag.get(sequenceNumber - 1), event.projectEvents(), EventTag.get(sequenceNumber));
            // publish() returns after serialising on this thread but dispatches each send on its own
            // executor, so a slow client cannot stall the Rabbit listener thread.
            sseStreamRegistry.publish(event.projectId(), sequenceNumber, response);
        } catch (Exception e) {
            // Pass the throwable so the cause/stack are visible in the log.
            LOGGER.error("Error forwarding project events to the SSE streams", e);
        }
    }
}
