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

/**
 * This class is generated by jOOQ
 */
package io.datakernel.aggregation_db.sql.tables;


import io.datakernel.aggregation_db.sql.DefaultSchema;
import io.datakernel.aggregation_db.sql.Keys;
import io.datakernel.aggregation_db.sql.tables.records.AggregationDbChunkRecord;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Identity;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.6.2"
	},
	comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AggregationDbChunk extends TableImpl<AggregationDbChunkRecord> {

	private static final long serialVersionUID = -455101307;

	/**
	 * The reference instance of <code>aggregation_db_chunk</code>
	 */
	public static final AggregationDbChunk AGGREGATION_DB_CHUNK = new AggregationDbChunk();

	/**
	 * The class holding records for this type
	 */
	@Override
	public Class<AggregationDbChunkRecord> getRecordType() {
		return AggregationDbChunkRecord.class;
	}

	/**
	 * The column <code>aggregation_db_chunk.id</code>.
	 */
	public final TableField<AggregationDbChunkRecord, Long> ID = createField("id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

	/**
	 * The column <code>aggregation_db_chunk.created</code>.
	 */
	public final TableField<AggregationDbChunkRecord, Timestamp> CREATED = createField("created", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false).defaulted(true), this, "");

	/**
	 * The column <code>aggregation_db_chunk.aggregation_id</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> AGGREGATION_ID = createField("aggregation_id", org.jooq.impl.SQLDataType.VARCHAR.length(100).nullable(false).defaulted(true), this, "");

	/**
	 * The column <code>aggregation_db_chunk.revision_id</code>.
	 */
	public final TableField<AggregationDbChunkRecord, Integer> REVISION_ID = createField("revision_id", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

	/**
	 * The column <code>aggregation_db_chunk.min_revision_id</code>.
	 */
	public final TableField<AggregationDbChunkRecord, Integer> MIN_REVISION_ID = createField("min_revision_id", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

	/**
	 * The column <code>aggregation_db_chunk.max_revision_id</code>.
	 */
	public final TableField<AggregationDbChunkRecord, Integer> MAX_REVISION_ID = createField("max_revision_id", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

	/**
	 * The column <code>aggregation_db_chunk.count</code>.
	 */
	public final TableField<AggregationDbChunkRecord, Integer> COUNT = createField("count", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

	/**
	 * The column <code>aggregation_db_chunk.consolidated_revision_id</code>.
	 */
	public final TableField<AggregationDbChunkRecord, Integer> CONSOLIDATED_REVISION_ID = createField("consolidated_revision_id", org.jooq.impl.SQLDataType.INTEGER, this, "");

	/**
	 * The column <code>aggregation_db_chunk.consolidation_started</code>.
	 */
	public final TableField<AggregationDbChunkRecord, Timestamp> CONSOLIDATION_STARTED = createField("consolidation_started", org.jooq.impl.SQLDataType.TIMESTAMP, this, "");

	/**
	 * The column <code>aggregation_db_chunk.consolidation_completed</code>.
	 */
	public final TableField<AggregationDbChunkRecord, Timestamp> CONSOLIDATION_COMPLETED = createField("consolidation_completed", org.jooq.impl.SQLDataType.TIMESTAMP, this, "");

	/**
	 * The column <code>aggregation_db_chunk.keys</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> KEYS = createField("keys", org.jooq.impl.SQLDataType.VARCHAR.length(1000).nullable(false).defaulted(true), this, "");

	/**
	 * The column <code>aggregation_db_chunk.fields</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> FIELDS = createField("fields", org.jooq.impl.SQLDataType.VARCHAR.length(1000).nullable(false).defaulted(true), this, "");

	/**
	 * The column <code>aggregation_db_chunk.d1_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D1_MIN = createField("d1_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d1_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D1_MAX = createField("d1_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d2_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D2_MIN = createField("d2_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d2_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D2_MAX = createField("d2_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d3_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D3_MIN = createField("d3_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d3_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D3_MAX = createField("d3_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d4_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D4_MIN = createField("d4_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d4_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D4_MAX = createField("d4_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d5_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D5_MIN = createField("d5_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d5_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D5_MAX = createField("d5_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d6_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D6_MIN = createField("d6_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d6_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D6_MAX = createField("d6_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d7_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D7_MIN = createField("d7_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d7_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D7_MAX = createField("d7_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d8_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D8_MIN = createField("d8_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d8_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D8_MAX = createField("d8_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d9_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D9_MIN = createField("d9_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d9_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D9_MAX = createField("d9_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d10_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D10_MIN = createField("d10_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d10_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D10_MAX = createField("d10_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d11_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D11_MIN = createField("d11_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d11_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D11_MAX = createField("d11_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d12_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D12_MIN = createField("d12_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d12_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D12_MAX = createField("d12_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d13_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D13_MIN = createField("d13_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d13_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D13_MAX = createField("d13_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d14_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D14_MIN = createField("d14_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d14_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D14_MAX = createField("d14_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d15_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D15_MIN = createField("d15_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d15_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D15_MAX = createField("d15_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d16_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D16_MIN = createField("d16_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d16_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D16_MAX = createField("d16_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d17_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D17_MIN = createField("d17_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d17_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D17_MAX = createField("d17_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d18_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D18_MIN = createField("d18_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d18_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D18_MAX = createField("d18_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d19_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D19_MIN = createField("d19_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d19_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D19_MAX = createField("d19_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d20_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D20_MIN = createField("d20_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d20_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D20_MAX = createField("d20_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d21_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D21_MIN = createField("d21_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d21_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D21_MAX = createField("d21_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d22_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D22_MIN = createField("d22_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d22_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D22_MAX = createField("d22_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d23_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D23_MIN = createField("d23_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d23_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D23_MAX = createField("d23_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d24_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D24_MIN = createField("d24_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d24_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D24_MAX = createField("d24_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d25_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D25_MIN = createField("d25_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d25_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D25_MAX = createField("d25_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d26_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D26_MIN = createField("d26_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d26_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D26_MAX = createField("d26_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d27_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D27_MIN = createField("d27_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d27_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D27_MAX = createField("d27_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d28_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D28_MIN = createField("d28_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d28_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D28_MAX = createField("d28_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d29_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D29_MIN = createField("d29_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d29_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D29_MAX = createField("d29_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d30_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D30_MIN = createField("d30_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d30_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D30_MAX = createField("d30_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d31_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D31_MIN = createField("d31_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d31_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D31_MAX = createField("d31_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d32_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D32_MIN = createField("d32_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d32_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D32_MAX = createField("d32_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d33_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D33_MIN = createField("d33_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d33_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D33_MAX = createField("d33_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d34_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D34_MIN = createField("d34_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d34_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D34_MAX = createField("d34_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d35_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D35_MIN = createField("d35_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d35_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D35_MAX = createField("d35_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d36_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D36_MIN = createField("d36_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d36_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D36_MAX = createField("d36_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d37_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D37_MIN = createField("d37_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d37_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D37_MAX = createField("d37_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d38_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D38_MIN = createField("d38_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d38_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D38_MAX = createField("d38_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d39_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D39_MIN = createField("d39_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d39_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D39_MAX = createField("d39_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d40_min</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D40_MIN = createField("d40_min", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * The column <code>aggregation_db_chunk.d40_max</code>.
	 */
	public final TableField<AggregationDbChunkRecord, String> D40_MAX = createField("d40_max", org.jooq.impl.SQLDataType.CLOB, this, "");

	/**
	 * Create a <code>aggregation_db_chunk</code> table reference
	 */
	public AggregationDbChunk() {
		this("aggregation_db_chunk", null);
	}

	/**
	 * Create an aliased <code>aggregation_db_chunk</code> table reference
	 */
	public AggregationDbChunk(String alias) {
		this(alias, AGGREGATION_DB_CHUNK);
	}

	private AggregationDbChunk(String alias, Table<AggregationDbChunkRecord> aliased) {
		this(alias, aliased, null);
	}

	private AggregationDbChunk(String alias, Table<AggregationDbChunkRecord> aliased, Field<?>[] parameters) {
		super(alias, DefaultSchema.DEFAULT_SCHEMA, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identity<AggregationDbChunkRecord, Long> getIdentity() {
		return Keys.IDENTITY_AGGREGATION_DB_CHUNK;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UniqueKey<AggregationDbChunkRecord> getPrimaryKey() {
		return Keys.KEY_AGGREGATION_DB_CHUNK_PRIMARY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<UniqueKey<AggregationDbChunkRecord>> getKeys() {
		return Arrays.<UniqueKey<AggregationDbChunkRecord>>asList(Keys.KEY_AGGREGATION_DB_CHUNK_PRIMARY);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AggregationDbChunk as(String alias) {
		return new AggregationDbChunk(alias, this);
	}

	/**
	 * Rename this table
	 */
	public AggregationDbChunk rename(String name) {
		return new AggregationDbChunk(name, null);
	}
}
