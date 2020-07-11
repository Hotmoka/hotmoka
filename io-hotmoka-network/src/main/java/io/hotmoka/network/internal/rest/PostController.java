package io.hotmoka.network.internal.rest;

import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.models.transactions.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.JarStoreTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.MethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.TransactionReferenceModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.hotmoka.network.internal.services.NodePostService;

@RestController
@RequestMapping("post")
public class PostController {

    @Autowired
    private NodePostService nodePostService;

    @PostMapping("/jarStoreTransaction")
    public @ResponseBody TransactionReferenceModel jarStoreTransaction(@RequestBody JarStoreTransactionRequestModel request) {
        return this.nodePostService.postJarStoreTransaction(request);
    }

    @PostMapping("/constructorCallTransaction")
    public @ResponseBody StorageReferenceModel constructorCallTransaction(@RequestBody ConstructorCallTransactionRequestModel request) {
        return this.nodePostService.postConstructorCallTransaction(request);
    }

    @PostMapping("/instanceMethodCallTransaction")
    public @ResponseBody StorageValueModel instanceMethodCallTransaction(@RequestBody MethodCallTransactionRequestModel request) {
        return this.nodePostService.postInstanceMethodCallTransaction(request);
    }

    @PostMapping("/staticMethodCallTransaction")
    public @ResponseBody StorageValueModel staticMethodCallTransaction(@RequestBody MethodCallTransactionRequestModel request) {
        return this.nodePostService.postStaticMethodCallTransaction(request);
    }
}