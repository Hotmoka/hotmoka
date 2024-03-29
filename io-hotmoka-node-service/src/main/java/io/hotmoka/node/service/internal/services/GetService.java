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

package io.hotmoka.node.service.internal.services;

import io.hotmoka.network.nodes.NodeInfoModel;
import io.hotmoka.network.requests.TransactionRestRequestModel;
import io.hotmoka.network.responses.SignatureAlgorithmResponseModel;
import io.hotmoka.network.responses.TransactionRestResponseModel;
import io.hotmoka.network.updates.ClassTagModel;
import io.hotmoka.network.updates.StateModel;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.network.values.TransactionReferenceModel;

public interface GetService {
    TransactionReferenceModel getTakamakaCode();
    StorageReferenceModel getManifest();
    NodeInfoModel getNodeID();
    StateModel getState(StorageReferenceModel request);
    ClassTagModel getClassTag(StorageReferenceModel request);
    TransactionRestRequestModel<?> getRequest(TransactionReferenceModel reference);
	SignatureAlgorithmResponseModel getNameOfSignatureAlgorithmForRequests();
    TransactionRestResponseModel<?> getResponse(TransactionReferenceModel reference);
    TransactionRestResponseModel<?> getPolledResponse(TransactionReferenceModel reference);
}