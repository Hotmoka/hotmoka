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

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import io.hotmoka.network.NetworkExceptionResponse;
import io.hotmoka.network.errors.ErrorModel;
import io.hotmoka.network.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.requests.GameteCreationTransactionRequestModel;
import io.hotmoka.network.requests.InitializationTransactionRequestModel;
import io.hotmoka.network.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.requests.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.node.service.internal.services.AddService;

@Controller
@MessageMapping("/add")
public class WebSocketsAddController {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AddService nodeAddService;

    @Autowired
    public WebSocketsAddController(SimpMessagingTemplate simpMessagingTemplate, AddService nodeAddService) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.nodeAddService = nodeAddService;
    }

    @MessageMapping("/jarStoreInitialTransaction")
    public void jarStoreInitialTransaction(Principal principal, JarStoreInitialTransactionRequestModel request) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/add/jarStoreInitialTransaction", nodeAddService.addJarStoreInitialTransaction(request));
    }

    @MessageMapping("/gameteCreationTransaction")
    public void redGreenGameteCreationTransaction(Principal principal, GameteCreationTransactionRequestModel request) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/add/gameteCreationTransaction", nodeAddService.addGameteCreationTransaction(request));
    }

    @MessageMapping("/initializationTransaction")
    public void initializationTransaction(Principal principal, InitializationTransactionRequestModel request) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/add/initializationTransaction", nodeAddService.addInitializationTransaction(request));
    }

    @MessageMapping("/jarStoreTransaction")
    public void jarStoreTransaction(Principal principal, JarStoreTransactionRequestModel request) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/add/jarStoreTransaction", nodeAddService.addJarStoreTransaction(request));
    }

    @MessageMapping("/constructorCallTransaction")
    public void constructorCallTransaction(Principal principal, ConstructorCallTransactionRequestModel request) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/add/constructorCallTransaction", nodeAddService.addConstructorCallTransaction(request));
    }

    @MessageMapping("/instanceMethodCallTransaction")
    public void instanceMethodCallTransaction(Principal principal, InstanceMethodCallTransactionRequestModel request) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/add/instanceMethodCallTransaction", nodeAddService.addInstanceMethodCallTransaction(request));
    }

    @MessageMapping("/staticMethodCallTransaction")
    public void staticMethodCallTransaction(Principal principal, StaticMethodCallTransactionRequestModel request) {
        simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/add/staticMethodCallTransaction", nodeAddService.addStaticMethodCallTransaction(request));
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
