package io.hotmoka.service.internal.websockets.controllers;

import io.hotmoka.service.internal.services.NetworkExceptionResponse;
import io.hotmoka.service.internal.services.RunService;
import io.hotmoka.service.models.errors.ErrorModel;
import io.hotmoka.service.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.service.models.requests.StaticMethodCallTransactionRequestModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@MessageMapping("/run")
public class WebSocketsRunController {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final RunService nodeRunService;

    @Autowired
    public WebSocketsRunController(SimpMessagingTemplate simpMessagingTemplate, RunService nodeRunService) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.nodeRunService = nodeRunService;
    }

    @MessageMapping("/instanceMethodCallTransaction")
    public void instanceMethodCallTransaction(Principal principal, InstanceMethodCallTransactionRequestModel request) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/run/instanceMethodCallTransaction", nodeRunService.runInstanceMethodCallTransaction(request));
    }

    @MessageMapping("/staticMethodCallTransaction")
    public void staticMethodCallTransaction(Principal principal, StaticMethodCallTransactionRequestModel request) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/run/staticMethodCallTransaction", nodeRunService.runStaticMethodCallTransaction(request));
    }

    @MessageExceptionHandler
    public void handleException(Exception e, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        String destinationTopic = (String) headerAccessor.getHeader("simpDestination");
        if (e instanceof NetworkExceptionResponse)
            simpMessagingTemplate.convertAndSendToUser(principal.getName(), destinationTopic + "/error", ((NetworkExceptionResponse) e).errorModel);
        else
            simpMessagingTemplate.convertAndSendToUser(principal.getName(), destinationTopic + "/error", new ErrorModel(e));
    }
}
