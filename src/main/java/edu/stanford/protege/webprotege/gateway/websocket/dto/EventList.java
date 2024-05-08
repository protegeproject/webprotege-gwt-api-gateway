package edu.stanford.protege.webprotege.gateway.websocket.dto;

import edu.stanford.protege.webprotege.common.Event;
import edu.stanford.protege.webprotege.event.EventTag;

import java.util.List;

public record EventList <E extends Event> (EventTag startTag, List<E> events, EventTag endTag){
}
