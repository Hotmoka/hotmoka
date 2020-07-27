package io.hotmoka.network.internal.rest;

import io.hotmoka.network.internal.services.AddService;
import io.hotmoka.network.models.requests.*;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.StorageValueModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("add")
public class AddController {

    @Autowired
    private AddService nodeAddService;

    @PostMapping("/jarStoreInitialTransaction")
    public @ResponseBody TransactionReferenceModel jarStoreInitialTransaction(@RequestBody JarStoreInitialTransactionRequestModel request) {
    	return nodeAddService.addJarStoreInitialTransaction(request);
    }

    @PostMapping("/gameteCreationTransaction")
    public @ResponseBody StorageReferenceModel gameteCreationTransaction(@RequestBody GameteCreationTransactionRequestModel request) {
        return nodeAddService.addGameteCreationTransaction(request);
    }

    @PostMapping("/redGreenGameteCreationTransaction")
    public @ResponseBody StorageReferenceModel redGreenGameteCreationTransaction(@RequestBody RedGreenGameteCreationTransactionRequestModel request) {
        return nodeAddService.addRedGreenGameteCreationTransaction(request);
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