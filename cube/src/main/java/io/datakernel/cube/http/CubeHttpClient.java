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

import com.google.gson.TypeAdapter;
import io.datakernel.aggregation.AggregationPredicate;
import io.datakernel.async.AsyncCallbacks;
import io.datakernel.async.ResultCallback;
import io.datakernel.bytebuf.ByteBufStrings;
import io.datakernel.cube.CubeQuery;
import io.datakernel.cube.ICube;
import io.datakernel.cube.QueryResult;
import io.datakernel.eventloop.Eventloop;
import io.datakernel.exception.ParseException;
import io.datakernel.http.AsyncHttpClient;
import io.datakernel.http.HttpRequest;
import io.datakernel.http.HttpUtils;
import io.datakernel.http.IAsyncHttpClient;
import io.datakernel.utils.GsonAdapters.TypeAdapterRegistryImpl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;

import static com.google.common.collect.Maps.newLinkedHashMap;
import static io.datakernel.cube.http.Utils.*;

public final class CubeHttpClient implements ICube {
	private final Eventloop eventloop;
	private final String url;
	private final IAsyncHttpClient httpClient;
	private final TypeAdapterRegistryImpl registry;
	private TypeAdapter<QueryResult> queryResultJson;
	private TypeAdapter<AggregationPredicate> aggregationPredicateJson;
	private final Map<String, Type> attributeTypes = newLinkedHashMap();
	private final Map<String, Type> measureTypes = newLinkedHashMap();

	private CubeHttpClient(Eventloop eventloop, IAsyncHttpClient httpClient, String url, TypeAdapterRegistryImpl registry) {
		this.eventloop = eventloop;
		this.url = url.replaceAll("/$", "");
		this.httpClient = httpClient;
		this.registry = registry;
	}

	public static CubeHttpClient create(Eventloop eventloop, AsyncHttpClient httpClient, String cubeServletUrl) {
		return new CubeHttpClient(eventloop, httpClient, cubeServletUrl, createCubeTypeAdaptersRegistry());
	}

	public static CubeHttpClient create(Eventloop eventloop, AsyncHttpClient httpClient, URI cubeServletUrl) {
		return create(eventloop, httpClient, cubeServletUrl.toString());
	}

	public CubeHttpClient withAttribute(String attribute, Type type) {
		attributeTypes.put(attribute, type);
		return this;
	}

	public CubeHttpClient withMeasure(String measureId, Class<?> type) {
		measureTypes.put(measureId, type);
		return this;
	}

	private TypeAdapter<AggregationPredicate> getAggregationPredicateJson() {
		if (aggregationPredicateJson == null) {
			aggregationPredicateJson = AggregationPredicateGsonAdapter.create(registry, attributeTypes, measureTypes);
		}
		return aggregationPredicateJson;
	}

	private TypeAdapter<QueryResult> getQueryResultJson() {
		if (queryResultJson == null) {
			queryResultJson = QueryResultGsonAdapter.create(registry, attributeTypes, measureTypes);
		}
		return queryResultJson;
	}

	@Override
	public Map<String, Type> getAttributeTypes() {
		return attributeTypes;
	}

	@Override
	public Map<String, Type> getMeasureTypes() {
		return measureTypes;
	}

	@Override
	public void query(CubeQuery query, final ResultCallback<QueryResult> callback) {
		httpClient.send(buildRequest(query)).whenComplete((httpResponse, throwable) -> {
			if (throwable != null) {
				callback.setException(AsyncCallbacks.throwableToException(throwable));
			} else {
				String response;
				try {
					response = ByteBufStrings.decodeUtf8(httpResponse.getBody()); // TODO getBodyAsString
				} catch (ParseException e) {
					callback.setException(new ParseException("Cube HTTP query failed. Invalid data received", e));
					return;
				}

				if (httpResponse.getCode() != 200) {
					callback.setException(new ParseException("Cube HTTP query failed. Response code: "
							+ httpResponse.getCode() + " Body: " + response));
					return;
				}

				QueryResult result;
				try {
					result = getQueryResultJson().fromJson(response);
				} catch (IOException e) {
					callback.setException(new ParseException("Cube HTTP query failed. Invalid data received", e));
					return;
				}

				callback.setResult(result);
			}
		});
	}

	private HttpRequest buildRequest(CubeQuery query) {
		Map<String, String> urlParams = newLinkedHashMap();

		urlParams.put(ATTRIBUTES_PARAM, JOINER.join(query.getAttributes()));
		urlParams.put(MEASURES_PARAM, JOINER.join(query.getMeasures()));
		urlParams.put(WHERE_PARAM, getAggregationPredicateJson().toJson(query.getWhere()));
		urlParams.put(SORT_PARAM, formatOrderings(query.getOrderings()));
		urlParams.put(HAVING_PARAM, getAggregationPredicateJson().toJson(query.getHaving()));
		if (query.getLimit() != null)
			urlParams.put(LIMIT_PARAM, query.getLimit().toString());
		if (query.getOffset() != null)
			urlParams.put(OFFSET_PARAM, query.getOffset().toString());
		urlParams.put(REPORT_TYPE_PARAM, query.getReportType().toString().toLowerCase());
		String url = this.url + "/" + "?" + HttpUtils.renderQueryString(urlParams);

		return HttpRequest.get(url);
	}
}
