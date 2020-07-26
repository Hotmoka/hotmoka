package io.hotmoka.network.internal.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.hotmoka.network.internal.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.internal.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.values.StorageReferenceModel;
import io.hotmoka.network.internal.models.values.StorageValueModel;
import io.hotmoka.network.internal.models.values.TransactionReferenceModel;
import io.hotmoka.network.internal.services.PostService;

@RestController
@RequestMapping("post")
public class PostController {

    @Autowired
    private PostService nodePostService;

    @PostMapping("/jarStoreTransaction")
    public @ResponseBody TransactionReferenceModel jarStoreTransaction(@RequestBody JarStoreTransactionRequestModel request) {
        return this.nodePostService.postJarStoreTransaction(request);
    }

    @PostMapping("/constructorCallTransaction")
    public @ResponseBody StorageReferenceModel constructorCallTransaction(@RequestBody ConstructorCallTransactionRequestModel request) {
        return this.nodePostService.postConstructorCallTransaction(request);
    }

    @PostMapping("/instanceMethodCallTransaction")
    public @ResponseBody StorageValueModel instanceMethodCallTransaction(@RequestBody InstanceMethodCallTransactionRequestModel request) {
        return this.nodePostService.postInstanceMethodCallTransaction(request);
    }

    @PostMapping("/staticMethodCallTransaction")
    public @ResponseBody StorageValueModel staticMethodCallTransaction(@RequestBody StaticMethodCallTransactionRequestModel request) {
        return this.nodePostService.postStaticMethodCallTransaction(request);
    }
}