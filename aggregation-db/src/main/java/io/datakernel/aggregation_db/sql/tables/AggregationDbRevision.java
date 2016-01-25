/**
 * This class is generated by jOOQ
 */
package io.datakernel.aggregation_db.sql.tables;


import io.datakernel.aggregation_db.sql.DefaultSchema;
import io.datakernel.aggregation_db.sql.Keys;
import io.datakernel.aggregation_db.sql.tables.records.AggregationDbRevisionRecord;

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
		"jOOQ version:3.7.2"
	},
	comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AggregationDbRevision extends TableImpl<AggregationDbRevisionRecord> {

	private static final long serialVersionUID = -1113850751;

	/**
	 * The reference instance of <code>aggregation_db_revision</code>
	 */
	public static final AggregationDbRevision AGGREGATION_DB_REVISION = new AggregationDbRevision();

	/**
	 * The class holding records for this type
	 */
	@Override
	public Class<AggregationDbRevisionRecord> getRecordType() {
		return AggregationDbRevisionRecord.class;
	}

	/**
	 * The column <code>aggregation_db_revision.id</code>.
	 */
	public final TableField<AggregationDbRevisionRecord, Integer> ID = createField("id", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

	/**
	 * Create a <code>aggregation_db_revision</code> table reference
	 */
	public AggregationDbRevision() {
		this("aggregation_db_revision", null);
	}

	/**
	 * Create an aliased <code>aggregation_db_revision</code> table reference
	 */
	public AggregationDbRevision(String alias) {
		this(alias, AGGREGATION_DB_REVISION);
	}

	private AggregationDbRevision(String alias, Table<AggregationDbRevisionRecord> aliased) {
		this(alias, aliased, null);
	}

	private AggregationDbRevision(String alias, Table<AggregationDbRevisionRecord> aliased, Field<?>[] parameters) {
		super(alias, DefaultSchema.DEFAULT_SCHEMA, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identity<AggregationDbRevisionRecord, Integer> getIdentity() {
		return Keys.IDENTITY_AGGREGATION_DB_REVISION;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UniqueKey<AggregationDbRevisionRecord> getPrimaryKey() {
		return Keys.KEY_AGGREGATION_DB_REVISION_PRIMARY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<UniqueKey<AggregationDbRevisionRecord>> getKeys() {
		return Arrays.<UniqueKey<AggregationDbRevisionRecord>>asList(Keys.KEY_AGGREGATION_DB_REVISION_PRIMARY);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AggregationDbRevision as(String alias) {
		return new AggregationDbRevision(alias, this);
	}

	/**
	 * Rename this table
	 */
	public AggregationDbRevision rename(String name) {
		return new AggregationDbRevision(name, null);
	}
}
