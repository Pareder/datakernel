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

import io.datakernel.codegen.ClassBuilder;
import io.datakernel.codegen.Expression;
import io.datakernel.codegen.Variable;
import io.datakernel.serializer.CompatibilityLevel;
import io.datakernel.serializer.SerializerBuilder.StaticMethods;
import io.datakernel.serializer.asm.SerializerGenBuilder.SerializerForType;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static io.datakernel.codegen.Expressions.*;
import static io.datakernel.common.Preconditions.*;
import static java.lang.reflect.Modifier.*;
import static java.util.Arrays.asList;
import static org.objectweb.asm.Type.*;

public class SerializerGenClass implements SerializerGen {

	private static final class FieldGen {
		private Field field;
		private Method method;
		private int versionAdded = -1;
		private int versionDeleted = -1;
		private SerializerGen serializer;

		public boolean hasVersion(int version) {
			if (versionAdded == -1 && versionDeleted == -1) {
				return true;
			}
			if (versionAdded != -1 && versionDeleted == -1) {
				return version >= versionAdded;
			}
			if (versionAdded == -1) {
				return version < versionDeleted;
			}
			if (versionAdded > versionDeleted) {
				return version < versionDeleted || version >= versionAdded;
			}
			if (versionAdded < versionDeleted) {
				return version >= versionAdded && version < versionDeleted;
			}
			throw new IllegalStateException("Added and deleted versions are equal");
		}

		public Class<?> getRawType() {
			if (field != null)
				return field.getType();
			if (method != null)
				return method.getReturnType();
			throw new AssertionError();
		}

		public Type getAsmType() {
			return getType(getRawType());
		}
	}

	private boolean implInterface;
	private Class<?> dataTypeIn;
	private Class<?> dataTypeOut;
	private List<SerializerForType> generics;

	private final Map<String, FieldGen> fields = new LinkedHashMap<>();

	private Constructor<?> constructor;
	private List<String> constructorParams;
	private Method factory;
	private List<String> factoryParams;
	private final Map<Method, List<String>> setters = new LinkedHashMap<>();

	public SerializerGenClass(@NotNull Class<?> type) {
		this.dataTypeIn = type;
		if (!dataTypeIn.isInterface()) {
			this.dataTypeOut = dataTypeIn;
		}
	}

	public SerializerGenClass(@NotNull Class<?> type, @NotNull Class<?> typeImpl) {
		checkArgument(type.isInterface(), "Class should be an interface");
		checkArgument(type.isAssignableFrom(typeImpl), "Class should be assignable from %s", typeImpl);
		this.dataTypeIn = type;
		this.dataTypeOut = typeImpl;
		this.implInterface = true;
	}

	public SerializerGenClass(Class<?> type, SerializerForType[] generics) {
		this(type);
		this.generics = asList(generics);
	}

	public SerializerGenClass(Class<?> type, SerializerForType[] generics, Class<?> typeImpl) {
		this(type, typeImpl);
		this.generics = asList(generics);
	}

	public void addSetter(@NotNull Method method, @NotNull List<String> fields) {
		checkState(implInterface || !dataTypeIn.isInterface(), "Class should either implement an interface or be an interface");
		checkArgument(!isPrivate(method.getModifiers()), "Setter cannot be private: %s", method);
		checkArgument(method.getGenericParameterTypes().length == fields.size(), "Number of arguments of a method should match a size of list of fields");
		checkArgument(!setters.containsKey(method), "Setter has already been added");
		setters.put(method, fields);
	}

	public void setFactory(@NotNull Method methodFactory, @NotNull List<String> fields) {
		checkState(implInterface || !dataTypeIn.isInterface(), "Class should either implement an interface or be an interface");
		checkArgument(this.factory == null, "Factory is already set: %s", this.factory);
		checkArgument(!isPrivate(methodFactory.getModifiers()), "Factory cannot be private: %s", methodFactory);
		checkArgument(isStatic(methodFactory.getModifiers()), "Factory must be static: %s", methodFactory);
		checkArgument(methodFactory.getGenericParameterTypes().length == fields.size(), "Number of arguments of a method should match a size of list of fields");
		this.factory = methodFactory;
		this.factoryParams = fields;
	}

	public void setConstructor(@NotNull Constructor<?> constructor, @NotNull List<String> fields) {
		checkState(implInterface || !dataTypeIn.isInterface(), "Class should either implement an interface or be an interface");
		checkArgument(this.constructor == null, "Constructor is already set: %s", this.constructor);
		checkArgument(!isPrivate(constructor.getModifiers()), "Constructor cannot be private: %s", constructor);
		checkArgument(constructor.getGenericParameterTypes().length == fields.size(), "Number of arguments of a constructor should match a size of list of fields");
		this.constructor = constructor;
		this.constructorParams = fields;
	}

	public void addField(Field field, SerializerGen serializer, int added, int removed) {
		checkState(implInterface || !dataTypeIn.isInterface(), "Class should either implement an interface or be an interface");
		checkArgument(isPublic(field.getModifiers()), "Method should be public");
		String fieldName = field.getName();
		checkArgument(!fields.containsKey(fieldName), "Duplicate field '%s'", field);
		FieldGen fieldGen = new FieldGen();
		fieldGen.field = field;
		fieldGen.serializer = serializer;
		fieldGen.versionAdded = added;
		fieldGen.versionDeleted = removed;
		fields.put(fieldName, fieldGen);
	}

	public void addGetter(Method method, SerializerGen serializer, int added, int removed) {
		checkArgument(method.getGenericParameterTypes().length == 0, "Method should have 0 generic parameter types");
		checkArgument(isPublic(method.getModifiers()), "Method should be public");
		String fieldName = stripGet(method.getName(), method.getReturnType());
		checkArgument(!fields.containsKey(fieldName), "Duplicate field '%s'", method);
		FieldGen fieldGen = new FieldGen();
		fieldGen.method = method;
		fieldGen.serializer = serializer;
		fieldGen.versionAdded = added;
		fieldGen.versionDeleted = removed;
		fields.put(fieldName, fieldGen);
	}

	public void addMatchingSetters() {
		checkArgument(implInterface || !dataTypeIn.isInterface(), "Class should either implement an interface or be an interface");
		Set<String> usedFields = new HashSet<>();
		if (constructorParams != null) {
			usedFields.addAll(constructorParams);
		}
		if (factoryParams != null) {
			usedFields.addAll(factoryParams);
		}
		for (List<String> list : setters.values()) {
			usedFields.addAll(list);
		}
		for (String fieldName : fields.keySet()) {
			FieldGen fieldGen = fields.get(fieldName);
			Method getter = fieldGen.method;
			if (getter == null)
				continue;
			if (usedFields.contains(fieldName))
				continue;
			String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
			try {
				Method setter;
				if (implInterface)
					setter = dataTypeOut.getMethod(setterName, getter.getReturnType());
				else
					setter = dataTypeIn.getMethod(setterName, getter.getReturnType());
				if (!isPrivate(setter.getModifiers())) {
					addSetter(setter, asList(fieldName));
				}
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void getVersions(VersionsCollector versions) {
		for (FieldGen fieldGen : fields.values()) {
			if (fieldGen.versionAdded != -1)
				versions.add(fieldGen.versionAdded);
			if (fieldGen.versionDeleted != -1)
				versions.add(fieldGen.versionDeleted);
			versions.addRecursive(fieldGen.serializer);
		}
	}

	@Override
	public boolean isInline() {
		return false;
	}

	@Override
	public Class<?> getRawType() {
		return dataTypeIn;
	}

	private static String stripGet(String getterName, Class<?> type) {
		if (type == Boolean.TYPE || type == Boolean.class) {
			if (getterName.startsWith("is") && getterName.length() > 2) {
				return Character.toLowerCase(getterName.charAt(2)) + getterName.substring(3);
			}
		}
		if (getterName.startsWith("get") && getterName.length() > 3) {
			return Character.toLowerCase(getterName.charAt(3)) + getterName.substring(4);
		}
		return getterName;
	}

	@SuppressWarnings("RedundantIfStatement")
	@Override
	public boolean equals(Object o) {
		if (generics == null)
			return super.equals(o);

		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SerializerGenClass that = (SerializerGenClass) o;

		if (!dataTypeIn.equals(that.dataTypeIn)) return false;
		if (!generics.equals(that.generics)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		if (generics == null)
			return super.hashCode();

		int result = dataTypeIn.hashCode();
		result = 31 * result + generics.hashCode();
		return result;
	}

	@Override
	public void prepareSerializeStaticMethods(int version, StaticMethods staticMethods, CompatibilityLevel compatibilityLevel) {
		if (staticMethods.startSerializeStaticMethod(this, version)) {
			return;
		}

		List<Expression> list = new ArrayList<>();
		for (String fieldName : fields.keySet()) {

			FieldGen fieldGen = fields.get(fieldName);
			if (!fieldGen.hasVersion(version)) continue;

			Class<?> type = fieldGen.serializer.getRawType();
			if (!fieldGen.getRawType().equals(Object.class)) type = fieldGen.getRawType();

			if (fieldGen.field != null) {
				fieldGen.serializer.prepareSerializeStaticMethods(version, staticMethods, compatibilityLevel);
				list.add(set(arg(1), fieldGen.serializer.serialize(arg(0), arg(1), cast(property(arg(2), fieldName), type), version, staticMethods, compatibilityLevel)));
			} else if (fieldGen.method != null) {
				fieldGen.serializer.prepareSerializeStaticMethods(version, staticMethods, compatibilityLevel);
				list.add(set(arg(1), fieldGen.serializer.serialize(arg(0), arg(1), cast(call(arg(2), fieldGen.method.getName()), type), version, staticMethods, compatibilityLevel)));
			} else {
				throw new AssertionError();
			}
		}
		list.add(arg(1));

		staticMethods.registerStaticSerializeMethod(this, version, sequence(list));
	}

	@Override
	public Expression serialize(Expression byteArray, Variable off, Expression field, int version, StaticMethods staticMethods, CompatibilityLevel compatibilityLevel) {
		return staticMethods.callStaticSerializeMethod(this, version, byteArray, off, field);
	}

	@Override
	public void prepareDeserializeStaticMethods(int version, StaticMethods staticMethods, CompatibilityLevel compatibilityLevel) {
		if (staticMethods.startDeserializeStaticMethod(this, version)) {
			return;
		}

		if (!implInterface && dataTypeIn.isInterface()) {
			Expression expression = deserializeInterface(dataTypeIn, version, staticMethods, compatibilityLevel);
			staticMethods.registerStaticDeserializeMethod(this, version, expression);
			return;
		}
		if (!implInterface && constructor == null && factory == null && setters.isEmpty()) {
			Expression expression = deserializeClassSimple(version, staticMethods, compatibilityLevel);
			staticMethods.registerStaticDeserializeMethod(this, version, expression);
			return;
		}

		List<Expression> list = new ArrayList<>();
		Map<String, Expression> map = new HashMap<>();
		for (String fieldName : fields.keySet()) {
			FieldGen fieldGen = fields.get(fieldName);

			if (!fieldGen.hasVersion(version)) continue;

			fieldGen.serializer.prepareDeserializeStaticMethods(version, staticMethods, compatibilityLevel);
			Expression expression = var(fieldGen.serializer.deserialize(fieldGen.getRawType(), version, staticMethods, compatibilityLevel));
			list.add(expression);
			map.put(fieldName, cast(expression, fieldGen.getRawType()));
		}

		Expression constructor;
		if (factory == null) {
			constructor = callConstructor(dataTypeIn, map, version);
		} else {
			constructor = callFactory(map, version);
		}

		Expression local = var(constructor);
		list.add(local);

		for (Method method : setters.keySet()) {
			boolean found = false;
			for (String fieldName : setters.get(method)) {
				FieldGen fieldGen = fields.get(fieldName);
				checkNotNull(fieldGen, "Field '%s' is not found in '%s'", fieldName, method);
				if (fieldGen.hasVersion(version)) {
					found = true;
					break;
				}
			}
			if (found) {
				Expression[] temp = new Expression[method.getParameterTypes().length];
				int i = 0;
				for (String fieldName : setters.get(method)) {
					FieldGen fieldGen = fields.get(fieldName);
					assert fieldGen != null;
					if (fieldGen.hasVersion(version)) {
						temp[i++] = map.get(fieldName);
					} else {
						temp[i++] = pushDefaultValue(fieldGen.getAsmType());
					}
				}
				list.add(call(local, method.getName(), temp));
			}
		}

		for (String fieldName : fields.keySet()) {
			FieldGen fieldGen = fields.get(fieldName);
			if (!fieldGen.hasVersion(version))
				continue;
			if (fieldGen.field == null || isFinal(fieldGen.field.getModifiers()))
				continue;
			Variable property = property(local, fieldName);
			list.add(set(property, map.get(fieldName)));
		}

		list.add(local);
		staticMethods.registerStaticDeserializeMethod(this, version, sequence(list));
	}

	@Override
	public Expression deserialize(Class<?> targetType, int version, StaticMethods staticMethods, CompatibilityLevel compatibilityLevel) {
		return staticMethods.callStaticDeserializeMethod(this, version, arg(0));
	}

	private Expression callFactory(Map<String, Expression> map, int version) {
		Expression[] param = new Expression[factoryParams.size()];
		int i = 0;
		for (String fieldName : factoryParams) {
			FieldGen fieldGen = fields.get(fieldName);
			checkNotNull(fieldGen, "Field '%s' is not found in '%s'", fieldName, factory);
			if (fieldGen.hasVersion(version)) {
				param[i++] = map.get(fieldName);
			} else {
				param[i++] = pushDefaultValue(fieldGen.getAsmType());
			}
		}
		return callStatic(factory.getDeclaringClass(), factory.getName(), param);
	}

	private Expression callConstructor(Class<?> targetType, Map<String, Expression> map, int version) {
		Expression[] param;
		if (constructorParams == null) {
			param = new Expression[0];
			return constructor(targetType, param);
		}
		param = new Expression[constructorParams.size()];

		int i = 0;
		for (String fieldName : constructorParams) {
			FieldGen fieldGen = fields.get(fieldName);
			checkNotNull(fieldGen, "Field '%s' is not found in '%s'", fieldName, constructor);
			if (fieldGen.hasVersion(version)) {
				param[i++] = map.get(fieldName);
			} else {
				param[i++] = pushDefaultValue(fieldGen.getAsmType());
			}

		}
		return constructor(targetType, param);
	}

	@SuppressWarnings("unchecked")
	private Expression deserializeInterface(Class<?> targetType,
			int version,
			StaticMethods staticMethods,
			CompatibilityLevel compatibilityLevel) {
		ClassBuilder<?> asmFactory = ClassBuilder.create(staticMethods.getDefiningClassLoader(), (Class<Object>) targetType);
		for (String fieldName : fields.keySet()) {
			FieldGen fieldGen = fields.get(fieldName);

			Method method = checkNotNull(fieldGen.method);

			asmFactory = asmFactory
					.withField(fieldName, method.getReturnType())
					.withMethod(method.getName(), property(self(), fieldName));
		}

		Class<?> newClass = asmFactory.build();

		return let(
				constructor(newClass),
				instance -> sequence(expressions -> {
					for (String fieldName : fields.keySet()) {
						FieldGen fieldGen = fields.get(fieldName);
						if (!fieldGen.hasVersion(version))
							continue;
						Variable property = property(instance, fieldName);

						fieldGen.serializer.prepareDeserializeStaticMethods(version, staticMethods, compatibilityLevel);
						Expression expression =
								fieldGen.serializer.deserialize(fieldGen.getRawType(), version, staticMethods, compatibilityLevel);
						expressions.add(set(property, expression));
					}
					expressions.add(instance);
				}));
	}

	private Expression deserializeClassSimple(int version,
			StaticMethods staticMethods,
			CompatibilityLevel compatibilityLevel) {
		return let(
				constructor(dataTypeIn),
				instance ->
						sequence(expressions -> {
							for (String fieldName : fields.keySet()) {
								FieldGen fieldGen = fields.get(fieldName);

								if (!fieldGen.hasVersion(version)) continue;

								Variable property = property(instance, fieldName);
								fieldGen.serializer.prepareDeserializeStaticMethods(version, staticMethods, compatibilityLevel);
								expressions.add(set(property, fieldGen.serializer.deserialize(fieldGen.getRawType(), version, staticMethods, compatibilityLevel)));
							}
							expressions.add(instance);
						}));
	}

	private Expression pushDefaultValue(Type type) {
		switch (type.getSort()) {
			case BOOLEAN:
				return value(false);
			case CHAR:
				return value((char) 0);
			case BYTE:
				return value((byte) 0);
			case SHORT:
				return value((short) 0);
			case INT:
				return value(0);
			case Type.LONG:
				return value(0L);
			case Type.FLOAT:
				return value(0f);
			case Type.DOUBLE:
				return value(0d);
			case ARRAY:
			case OBJECT:
				return nullRef(type);
			default:
				throw new IllegalArgumentException("Unsupported type " + type);
		}
	}
}
