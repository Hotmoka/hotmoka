package io.hotmoka.network.internal.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.hotmoka.network.internal.services.GetService;
import io.hotmoka.network.models.requests.TransactionRequestModel;
import io.hotmoka.network.models.updates.ClassTagModel;
import io.hotmoka.network.models.updates.StateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

@RestController
@RequestMapping("get")
public class GetController {

    @Autowired
    private GetService nodeGetService;

    @GetMapping("/takamakaCode")
    public @ResponseBody TransactionReferenceModel getTakamakaCode() {
        return nodeGetService.getTakamakaCode();
    }

    @GetMapping("/manifest")
    public @ResponseBody StorageReferenceModel getManifest() {
        return nodeGetService.getManifest();
    }

    @PostMapping("/state")
    public @ResponseBody StateModel getState(@RequestBody StorageReferenceModel request) {
        return nodeGetService.getState(request);
    }

    @PostMapping("/classTag")
    public @ResponseBody ClassTagModel getClassTag(@RequestBody StorageReferenceModel request) {
        return nodeGetService.getClassTag(request);
    }

    @GetMapping("/requestAt")
    public @ResponseBody TransactionRequestModel getRequestAt(@RequestBody TransactionReferenceModel reference) {
        return nodeGetService.getRequestAt(reference);
    }

    @GetMapping("/signatureAlgorithmForRequests")
    public @ResponseBody String getSignatureAlgorithmForRequests() {
        return nodeGetService.getSignatureAlgorithmForRequests();
    }
}