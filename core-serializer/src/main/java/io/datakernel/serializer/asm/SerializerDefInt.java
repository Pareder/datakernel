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

package io.datakernel.serializer.asm;

import io.datakernel.codegen.Expression;
import io.datakernel.codegen.Variable;
import io.datakernel.serializer.CompatibilityLevel;

import static io.datakernel.codegen.Expressions.cast;
import static io.datakernel.serializer.CompatibilityLevel.LEVEL_3_LE;
import static io.datakernel.serializer.asm.SerializerExpressions.*;

public final class SerializerDefInt extends SerializerDefPrimitive {
	private final boolean varLength;

	public SerializerDefInt() {
		super(int.class);
		this.varLength = false;
	}

	public SerializerDefInt(boolean varLength) {
		super(int.class);
		this.varLength = varLength;
	}

	@Override
	protected Expression doSerialize(Expression byteArray, Variable off, Expression value, CompatibilityLevel compatibilityLevel) {
		return varLength ?
				writeVarInt(byteArray, off, cast(value, int.class)) :
				writeInt(byteArray, off, cast(value, int.class), compatibilityLevel.compareTo(LEVEL_3_LE) < 0);
	}

	@Override
	protected Expression doDeserialize(Expression in, CompatibilityLevel compatibilityLevel) {
		return varLength ?
				readVarInt(in) :
				readInt(in, compatibilityLevel.compareTo(LEVEL_3_LE) < 0);
	}
}
