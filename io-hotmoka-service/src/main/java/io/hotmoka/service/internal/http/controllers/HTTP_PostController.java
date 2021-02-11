package io.hotmoka.service.internal.http.controllers;

import io.hotmoka.service.internal.services.PostService;
import io.hotmoka.service.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.service.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.service.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.service.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.service.models.values.TransactionReferenceModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("post")
public class HTTP_PostController {

    @Autowired
    private PostService nodePostService;

    @PostMapping("/jarStoreTransaction")
    public @ResponseBody
    TransactionReferenceModel jarStoreTransaction(@RequestBody JarStoreTransactionRequestModel request) {
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