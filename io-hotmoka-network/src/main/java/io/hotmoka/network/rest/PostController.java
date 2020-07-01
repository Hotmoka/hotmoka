package io.hotmoka.network.rest;

import io.hotmoka.network.service.post.NodePostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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