package io.hotmoka.network.internal.websocket.controllers;

import io.hotmoka.network.internal.services.NetworkExceptionResponse;
import io.hotmoka.network.internal.services.PostService;
import io.hotmoka.network.models.errors.ErrorModel;
import io.hotmoka.network.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@MessageMapping("/post")
public class WsPostController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private PostService nodePostService;

    @MessageMapping("/jarStoreTransaction")
    @SendTo("/topic/post/jarStoreTransaction")
    public TransactionReferenceModel jarStoreTransaction(JarStoreTransactionRequestModel request) {
        return nodePostService.postJarStoreTransaction(request);
    }

    @MessageMapping("/constructorCallTransaction")
    @SendTo("/topic/post/constructorCallTransaction")
    public TransactionReferenceModel constructorCallTransaction(ConstructorCallTransactionRequestModel request) {
        return nodePostService.postConstructorCallTransaction(request);
    }

    @MessageMapping("/instanceMethodCallTransaction")
    @SendTo("/topic/post/instanceMethodCallTransaction")
    public TransactionReferenceModel instanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request) {
        return nodePostService.postInstanceMethodCallTransaction(request);
    }

    @MessageMapping("/staticMethodCallTransaction")
    @SendTo("/topic/post/staticMethodCallTransaction")
    public TransactionReferenceModel staticMethodCallTransaction(StaticMethodCallTransactionRequestModel request) {
        return nodePostService.postStaticMethodCallTransaction(request);
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
