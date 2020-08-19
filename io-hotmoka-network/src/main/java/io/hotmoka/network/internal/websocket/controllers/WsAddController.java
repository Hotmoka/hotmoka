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

@Controller
@MessageMapping("/add")
public class WsAddController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private AddService nodeAddService;

    @MessageMapping("/jarStoreInitialTransaction")
    @SendTo("/add/jarStoreInitialTransaction")
    public TransactionReferenceModel jarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request) {
        return nodeAddService.addJarStoreInitialTransaction(request);
    }

    @MessageMapping("/gameteCreationTransaction")
    @SendTo("/add/gameteCreationTransaction")
    public StorageReferenceModel gameteCreationTransaction(GameteCreationTransactionRequestModel request) {
        return nodeAddService.addGameteCreationTransaction(request);
    }

    @MessageMapping("/redGreenGameteCreationTransaction")
    @SendTo("/add/redGreenGameteCreationTransaction")
    public StorageReferenceModel redGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequestModel request) {
        return nodeAddService.addRedGreenGameteCreationTransaction(request);
    }

    @MessageMapping("/initializationTransaction")
    @SendTo("/add/initializationTransaction")
    public ResponseEntity<Void> initializationTransaction(InitializationTransactionRequestModel request) {
        return nodeAddService.addInitializationTransaction(request);
    }

    @MessageMapping("/jarStoreTransaction")
    @SendTo("/add/jarStoreTransaction")
    public TransactionReferenceModel jarStoreTransaction(JarStoreTransactionRequestModel request) {
        return nodeAddService.addJarStoreTransaction(request);
    }

    @MessageMapping("/constructorCallTransaction")
    @SendTo("/add/constructorCallTransaction")
    public StorageReferenceModel constructorCallTransaction(ConstructorCallTransactionRequestModel request) {
        return nodeAddService.addConstructorCallTransaction(request);
    }

    @MessageMapping("/instanceMethodCallTransaction")
    @SendTo("/add/instanceMethodCallTransaction")
    public StorageValueModel instanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request) {
        return nodeAddService.addInstanceMethodCallTransaction(request);
    }

    @MessageMapping("/staticMethodCallTransaction")
    @SendTo("/add/staticMethodCallTransaction")
    public StorageValueModel staticMethodCallTransaction(StaticMethodCallTransactionRequestModel request) {
        return nodeAddService.addStaticMethodCallTransaction(request);
    }

    @MessageExceptionHandler
    public void handleException(Exception e) {
        String userId = ""; // TODO: should return error to the userid and not to all users
        if (e instanceof NetworkExceptionResponse)
            simpMessagingTemplate.convertAndSend("/add/errors", ((NetworkExceptionResponse) e).errorModel);
        else
            simpMessagingTemplate.convertAndSend("/add/errors", new ErrorModel(e));
    }
}
