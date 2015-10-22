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

package io.datakernel.aggregation_db;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import io.datakernel.aggregation_db.api.QueryResultPlaceholder;
import io.datakernel.aggregation_db.api.ReportingDSLExpression;
import io.datakernel.aggregation_db.fieldtype.FieldType;
import io.datakernel.aggregation_db.keytype.KeyType;
import io.datakernel.codegen.AsmBuilder;
import io.datakernel.codegen.Expression;
import io.datakernel.codegen.ExpressionComparator;
import io.datakernel.codegen.ExpressionSequence;
import io.datakernel.codegen.utils.DefiningClassLoader;
import io.datakernel.serializer.BufferSerializer;
import io.datakernel.serializer.SerializerBuilder;
import io.datakernel.serializer.asm.SerializerGenClass;
import io.datakernel.stream.processor.StreamReducers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static io.datakernel.codegen.Expressions.*;

/**
 * Defines a structure of an aggregation.
 * It is defined by keys, fields and their types.
 * Contains methods for defining dynamic classes, that are used for different operations.
 * Provides serializer for records that have the defined structure.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class AggregationStructure {
	private final DefiningClassLoader classLoader;
	private final Map<String, KeyType> keys;
	private final Map<String, FieldType> fields;
	private final Map<String, FieldType> outputFields;
	private final AggregationKeyRelationships childParentRelationships;
	private final Map<String, ReportingDSLExpression> computedMeasures;

	private final static Logger logger = LoggerFactory.getLogger(AggregationStructure.class);

	/**
	 * Constructs a new aggregation structure with the given class loader, keys and fields.
	 *
	 * @param classLoader class loader for loading dynamically created classes
	 * @param keys        map of aggregation keys (key is key name)
	 * @param fields      map of aggregation fields (key is field name)
	 */
	public AggregationStructure(DefiningClassLoader classLoader, Map<String, KeyType> keys, Map<String, FieldType> fields) {
		this(classLoader, keys, fields, fields, ImmutableMap.<String, String>of(), new HashMap<String, ReportingDSLExpression>());
	}

	public AggregationStructure(DefiningClassLoader classLoader, Map<String, KeyType> keys, Map<String, FieldType> fields,
	                            Map<String, String> childParentRelationships) {
		this(classLoader, keys, fields, fields, childParentRelationships, new HashMap<String, ReportingDSLExpression>());
	}

	public AggregationStructure(DefiningClassLoader classLoader, Map<String, KeyType> keys, Map<String, FieldType> fields,
	                            Map<String, String> childParentRelationships,
	                            Map<String, ReportingDSLExpression> computedMeasures) {
		this(classLoader, keys, fields, fields, childParentRelationships, computedMeasures);
	}

	public AggregationStructure(DefiningClassLoader classLoader, Map<String, KeyType> keys,
	                            Map<String, FieldType> fields, Map<String, FieldType> outputFields,
	                            Map<String, String> childParentRelationships,
	                            Map<String, ReportingDSLExpression> computedMeasures) {
		this.classLoader = classLoader;
		this.keys = keys;
		this.fields = fields;
		this.outputFields = outputFields;
		this.childParentRelationships = new AggregationKeyRelationships(childParentRelationships);
		this.computedMeasures = computedMeasures;
	}

	public void checkThatKeysExist(List<String> keys) {
		for (String key : keys) {
			if (!this.keys.containsKey(key)) {
				throw new AggregationException("Key with the name '" + key + "' not found.");
			}
		}
	}

	public void checkThatFieldsExist(List<String> fields) {
		for (String field : fields) {
			if (!this.fields.containsKey(field)) {
				throw new AggregationException("Field with the name '" + field + "' not found.");
			}
		}
	}

	public Map<String, KeyType> getKeys() {
		return keys;
	}

	public KeyType getKeyType(String key) {
		return keys.get(key);
	}

	public Map<String, FieldType> getFields() {
		return fields;
	}

	public Map<String, FieldType> getOutputFields() {
		return outputFields;
	}

	public Map<String, ReportingDSLExpression> getComputedMeasures() {
		return computedMeasures;
	}

	public AggregationKeyRelationships getChildParentRelationships() {
		return childParentRelationships;
	}

	public Class<?> createKeyClass(List<String> keys) {
		logger.trace("Creating key class for keys {}", keys);
		AsmBuilder builder = new AsmBuilder(classLoader, Comparable.class);
		for (String key : keys) {
			KeyType d = this.keys.get(key);
			builder.field(key, d.getDataType());
		}
		builder.method("compareTo", compareTo(keys));
		builder.method("equals", asEquals(keys));
		builder.method("hashCode", hashCodeOfThis(keys));
		builder.method("toString", asString(keys));

		return builder.defineClass();
	}

	public Function createFieldFunction(Class<?> recordClass, Class<?> fieldClass, List<String> fields) {
		logger.trace("Creating field function for fields {}", fields);
		AsmBuilder builder = new AsmBuilder(classLoader, Function.class);
		Expression field = let(constructor(fieldClass));
		ExpressionSequence applyDef = sequence(field);

		for (String fieldString : fields) {
			applyDef.add(set(
					field(field, fieldString),
					field(cast(arg(0), recordClass), fieldString)));
		}

		applyDef.add(field);
		builder.method("apply", applyDef);

		return (Function) builder.newInstance();
	}

	public Class<?> createFieldClass(List<String> fields) {
		logger.trace("Creating field class for fields {}", fields);
		AsmBuilder builder = new AsmBuilder(classLoader, Comparable.class);

		for (String field : fields) {
			Class<?> dataType = null;
			FieldType fieldType = this.fields.get(field);

			if (fieldType != null) {
				dataType = fieldType.getDataType();
			} else {
				KeyType keyType = this.keys.get(field);
				if (keyType != null) {
					dataType = keyType.getDataType();
				}
			}

			builder.field(field, dataType);
		}

		builder.method("compareTo", compareTo(fields));
		builder.method("equals", asEquals(fields));
		builder.method("toString", asString(fields));
		builder.method("hashCode", hashCodeOfThis(fields));

		return builder.defineClass();
	}

	public Comparator createFieldComparator(AggregationQuery query, Class<?> fieldClass) {
		logger.trace("Creating field comparator for query {}", query.toString());
		AsmBuilder builder = new AsmBuilder(classLoader, Comparator.class);
		ExpressionComparator comparator = comparator();
		List<AggregationQuery.QueryOrdering> orderings = query.getOrderings();

		for (AggregationQuery.QueryOrdering ordering : orderings) {
			boolean isAsc = ordering.isAsc();
			String field = ordering.getPropertyName();
			if (isAsc)
				comparator.add(
						field(cast(arg(0), fieldClass), field),
						field(cast(arg(1), fieldClass), field));
			else
				comparator.add(
						field(cast(arg(1), fieldClass), field),
						field(cast(arg(0), fieldClass), field));
		}

		builder.method("compare", comparator);

		return (Comparator) builder.newInstance();
	}

	public Function createKeyFunction(Class<?> recordClass, Class<?> keyClass, List<String> keys) {
		logger.trace("Creating key function for keys {}", keys);
		AsmBuilder factory = new AsmBuilder<>(classLoader, Function.class);
		Expression key = let(constructor(keyClass));
		ExpressionSequence applyDef = sequence(key);
		for (String keyString : keys) {
			applyDef.add(set(
					field(key, keyString),
					field(cast(arg(0), recordClass), keyString)));
		}
		applyDef.add(key);
		factory.method("apply", applyDef);
		return (Function) factory.newInstance();
	}

	public Class<?> createRecordClass(List<String> keys, List<String> fields) {
		logger.trace("Creating record class for keys {}, fields {}", keys, fields);
		AsmBuilder<Object> builder = new AsmBuilder<>(classLoader, Object.class);
		for (String key : keys) {
			KeyType d = this.keys.get(key);
			builder.field(key, d.getDataType());
		}
		for (String field : fields) {
			FieldType m = this.outputFields.get(field);
			builder.field(field, m.getDataType());
		}
		builder.method("toString", asString(newArrayList(concat(keys, fields))));
		return builder.defineClass();
	}

	public Class<QueryResultPlaceholder> createResultClass(AggregationQuery query, List<String> computedMeasureNames) {
		logger.trace("Creating result class for query {} with computed measures {}", query.toString(), computedMeasureNames);
		AsmBuilder<QueryResultPlaceholder> builder = new AsmBuilder<>(classLoader, QueryResultPlaceholder.class);
		List<String> resultKeys = query.getResultKeys();
		List<String> resultFields = query.getResultFields();
		for (String key : resultKeys) {
			KeyType keyType = this.keys.get(key);
			builder.field(key, keyType.getDataType());
		}
		for (String field : resultFields) {
			FieldType fieldType = this.fields.get(field);
			builder.field(field, fieldType.getDataType());
		}
		ExpressionSequence computeSequence = sequence();
		for (String computedMeasure : computedMeasureNames) {
			builder.field(computedMeasure, double.class);
			computeSequence.add(set(field(self(), computedMeasure), computedMeasures.get(computedMeasure).getExpression()));
		}
		builder.method("compute", computeSequence);
		builder.method("toString", asString(newArrayList(concat(resultKeys, resultFields))));
		return builder.defineClass();
	}

	public Class<?> createResultClass(AggregationQuery query) {
		logger.trace("Creating result class for query {}", query.toString());
		AsmBuilder<Object> builder = new AsmBuilder<>(classLoader, Object.class);
		List<String> resultKeys = query.getResultKeys();
		List<String> resultFields = query.getResultFields();
		for (String key : resultKeys) {
			KeyType keyType = this.keys.get(key);
			if (keyType == null) {
				throw new AggregationException("Key with the name '" + key + "' not found.");
			}
			builder.field(key, keyType.getDataType());
		}
		for (String field : resultFields) {
			FieldType fieldType = this.fields.get(field);
			if (fieldType == null) {
				throw new AggregationException("Field with the name '" + field + "' not found.");
			}
			builder.field(field, fieldType.getDataType());
		}
		builder.method("toString", asString(newArrayList(concat(resultKeys, resultFields))));
		return builder.defineClass();
	}

	public StreamReducers.Reducer mergeFieldsReducer(Class<?> inputClass, Class<?> outputClass,
	                                                 List<String> keys, List<String> recordFields) {
		logger.trace("Creating merge fields reducer for keys {}, fields {}", keys, recordFields);
		AsmBuilder builder = new AsmBuilder(classLoader, StreamReducers.Reducer.class);

		Expression accumulator1 = let(constructor(outputClass));
		ExpressionSequence onFirstItemDef = sequence(accumulator1);
		for (String key : keys) {
			onFirstItemDef.add(set(
					field(accumulator1, key),
					field(cast(arg(2), inputClass), key)));
		}
		for (String field : recordFields) {
			onFirstItemDef.add(set(
					field(accumulator1, field),
					field(cast(arg(2), inputClass), field)));
		}
		onFirstItemDef.add(accumulator1);
		builder.method("onFirstItem", onFirstItemDef);

		Expression accumulator2 = let(cast(arg(3), outputClass));
		ExpressionSequence onNextItemDef = sequence(accumulator2);
		for (String field : recordFields) {
			onNextItemDef.add(set(
					field(accumulator2, field),
					add(
							field(cast(arg(2), inputClass), field),
							field(cast(arg(3), outputClass), field)
					)));
		}
		onNextItemDef.add(accumulator2);
		builder.method("onNextItem", onNextItemDef);

		builder.method("onComplete", call(arg(0), "onData", arg(2)));

		return (StreamReducers.Reducer) builder.newInstance();
	}

	public <T> BufferSerializer<T> createBufferSerializer(Class<T> recordClass, List<String> keys, List<String> fields) {
		SerializerGenClass serializerGenClass = new SerializerGenClass(recordClass);
		for (String key : keys) {
			KeyType keyType = this.keys.get(key);
			try {
				Field recordClassKey = recordClass.getField(key);
				serializerGenClass.addField(recordClassKey, keyType.serializerGen(), -1, -1);
			} catch (NoSuchFieldException e) {
				throw propagate(e);
			}
		}
		for (String field : fields) {
			FieldType fieldType = this.outputFields.get(field);
			try {
				Field recordClassField = recordClass.getField(field);
				serializerGenClass.addField(recordClassField, fieldType.serializerGen(), -1, -1);
			} catch (NoSuchFieldException e) {
				throw propagate(e);
			}
		}
		return SerializerBuilder.newDefaultInstance(classLoader).create(serializerGenClass);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("keys", keys)
				.add("fields", fields)
				.toString();
	}
}
