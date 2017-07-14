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

package io.datakernel.cube;

import com.google.common.base.MoreObjects;

import java.util.List;
import java.util.Map;

public final class QueryResult {
	private final RecordScheme recordScheme;
	private final List<String> attributes;
	private final List<String> measures;
	private final List<String> sortedBy;

	private final List<Record> records;
	private final Record totals;
	private final int totalCount;
	private Map<String, String> childParentRelations;

	private final Map<String, Object> filterAttributes;

	private QueryResult(RecordScheme recordScheme, List<Record> records, Record totals, int totalCount,
	                    List<String> attributes, List<String> measures, List<String> sortedBy,
	                    Map<String, Object> filterAttributes, Map<String, String> childParentRelations) {
		this.recordScheme = recordScheme;
		this.records = records;
		this.totals = totals;
		this.totalCount = totalCount;
		this.attributes = attributes;
		this.measures = measures;
		this.sortedBy = sortedBy;
		this.filterAttributes = filterAttributes;
		this.childParentRelations = childParentRelations;
	}

	public static QueryResult create(RecordScheme recordScheme, List<Record> records, Record totals, int totalCount,
	                                 List<String> attributes, List<String> measures, List<String> sortedBy,
	                                 Map<String, Object> filterAttributes, Map<String, String> childParentRelations) {
		return new QueryResult(recordScheme, records, totals, totalCount, attributes, measures, sortedBy, filterAttributes, childParentRelations);
	}

	public RecordScheme getRecordScheme() {
		return recordScheme;
	}

	public List<String> getAttributes() {
		return attributes;
	}

	public List<String> getMeasures() {
		return measures;
	}

	public List<Record> getRecords() {
		return records;
	}

	public Record getTotals() {
		return totals;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public Map<String, Object> getFilterAttributes() {
		return filterAttributes;
	}

	public List<String> getSortedBy() {
		return sortedBy;
	}

	public Map<String, String> getChildParentRelations() {
		return childParentRelations;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("attributes", attributes)
				.add("measures", measures)
				.add("records", records)
				.add("totals", totals)
				.add("count", totalCount)
				.add("sortedBy", sortedBy)
				.add("getChildParentRelations", childParentRelations)
				.toString();
	}
}
