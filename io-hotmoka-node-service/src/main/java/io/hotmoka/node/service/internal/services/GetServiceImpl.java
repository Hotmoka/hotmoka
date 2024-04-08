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

import org.springframework.stereotype.Service;

import io.hotmoka.network.responses.SignatureAlgorithmResponseModel;

@Service
public class GetServiceImpl extends AbstractService implements GetService {

	@Override
	public SignatureAlgorithmResponseModel getNameOfSignatureAlgorithmForRequests() {
		return wrapExceptions(() -> new SignatureAlgorithmResponseModel(getNode().getNameOfSignatureAlgorithmForRequests().toLowerCase()));
	}
}