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

import io.datakernel.codegen.DefiningClassLoader;
import io.datakernel.codegen.Expression;
import io.datakernel.codegen.Variable;
import io.datakernel.serializer.CompatibilityLevel;
import io.datakernel.serializer.util.BinaryOutputUtils;

import static io.datakernel.codegen.Expressions.*;

public final class SerializerGenFloat extends SerializerGenPrimitive {

	public SerializerGenFloat() {
		super(float.class);
	}

	@Override
	public Expression serialize(DefiningClassLoader classLoader, Expression byteArray, Variable off, Expression value, int version, CompatibilityLevel compatibilityLevel) {
		return callStatic(BinaryOutputUtils.class, "writeFloat", byteArray, off, cast(value, float.class));
	}

	@Override
	public Expression deserialize(DefiningClassLoader classLoader, Class<?> targetType, int version, CompatibilityLevel compatibilityLevel) {
		if (targetType.isPrimitive())
			return call(arg(0), "readFloat");
		else
			return cast(call(arg(0), "readFloat"), Float.class);
	}
}

