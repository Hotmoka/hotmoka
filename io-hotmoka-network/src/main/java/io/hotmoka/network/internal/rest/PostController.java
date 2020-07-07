package io.hotmoka.network.internal.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.hotmoka.network.internal.services.NodePostService;

@RestController
@RequestMapping("post")
public class PostController {

    @Autowired
    private NodePostService nodePostService;

    @PostMapping("/jarStoreTransaction")
    public Object jarStoreTransaction() {
        return this.nodePostService.postJarStoreTransaction();
    }

    @PostMapping("/constructorCallTransaction")
    public Object constructorCallTransaction() {
        return this.nodePostService.postConstructorCallTransaction();
    }

    @PostMapping("/instanceMethodCallTransaction")
    public Object instanceMethodCallTransaction() {
        return this.nodePostService.postInstanceMethodCallTransaction();
    }

    @PostMapping("/staticMethodCallTransaction")
    public Object staticMethodCallTransaction() {
        return this.nodePostService.postStaticMethodCallTransaction();
    }
}