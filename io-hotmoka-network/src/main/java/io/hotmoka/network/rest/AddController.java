package io.hotmoka.network.rest;

import io.hotmoka.network.model.transaction.TransactionRequestModel;
import io.hotmoka.network.service.add.NodeAddService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("add")
public class AddController {

    @Autowired
    private NodeAddService nodeAddService;

    @PostMapping("/jarStoreInitialTransaction")
    public Object jarStoreInitialTransaction(@RequestBody TransactionRequestModel transactionRequestModel) {
       return this.nodeAddService.addJarStoreInitialTransaction(transactionRequestModel);
    }

    @PostMapping("/gameteCreationTransaction")
    public Object gameteCreationTransaction() {
        return this.nodeAddService.addGameteCreationTransaction();
    }

    @PostMapping("/redGreenGameteCreationTransaction")
    public Object redGreenGameteCreationTransaction() {
        return this.nodeAddService.addRedGreenGameteCreationTransaction();
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