package io.hotmoka.network.rest;

import io.hotmoka.network.model.transaction.GameteCreationTransactionRequestModel;
import io.hotmoka.network.model.transaction.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.model.transaction.RGGameteCreationTransactionRequestModel;
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
    public Object initializationTransaction() {
        return this.nodeAddService.addInitializationTransaction();
    }

    @PostMapping("/jarStoreTransaction")
    public Object jarStoreTransaction() {
        return this.nodeAddService.addJarStoreTransaction();
    }

    @PostMapping("/constructorCallTransaction")
    public Object constructorCallTransaction() {
        return this.nodeAddService.addConstructorCallTransaction();
    }

    @PostMapping("/instanceMethodCallTransaction")
    public Object instanceMethodCallTransaction() {
        return this.nodeAddService.addInstanceMethodCallTransaction();
    }

    @PostMapping("/staticMethodCallTransaction")
    public Object staticMethodCallTransaction() {
        return this.nodeAddService.addStaticMethodCallTransaction();
    }
}