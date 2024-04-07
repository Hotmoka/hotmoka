/*
Copyright 2021 Dinu Berinde and Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.service.internal.websockets;


import io.hotmoka.network.NetworkExceptionResponse;
import io.hotmoka.network.errors.ErrorModel;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.network.values.TransactionReferenceModel;
import io.hotmoka.node.service.internal.services.GetService;

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

    @MessageMapping("/state")
    public void getState(Principal principal, StorageReferenceModel request) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/get/state", nodeGetService.getState(request));
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

    @MessageMapping("/nameOfSignatureAlgorithmForRequests")
    public void getNameOfSignatureAlgorithmForRequests(Principal principal) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/get/nameOfSignatureAlgorithmForRequests", nodeGetService.getNameOfSignatureAlgorithmForRequests());
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
