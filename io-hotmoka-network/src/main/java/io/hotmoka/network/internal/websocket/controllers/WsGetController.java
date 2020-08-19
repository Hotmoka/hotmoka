package io.hotmoka.network.internal.websocket.controllers;


import io.hotmoka.network.internal.services.GetService;
import io.hotmoka.network.internal.services.NetworkExceptionResponse;
import io.hotmoka.network.models.errors.ErrorModel;
import io.hotmoka.network.models.requests.TransactionRestRequestModel;
import io.hotmoka.network.models.responses.SignatureAlgorithmResponseModel;
import io.hotmoka.network.models.responses.TransactionRestResponseModel;
import io.hotmoka.network.models.updates.ClassTagModel;
import io.hotmoka.network.models.updates.StateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@MessageMapping("/get")
public class WsGetController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private GetService nodeGetService;

    @MessageMapping("/takamakaCode")
    @SendTo("/get/takamakaCode")
    public TransactionReferenceModel getTakamakaCode() {
        return nodeGetService.getTakamakaCode();
    }

    @MessageMapping("/manifest")
    @SendTo("/get/manifest")
    public StorageReferenceModel getManifest() {
        return nodeGetService.getManifest();
    }

    @MessageMapping("/state")
    @SendTo("/get/state")
    public StateModel getState(StorageReferenceModel request) {
        return nodeGetService.getState(request);
    }

    @MessageMapping("/classTag")
    @SendTo("/get/classTag")
    public ClassTagModel getClassTag(StorageReferenceModel request) {
        return nodeGetService.getClassTag(request);
    }

    @MessageMapping("/request")
    @SendTo("/get/request")
    public TransactionRestRequestModel<?> getRequestAt(TransactionReferenceModel reference) {
        return nodeGetService.getRequest(reference);
    }

    @MessageMapping("/response")
    @SendTo("/get/response")
    public TransactionRestResponseModel<?> getResponseAt(TransactionReferenceModel reference) {
        return nodeGetService.getResponse(reference);
    }

    @MessageMapping("/polledResponse")
    @SendTo("/get/polledResponse")
    public TransactionRestResponseModel<?> getPolledResponseAt(TransactionReferenceModel reference) {
        return nodeGetService.getPolledResponse(reference);
    }

    @MessageMapping("/signatureAlgorithmForRequests")
    @SendTo("/get/signatureAlgorithmForRequests")
    public SignatureAlgorithmResponseModel getSignatureAlgorithmForRequests() {
        return new SignatureAlgorithmResponseModel(nodeGetService.getSignatureAlgorithmForRequests());
    }

    @MessageExceptionHandler
    public void handleException(Exception e) {
        String userId = ""; // TODO: should return error to the userid and not to all users
        if (e instanceof NetworkExceptionResponse)
            simpMessagingTemplate.convertAndSend("/get/errors", ((NetworkExceptionResponse) e).errorModel);
        else
            simpMessagingTemplate.convertAndSend("/get/errors", new ErrorModel(e));
    }
}
