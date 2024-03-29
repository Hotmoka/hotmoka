/*
Copyright 2021 Dinu Berinde and Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.service.internal.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.hotmoka.network.nodes.NodeInfoModel;
import io.hotmoka.network.requests.TransactionRestRequestModel;
import io.hotmoka.network.responses.SignatureAlgorithmResponseModel;
import io.hotmoka.network.responses.TransactionRestResponseModel;
import io.hotmoka.network.updates.ClassTagModel;
import io.hotmoka.network.updates.StateModel;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.network.values.TransactionReferenceModel;
import io.hotmoka.node.service.internal.services.GetService;

@RestController
@RequestMapping("get")
public class HTTP_GetController {

    @Autowired
    private GetService nodeGetService;

    @GetMapping("/takamakaCode")
    public @ResponseBody TransactionReferenceModel getTakamakaCode() {
        return nodeGetService.getTakamakaCode();
    }

    @GetMapping("/manifest")
    public @ResponseBody StorageReferenceModel getManifest() {
        return nodeGetService.getManifest();
    }

    @GetMapping("/nodeID")
    public @ResponseBody NodeInfoModel getNodeID() {
        return nodeGetService.getNodeID();
    }

    @PostMapping("/state")
    public @ResponseBody StateModel getState(@RequestBody StorageReferenceModel request) {
        return nodeGetService.getState(request);
    }

    @PostMapping("/classTag")
    public @ResponseBody ClassTagModel getClassTag(@RequestBody StorageReferenceModel request) {
        return nodeGetService.getClassTag(request);
    }

    @PostMapping("/request")
    public @ResponseBody
    TransactionRestRequestModel<?> getRequestAt(@RequestBody TransactionReferenceModel reference) {
        return nodeGetService.getRequest(reference);
    }

    @PostMapping("/response")
    public @ResponseBody TransactionRestResponseModel<?> getResponseAt(@RequestBody TransactionReferenceModel reference) {
        return nodeGetService.getResponse(reference);
    }

    @PostMapping("/polledResponse")
    public @ResponseBody TransactionRestResponseModel<?> getPolledResponseAt(@RequestBody TransactionReferenceModel reference) {
        return nodeGetService.getPolledResponse(reference);
    }

    @GetMapping("/nameOfSignatureAlgorithmForRequests")
    public @ResponseBody
    SignatureAlgorithmResponseModel getNameOfSignatureAlgorithmForRequests() {
        return nodeGetService.getNameOfSignatureAlgorithmForRequests();
    }
}