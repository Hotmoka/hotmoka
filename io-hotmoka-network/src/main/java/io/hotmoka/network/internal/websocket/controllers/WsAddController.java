package io.hotmoka.network.internal.websocket.controllers;

import io.hotmoka.network.internal.services.AddService;
import io.hotmoka.network.internal.services.NetworkExceptionResponse;
import io.hotmoka.network.models.errors.ErrorModel;
import io.hotmoka.network.models.requests.*;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.StorageValueModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@MessageMapping("/add")
public class WsAddController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private AddService nodeAddService;

    @MessageMapping("/jarStoreInitialTransaction")
    @SendTo("/topic/add/jarStoreInitialTransaction")
    public TransactionReferenceModel jarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request) {
        return nodeAddService.addJarStoreInitialTransaction(request);
    }

    @MessageMapping("/gameteCreationTransaction")
    @SendTo("/topic/add/gameteCreationTransaction")
    public StorageReferenceModel gameteCreationTransaction(GameteCreationTransactionRequestModel request) {
        return nodeAddService.addGameteCreationTransaction(request);
    }

    @MessageMapping("/redGreenGameteCreationTransaction")
    @SendTo("/topic/add/redGreenGameteCreationTransaction")
    public StorageReferenceModel redGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequestModel request) {
        return nodeAddService.addRedGreenGameteCreationTransaction(request);
    }

    @MessageMapping("/initializationTransaction")
    @SendTo("/topic/add/initializationTransaction")
    public ResponseEntity<Void> initializationTransaction(InitializationTransactionRequestModel request) {
        return nodeAddService.addInitializationTransaction(request);
    }

    @MessageMapping("/jarStoreTransaction")
    @SendTo("/topic/add/jarStoreTransaction")
    public TransactionReferenceModel jarStoreTransaction(JarStoreTransactionRequestModel request) {
        return nodeAddService.addJarStoreTransaction(request);
    }

    @MessageMapping("/constructorCallTransaction")
    @SendTo("/topic/add/constructorCallTransaction")
    public StorageReferenceModel constructorCallTransaction(ConstructorCallTransactionRequestModel request) {
        return nodeAddService.addConstructorCallTransaction(request);
    }

    @MessageMapping("/instanceMethodCallTransaction")
    @SendTo("/topic/add/instanceMethodCallTransaction")
    public StorageValueModel instanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request) {
        return nodeAddService.addInstanceMethodCallTransaction(request);
    }

    @MessageMapping("/staticMethodCallTransaction")
    @SendTo("/topic/add/staticMethodCallTransaction")
    public StorageValueModel staticMethodCallTransaction(StaticMethodCallTransactionRequestModel request) {
        return nodeAddService.addStaticMethodCallTransaction(request);
    }

    @MessageExceptionHandler
    public void handleException(Exception e, Principal principal) {
        if (e instanceof NetworkExceptionResponse)
            simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/add/errors", ((NetworkExceptionResponse) e).errorModel);
        else
            simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/add/errors", new ErrorModel(e));
    }
}
