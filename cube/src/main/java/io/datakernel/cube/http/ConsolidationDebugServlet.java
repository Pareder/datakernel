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

package io.datakernel.cube.http;

import com.google.gson.*;
import io.datakernel.aggregation.PrimaryKey;
import io.datakernel.async.Stage;
import io.datakernel.cube.Cube;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.http.*;

import java.lang.reflect.Type;

import static io.datakernel.bytebuf.ByteBufStrings.wrapUtf8;
import static io.datakernel.http.HttpHeaderValue.ofContentType;
import static io.datakernel.http.HttpHeaders.CONTENT_TYPE;

public final class ConsolidationDebugServlet extends AsyncServletWithStats {
	private final Gson gson;
	private final Cube cube;

	private ConsolidationDebugServlet(Eventloop eventloop, Cube cube) {
		super(eventloop);
		this.cube = cube;
		this.gson = new GsonBuilder().registerTypeAdapter(PrimaryKey.class, new PrimaryKeySerializer()).create();
	}

	public static ConsolidationDebugServlet create(Eventloop eventloop, Cube cube) {
		return new ConsolidationDebugServlet(eventloop, cube);
	}

	public static class PrimaryKeySerializer implements JsonSerializer<PrimaryKey> {
		@Override
		public JsonElement serialize(PrimaryKey primaryKey, Type type, JsonSerializationContext ctx) {
			JsonArray jsonArray = new JsonArray();

			for (int i = 0; i < primaryKey.size(); ++i) {
				jsonArray.add(ctx.serialize(primaryKey.get(i)));
			}

			return jsonArray;
		}
	}

	@Override
	public Stage<HttpResponse> doServe(HttpRequest request) {
		return Stage.of(HttpResponse.ok200()
				.withHeader(CONTENT_TYPE, ofContentType(ContentType.of(MediaTypes.JSON)))
				.withBody(wrapUtf8(gson.toJson(cube.getConsolidationDebugInfo()))));
	}
}
