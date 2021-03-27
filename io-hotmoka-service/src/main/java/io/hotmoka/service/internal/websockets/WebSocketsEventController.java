package io.hotmoka.service.internal.websockets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import io.hotmoka.network.requests.EventRequestModel;

@Controller
public class WebSocketsEventController {
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    public WebSocketsEventController(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @MessageMapping("/events")
    public void addEvent(EventRequestModel eventRequestModel) {
        simpMessagingTemplate.convertAndSend("/topic/events", eventRequestModel);
    }
}
