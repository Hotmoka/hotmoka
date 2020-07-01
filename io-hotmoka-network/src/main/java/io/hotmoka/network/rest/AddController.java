package io.hotmoka.network.rest;

import io.hotmoka.network.service.add.NodeAddService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("add")
public class AddController {

    @Autowired
    private NodeAddService nodeAddService;

    @RequestMapping("/jarStoreInitialTransaction")
    public Object jarStoreInitialTransaction() {
       return this.nodeAddService.addJarStoreInitialTransaction();
    }

    @RequestMapping("/gameteCreationTransaction")
    public Object gameteCreationTransaction() {
        return this.nodeAddService.addGameteCreationTransaction();
    }

    @RequestMapping("/redGreenGameteCreationTransaction")
    public Object redGreenGameteCreationTransaction() {
        return this.nodeAddService.addRedGreenGameteCreationTransaction();
    }

    @RequestMapping("/initializationTransaction")
    public Object initializationTransaction() {
        return this.nodeAddService.addInitializationTransaction();
    }

    @RequestMapping("/jarStoreTransaction")
    public Object jarStoreTransaction() {
        return this.nodeAddService.addJarStoreTransaction();
    }

    @RequestMapping("/constructorCallTransaction")
    public Object constructorCallTransaction() {
        return this.nodeAddService.addConstructorCallTransaction();
    }

    @RequestMapping("/instanceMethodCallTransaction")
    public Object instanceMethodCallTransaction() {
        return this.nodeAddService.addInstanceMethodCallTransaction();
    }

    @RequestMapping("/staticMethodCallTransaction")
    public Object staticMethodCallTransaction() {
        return this.nodeAddService.addStaticMethodCallTransaction();
    }
}