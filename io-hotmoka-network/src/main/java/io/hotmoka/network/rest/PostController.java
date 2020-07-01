package io.hotmoka.network.rest;

import io.hotmoka.network.service.post.NodePostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("post")
public class PostController {

    @Autowired
    private NodePostService nodePostService;

    @RequestMapping("/jarStoreTransaction")
    public Object jarStoreTransaction() {
        return this.nodePostService.postJarStoreTransaction();
    }

    @RequestMapping("/constructorCallTransaction")
    public Object constructorCallTransaction() {
        return this.nodePostService.postConstructorCallTransaction();
    }

    @RequestMapping("/instanceMethodCallTransaction")
    public Object instanceMethodCallTransaction() {
        return this.nodePostService.postInstanceMethodCallTransaction();
    }

    @RequestMapping("/staticMethodCallTransaction")
    public Object staticMethodCallTransaction() {
        return this.nodePostService.postStaticMethodCallTransaction();
    }
}