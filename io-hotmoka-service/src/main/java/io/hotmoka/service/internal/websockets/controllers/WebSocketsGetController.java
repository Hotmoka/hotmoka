package io.hotmoka.service.internal.websockets.controllers;


import io.hotmoka.service.internal.services.GetService;
import io.hotmoka.service.internal.services.NetworkExceptionResponse;
import io.hotmoka.service.models.errors.ErrorModel;
import io.hotmoka.service.models.values.StorageReferenceModel;
import io.hotmoka.service.models.values.TransactionReferenceModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@MessageMapping("/get")
public class WebSocketsGetController {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final GetService nodeGetService;

    @Autowired
    public WebSocketsGetController(SimpMessagingTemplate simpMessagingTemplate, GetService nodeGetService) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.nodeGetService = nodeGetService;
    }

    @MessageMapping("/takamakaCode")
    public void getTakamakaCode(Principal principal) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/get/takamakaCode", nodeGetService.getTakamakaCode());
    }

    @MessageMapping("/manifest")
    public void getManifest(Principal principal) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/get/manifest", nodeGetService.getManifest());
    }

    @MessageMapping("/state")
    public void getState(Principal principal, StorageReferenceModel request) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/get/state", nodeGetService.getState(request));
    }

    @MessageMapping("/classTag")
    public void getClassTag(Principal principal, StorageReferenceModel request) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/get/classTag", nodeGetService.getClassTag(request));
    }

    @MessageMapping("/request")
    public void getRequestAt(Principal principal, TransactionReferenceModel reference) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/get/request", nodeGetService.getRequest(reference));
    }

    @MessageMapping("/response")
    public void getResponseAt(Principal principal, TransactionReferenceModel reference) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/get/response", nodeGetService.getResponse(reference));
    }

    @MessageMapping("/polledResponse")
    public void getPolledResponseAt(Principal principal, TransactionReferenceModel reference) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/get/polledResponse", nodeGetService.getPolledResponse(reference));
    }

    @MessageMapping("/signatureAlgorithmForRequests")
    public void getSignatureAlgorithmForRequests(Principal principal) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/get/signatureAlgorithmForRequests", nodeGetService.getSignatureAlgorithmForRequests());
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
