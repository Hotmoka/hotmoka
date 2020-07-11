package io.hotmoka.network.internal.rest;

import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.models.transactions.*;
import io.hotmoka.network.internal.services.NodeAddService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("add")
public class AddController {

    @Autowired
    private NodeAddService nodeAddService;

    @PostMapping("/jarStoreInitialTransaction")
    public @ResponseBody TransactionReferenceModel jarStoreInitialTransaction(@RequestBody JarStoreInitialTransactionRequestModel request) {
       return this.nodeAddService.addJarStoreInitialTransaction(request);
    }

    @PostMapping("/gameteCreationTransaction")
    public @ResponseBody StorageReferenceModel gameteCreationTransaction(@RequestBody GameteCreationTransactionRequestModel request) {
        return this.nodeAddService.addGameteCreationTransaction(request);
    }

    @PostMapping("/redGreenGameteCreationTransaction")
    public @ResponseBody StorageReferenceModel redGreenGameteCreationTransaction(@RequestBody RGGameteCreationTransactionRequestModel request) {
        return this.nodeAddService.addRedGreenGameteCreationTransaction(request);
    }

    @PostMapping("/initializationTransaction")
    public Object initializationTransaction(@RequestBody InitializationTransactionRequestModel request) {
        return this.nodeAddService.addInitializationTransaction(request);
    }

    @PostMapping("/jarStoreTransaction")
    public @ResponseBody TransactionReferenceModel jarStoreTransaction(@RequestBody JarStoreTransactionRequestModel request) {
        return this.nodeAddService.addJarStoreTransaction(request);
    }

    @PostMapping("/constructorCallTransaction")
    public @ResponseBody StorageReferenceModel constructorCallTransaction(@RequestBody ConstructorCallTransactionRequestModel request) {
        return this.nodeAddService.addConstructorCallTransaction(request);
    }

    @PostMapping("/instanceMethodCallTransaction")
    public @ResponseBody StorageValueModel instanceMethodCallTransaction(@RequestBody MethodCallTransactionRequestModel request) {
        return this.nodeAddService.addInstanceMethodCallTransaction(request);
    }

    @PostMapping("/staticMethodCallTransaction")
    public @ResponseBody StorageValueModel staticMethodCallTransaction(@RequestBody MethodCallTransactionRequestModel request) {
        return this.nodeAddService.addStaticMethodCallTransaction(request);
    }
}