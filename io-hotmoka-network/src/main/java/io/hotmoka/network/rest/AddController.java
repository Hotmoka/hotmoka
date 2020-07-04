package io.hotmoka.network.rest;

import io.hotmoka.network.model.storage.StorageModel;
import io.hotmoka.network.model.transaction.*;
import io.hotmoka.network.service.add.NodeAddService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("add")
public class AddController {

    @Autowired
    private NodeAddService nodeAddService;

    @PostMapping("/jarStoreInitialTransaction")
    public Object jarStoreInitialTransaction(@RequestBody JarStoreInitialTransactionRequestModel request) {
       return this.nodeAddService.addJarStoreInitialTransaction(request);
    }

    @PostMapping("/gameteCreationTransaction")
    public Object gameteCreationTransaction(@RequestBody GameteCreationTransactionRequestModel request) {
        return this.nodeAddService.addGameteCreationTransaction(request);
    }

    @PostMapping("/redGreenGameteCreationTransaction")
    public Object redGreenGameteCreationTransaction(@RequestBody RGGameteCreationTransactionRequestModel request) {
        return this.nodeAddService.addRedGreenGameteCreationTransaction(request);
    }

    @PostMapping("/initializationTransaction")
    public Object initializationTransaction(@RequestBody StorageModel request) {
        return this.nodeAddService.addInitializationTransaction(request);
    }

    @PostMapping("/jarStoreTransaction")
    public Object jarStoreTransaction(@RequestBody JarStoreTransactionRequestModel request) {
        return this.nodeAddService.addJarStoreTransaction(request);
    }

    @PostMapping("/constructorCallTransaction")
    public Object constructorCallTransaction(@RequestBody ConstructorCallTransactionRequestModel request) {
        return this.nodeAddService.addConstructorCallTransaction(request);
    }

    @PostMapping("/instanceMethodCallTransaction")
    public Object instanceMethodCallTransaction(@RequestBody MethodCallTransactionRequestModel request) {
        return this.nodeAddService.addInstanceMethodCallTransaction(request);
    }

    @PostMapping("/staticMethodCallTransaction")
    public Object staticMethodCallTransaction() {
        return this.nodeAddService.addStaticMethodCallTransaction();
    }
}