/*
 * Copyright (C) 2015 SoftIndex LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.datakernel.uikernel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.datakernel.common.Preconditions.checkNotNull;

public final class CreateResponse<K> {
	private final K id;
	private final Map<String, List<String>> errors;

	private CreateResponse(K id, Map<String, List<String>> errors) {
		this.id = checkNotNull(id, "Id cannot be null in CreateResponse");
		this.errors = checkNotNull(errors, "Errors cannot be null in CreateResponse");
	}

	public static <K> CreateResponse<K> of(K id) {
		return new CreateResponse<>(id, Collections.emptyMap());
	}

	public static <K> CreateResponse<K> of(K id, Map<String, List<String>> errors) {
		return new CreateResponse<>(id, errors);
	}

	String toJson(Gson gson, Class<K> idType) {
		JsonObject result = new JsonObject();
		result.add("data", gson.toJsonTree(id, idType));
		result.add("errors", gson.toJsonTree(errors));
		return gson.toJson(result);
	}
}
