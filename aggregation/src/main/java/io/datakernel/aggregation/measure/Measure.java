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

package io.datakernel.aggregation.measure;

import io.datakernel.aggregation.fieldtype.FieldType;
import io.datakernel.codegen.Expression;
import io.datakernel.codegen.VarField;

/**
 * Represents some metric, often numerical with logic for aggregating values of this metric .
 */
public abstract class Measure {
	protected final FieldType fieldType;

	protected Measure(FieldType fieldType) {
		this.fieldType = fieldType;
	}

	public final FieldType getFieldType() {
		return fieldType;
	}

	public abstract Expression valueOfAccumulator(Expression accumulator);

	public abstract Expression zeroAccumulator(VarField accumulator);

	public abstract Expression initAccumulatorWithAccumulator(VarField accumulator, Expression firstAccumulator);

	public abstract Expression initAccumulatorWithValue(VarField accumulator, VarField firstValue);

	public abstract Expression reduce(VarField accumulator, VarField nextAccumulator);

	public abstract Expression accumulate(VarField accumulator, VarField nextValue);
}
