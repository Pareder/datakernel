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
import java.util.Objects;
import java.util.Set;

public final class QueryResult {
	private final RecordScheme recordScheme;
	private final List<String> attributes;
	private final List<String> measures;
	private final List<String> sortedBy;

	private final List<Record> records;
	private final Record totals;
	private final int totalCount;

	private final Map<String, Object> filterAttributes;
	private final boolean metaOnly;
	private boolean resolveAttributes;

	private QueryResult(RecordScheme recordScheme, List<Record> records, Record totals, int totalCount,
	                    List<String> attributes, List<String> measures, List<String> sortedBy,
	                    Map<String, Object> filterAttributes, boolean metaOnly, boolean resolveAttributes) {
		this.recordScheme = recordScheme;
		this.records = records;
		this.totals = totals;
		this.totalCount = totalCount;
		this.attributes = attributes;
		this.measures = measures;
		this.sortedBy = sortedBy;
		this.filterAttributes = filterAttributes;
		this.metaOnly = metaOnly;
		this.resolveAttributes = resolveAttributes;
	}

	public static QueryResult create(RecordScheme recordScheme, List<Record> records, Record totals, int totalCount,
	                                 List<String> attributes, List<String> measures, List<String> sortedBy,
	                                 Map<String, Object> filterAttributes, boolean onlyMeta, boolean resolveAttributes) {
		return new QueryResult(recordScheme, records, totals, totalCount, attributes, measures, sortedBy,
				filterAttributes, onlyMeta, resolveAttributes);
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

	public boolean isMetaOnly() {
		return metaOnly;
	}

	public boolean isResolveAttributes() {
		return resolveAttributes;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("records", records)
				.add("totals", totals)
				.add("count", totalCount)
				.add("measures", measures)
				.add("sortedBy", sortedBy)
				.add("metaOnly", metaOnly)
				.add("resolveAttributes", resolveAttributes)
				.toString();
	}

	public static final class Drilldown {
		private final List<String> chain;
		private final Set<String> measures;

		private Drilldown(List<String> chain, Set<String> measures) {
			this.chain = chain;
			this.measures = measures;
		}

		public static Drilldown create(List<String> chain, Set<String> measures) {return new Drilldown(chain, measures);}

		public List<String> getChain() {
			return chain;
		}

		public Set<String> getMeasures() {
			return measures;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Drilldown drilldown1 = (Drilldown) o;
			return Objects.equals(chain, drilldown1.chain) &&
					Objects.equals(measures, drilldown1.measures);
		}

		@Override
		public int hashCode() {
			return Objects.hash(chain, measures);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("chain", chain)
					.add("measures", measures)
					.toString();
		}
	}
}
