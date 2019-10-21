/*
 * Copyright (C) 2015-2018 SoftIndex LLC.
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
import io.datakernel.serializer.HasNullable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Function;

import static io.datakernel.codegen.Expressions.*;
import static io.datakernel.common.Preconditions.checkArgument;
import static io.datakernel.serializer.asm.SerializerExpressions.*;
import static io.datakernel.serializer.asm.SerializerGen.StaticDecoders.methodIn;
import static io.datakernel.serializer.asm.SerializerGen.StaticEncoders.*;
import static java.util.Collections.emptySet;

public abstract class AbstractSerializerGenCollection implements SerializerGen, HasNullable {
	protected final SerializerGen valueSerializer;
	protected final Class<?> collectionType;
	protected final Class<?> collectionImplType;
	protected final Class<?> elementType;
	protected final boolean nullable;

	protected AbstractSerializerGenCollection(@NotNull SerializerGen valueSerializer, @NotNull Class<?> collectionType, @NotNull Class<?> collectionImplType, @NotNull Class<?> elementType, boolean nullable) {
		this.valueSerializer = valueSerializer;
		this.collectionType = collectionType;
		this.collectionImplType = collectionImplType;
		this.elementType = elementType;
		this.nullable = nullable;
	}

	protected Expression collectionForEach(Expression collection, Class<?> valueType, Function<Expression, Expression> value) {
		return forEach(collection, valueType, value);
	}

	protected Expression createConstructor(Expression length) {
		return constructor(collectionImplType, !nullable ? length : dec(length));
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(valueSerializer);
	}

	@Override
	public Set<Integer> getVersions() {
		return emptySet();
	}

	@Override
	public Class<?> getRawType() {
		return collectionType;
	}

	@Override
	public final Expression serialize(DefiningClassLoader classLoader, StaticEncoders staticEncoders, Expression buf, Variable pos, Expression value, int version, CompatibilityLevel compatibilityLevel) {
		return staticEncoders.define(collectionType, buf, pos, value,
				serializeImpl(classLoader, staticEncoders, methodBuf(), methodPos(), methodValue(), version, compatibilityLevel));
	}

	private Expression serializeImpl(DefiningClassLoader classLoader, StaticEncoders staticEncoders, Expression buf, Variable pos, Expression value, int version, CompatibilityLevel compatibilityLevel) {
		Expression forEach = collectionForEach(value, valueSerializer.getRawType(),
				it -> valueSerializer.serialize(classLoader, staticEncoders, buf, pos, cast(it, valueSerializer.getRawType()), version, compatibilityLevel));

		if (!nullable) {
			return sequence(
					writeVarInt(buf, pos, call(value, "size")),
					forEach);
		} else {
			return ifThenElse(isNull(value),
					writeByte(buf, pos, value((byte) 0)),
					sequence(
							writeVarInt(buf, pos, inc(call(value, "size"))),
							forEach));
		}
	}

	@Override
	public final Expression deserialize(DefiningClassLoader classLoader, StaticDecoders staticDecoders, Expression in, Class<?> targetType, int version, CompatibilityLevel compatibilityLevel) {
		checkArgument(targetType.isAssignableFrom(collectionImplType), "Target(%s) should be assignable from collection implementation type(%s)", targetType, collectionImplType);
		return staticDecoders.define(collectionImplType, in,
				deserializeImpl(classLoader, staticDecoders, methodIn(), version, compatibilityLevel));
	}

	private Expression deserializeImpl(DefiningClassLoader classLoader, StaticDecoders staticDecoders, Expression in, int version, CompatibilityLevel compatibilityLevel) {
		return let(readVarInt(in), length ->
				!nullable ?
						let(createConstructor(length), instance -> sequence(
								loop(value(0), length,
										it -> sequence(
												call(instance, "add",
														cast(valueSerializer.deserialize(classLoader, staticDecoders, in, elementType, version, compatibilityLevel), elementType)),
												voidExp())),
								instance)) :
						ifThenElse(cmpEq(length, value(0)),
								nullRef(collectionImplType),
								let(createConstructor(length), instance -> sequence(
										loop(value(0), dec(length),
												it -> sequence(
														call(instance, "add",
																cast(valueSerializer.deserialize(classLoader, staticDecoders, in, elementType, version, compatibilityLevel), elementType)),
														voidExp())),
										instance))));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AbstractSerializerGenCollection)) return false;
		AbstractSerializerGenCollection that = (AbstractSerializerGenCollection) o;
		if (nullable != that.nullable) return false;
		if (!valueSerializer.equals(that.valueSerializer)) return false;
		if (!collectionType.equals(that.collectionType)) return false;
		if (!collectionImplType.equals(that.collectionImplType)) return false;
		return elementType.equals(that.elementType);
	}

	@Override
	public int hashCode() {
		int result = valueSerializer.hashCode();
		result = 31 * result + collectionType.hashCode();
		result = 31 * result + collectionImplType.hashCode();
		result = 31 * result + elementType.hashCode();
		result = 31 * result + (nullable ? 1 : 0);
		return result;
	}
}
