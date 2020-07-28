package io.hotmoka.network.internal.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.hotmoka.network.internal.services.PostService;
import io.hotmoka.network.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

@RestController
@RequestMapping("post")
public class PostController {

    @Autowired
    private PostService nodePostService;

    @PostMapping("/jarStoreTransaction")
    public @ResponseBody TransactionReferenceModel jarStoreTransaction(@RequestBody JarStoreTransactionRequestModel request) {
        return nodePostService.postJarStoreTransaction(request);
    }

    @PostMapping("/constructorCallTransaction")
    public @ResponseBody TransactionReferenceModel constructorCallTransaction(@RequestBody ConstructorCallTransactionRequestModel request) {
        return nodePostService.postConstructorCallTransaction(request);
    }

    @PostMapping("/instanceMethodCallTransaction")
    public @ResponseBody TransactionReferenceModel instanceMethodCallTransaction(@RequestBody InstanceMethodCallTransactionRequestModel request) {
        return nodePostService.postInstanceMethodCallTransaction(request);
    }

    @PostMapping("/staticMethodCallTransaction")
    public @ResponseBody TransactionReferenceModel staticMethodCallTransaction(@RequestBody StaticMethodCallTransactionRequestModel request) {
        return nodePostService.postStaticMethodCallTransaction(request);
    }
}