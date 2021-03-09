package io.hotmoka.service.internal.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.hotmoka.service.internal.services.AddService;
import io.hotmoka.service.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.service.models.requests.InitializationTransactionRequestModel;
import io.hotmoka.service.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.service.models.requests.JarStoreInitialTransactionRequestModel;
import io.hotmoka.service.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.service.models.requests.GameteCreationTransactionRequestModel;
import io.hotmoka.service.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.service.models.values.StorageReferenceModel;
import io.hotmoka.service.models.values.StorageValueModel;
import io.hotmoka.service.models.values.TransactionReferenceModel;

@RestController
@RequestMapping("add")
public class HTTP_AddController {

    @Autowired
    private AddService nodeAddService;

    @PostMapping("/jarStoreInitialTransaction")
    public @ResponseBody TransactionReferenceModel jarStoreInitialTransaction(@RequestBody JarStoreInitialTransactionRequestModel request) {
    	return nodeAddService.addJarStoreInitialTransaction(request);
    }

    @PostMapping("/gameteCreationTransaction")
    public @ResponseBody StorageReferenceModel redGreenGameteCreationTransaction(@RequestBody GameteCreationTransactionRequestModel request) {
        return nodeAddService.addGameteCreationTransaction(request);
    }

    @PostMapping("/initializationTransaction")
    public @ResponseBody ResponseEntity<Void> initializationTransaction(@RequestBody InitializationTransactionRequestModel request) {
        return nodeAddService.addInitializationTransaction(request);
    }

    @PostMapping("/jarStoreTransaction")
    public @ResponseBody TransactionReferenceModel jarStoreTransaction(@RequestBody JarStoreTransactionRequestModel request) {
        return nodeAddService.addJarStoreTransaction(request);
    }

    @PostMapping("/constructorCallTransaction")
    public @ResponseBody StorageReferenceModel constructorCallTransaction(@RequestBody ConstructorCallTransactionRequestModel request) {
        return nodeAddService.addConstructorCallTransaction(request);
    }

    @PostMapping("/instanceMethodCallTransaction")
    public @ResponseBody StorageValueModel instanceMethodCallTransaction(@RequestBody InstanceMethodCallTransactionRequestModel request) {
        return nodeAddService.addInstanceMethodCallTransaction(request);
    }

    @PostMapping("/staticMethodCallTransaction")
    public @ResponseBody StorageValueModel staticMethodCallTransaction(@RequestBody StaticMethodCallTransactionRequestModel request) {
        return nodeAddService.addStaticMethodCallTransaction(request);
    }
}