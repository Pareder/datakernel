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

package io.datakernel.codegen;

import io.datakernel.codegen.utils.Primitives;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.HashMap;
import java.util.Map;

import static io.datakernel.util.Preconditions.check;
import static io.datakernel.util.Preconditions.checkNotNull;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.String.format;
import static org.objectweb.asm.Type.CHAR_TYPE;
import static org.objectweb.asm.Type.getType;

@SuppressWarnings("WeakerAccess")
public final class Utils {
	private static final Map<String, Type> WRAPPER_TO_PRIMITIVE = new HashMap<>();

	static {
		for (Class<?> primitiveType : Primitives.allPrimitiveTypes()) {
			Class<?> wrappedType = Primitives.wrap(primitiveType);
			WRAPPER_TO_PRIMITIVE.put(wrappedType.getName(), getType(primitiveType));
		}
	}

	private static final Method BOOLEAN_VALUE = Method.getMethod("boolean booleanValue()");
	private static final Method CHAR_VALUE = Method.getMethod("char charValue()");
	private static final Method INT_VALUE = Method.getMethod("int intValue()");
	private static final Method FLOAT_VALUE = Method.getMethod("float floatValue()");
	private static final Method LONG_VALUE = Method.getMethod("long longValue()");
	private static final Method DOUBLE_VALUE = Method.getMethod("double doubleValue()");
	private static final Method SHORT_VALUE = Method.getMethod("short shortValue()");
	private static final Method BYTE_VALUE = Method.getMethod("byte byteValue()");

	private static final Type BOOLEAN_TYPE = getType(Boolean.class);
	private static final Type CHARACTER_TYPE = getType(Character.class);
	private static final Type BYTE_TYPE = getType(Byte.class);
	private static final Type SHORT_TYPE = getType(Short.class);
	private static final Type INT_TYPE = getType(Integer.class);
	private static final Type FLOAT_TYPE = getType(Float.class);
	private static final Type LONG_TYPE = getType(Long.class);
	private static final Type DOUBLE_TYPE = getType(Double.class);
	private static final Type VOID_TYPE = getType(Void.class);

	public static boolean isPrimitiveType(Type type) {
		int sort = type.getSort();
		return sort == Type.BOOLEAN ||
				sort == Type.CHAR ||
				sort == Type.BYTE ||
				sort == Type.SHORT ||
				sort == Type.INT ||
				sort == Type.FLOAT ||
				sort == Type.LONG ||
				sort == Type.DOUBLE ||
				sort == Type.VOID;
	}

	public static boolean isWrapperType(Type type) {
		return type.getSort() == Type.OBJECT &&
				WRAPPER_TO_PRIMITIVE.containsKey(type.getClassName());
	}

	public static Method primitiveValueMethod(Type type) {
		if (type.equals(getType(char.class)) || type.equals(CHAR_TYPE))
			return CHAR_VALUE;
		if (type.equals(getType(boolean.class)) || type.equals(BOOLEAN_TYPE))
			return BOOLEAN_VALUE;
		if (type.equals(getType(double.class)) || type.equals(DOUBLE_TYPE))
			return DOUBLE_VALUE;
		if (type.equals(getType(float.class)) || type.equals(FLOAT_TYPE))
			return FLOAT_VALUE;
		if (type.equals(getType(long.class)) || type.equals(LONG_TYPE))
			return LONG_VALUE;
		if (type.equals(getType(int.class)) || type.equals(INT_TYPE))
			return INT_VALUE;
		if (type.equals(getType(short.class)) || type.equals(SHORT_TYPE))
			return SHORT_VALUE;
		if (type.equals(getType(byte.class)) || type.equals(BYTE_TYPE))
			return BYTE_VALUE;

		throw new IllegalArgumentException(format("No primitive value method for %s ", type.getClassName()));
	}

	public static Type wrap(Type type) {
		int sort = type.getSort();
		if (sort == Type.BOOLEAN)
			return BOOLEAN_TYPE;
		if (sort == Type.CHAR)
			return CHARACTER_TYPE;
		if (sort == Type.BYTE)
			return BYTE_TYPE;
		if (sort == Type.SHORT)
			return SHORT_TYPE;
		if (sort == Type.INT)
			return INT_TYPE;
		if (sort == Type.FLOAT)
			return FLOAT_TYPE;
		if (sort == Type.LONG)
			return LONG_TYPE;
		if (sort == Type.DOUBLE)
			return DOUBLE_TYPE;
		if (sort == Type.VOID)
			return VOID_TYPE;

		throw new IllegalArgumentException(format("%s is not primitive", type.getClassName()));
	}

	public static Type unwrap(Type type) {
		check(type.getSort() == Type.OBJECT, "Cannot unwrap type that is not an object reference");
		Type result = WRAPPER_TO_PRIMITIVE.get(type.getClassName());
		return checkNotNull(result);
	}

	public static Class<?> getJavaType(ClassLoader classLoader, Type type) {
		int sort = type.getSort();
		if (sort == Type.BOOLEAN)
			return boolean.class;
		if (sort == Type.CHAR)
			return char.class;
		if (sort == Type.BYTE)
			return byte.class;
		if (sort == Type.SHORT)
			return short.class;
		if (sort == Type.INT)
			return int.class;
		if (sort == Type.FLOAT)
			return float.class;
		if (sort == Type.LONG)
			return long.class;
		if (sort == Type.DOUBLE)
			return double.class;
		if (sort == Type.VOID)
			return void.class;
		if (sort == Type.OBJECT) {
			try {
				return classLoader.loadClass(type.getClassName());
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(format("No class %s in class loader", type.getClassName()), e);
			}
		}
		if (sort == Type.ARRAY) {
			Class<?> result;
			if (type.equals(getType(Object[].class))) {
				result = Object[].class;
			} else {
				String className = type.getDescriptor().replace('/', '.');
				try {
					result = Class.forName(className);
				} catch (ClassNotFoundException e) {
					throw new IllegalArgumentException(format("No class %s in Class.forName", className), e);
				}
			}
			return result;
		}
		throw new IllegalArgumentException(format("No Java type for %s", type.getClassName()));
	}

	public static Class<?> getJavaType(Type type) {
		return getJavaType(getSystemClassLoader(), type);
	}

	public static Class<?> tryGetJavaType(Type type) {
		return tryGetJavaType(getSystemClassLoader(), type);
	}

	public static Class<?> tryGetJavaType(ClassLoader classLoader, Type type) {
		int sort = type.getSort();
		if (sort == Type.BOOLEAN)
			return boolean.class;
		if (sort == Type.CHAR)
			return char.class;
		if (sort == Type.BYTE)
			return byte.class;
		if (sort == Type.SHORT)
			return short.class;
		if (sort == Type.INT)
			return int.class;
		if (sort == Type.FLOAT)
			return float.class;
		if (sort == Type.LONG)
			return long.class;
		if (sort == Type.DOUBLE)
			return double.class;
		if (sort == Type.VOID)
			return void.class;
		if (sort == Type.OBJECT) {
			try {
				return classLoader.loadClass(type.getClassName());
			} catch (ClassNotFoundException e) {
				return Object.class;
			}
		}
		if (sort == Type.ARRAY) {
			Class<?> result;
			if (type.equals(getType(Object[].class))) {
				result = Object[].class;
			} else {
				String className = type.getDescriptor().replace('/', '.');
				try {
					result = Class.forName(className);
				} catch (ClassNotFoundException e) {
					throw new IllegalArgumentException(format("No class %s in Class.forName", className), e);
				}
			}
			return result;
		}
		return Object.class;
	}

	public static void invokeVirtualOrInterface(GeneratorAdapter g, Class<?> owner, Method method) {
		if (owner.isInterface())
			g.invokeInterface(getType(owner), method);
		else
			g.invokeVirtual(getType(owner), method);
	}

	public static String exceptionInGeneratedClass(Context ctx) {
		return format("Thrown in generated class %s in method %s",
				ctx.getSelfType().getClassName(),
				ctx.getMethod()
		);
	}

	public static boolean isValidCast(Type from, Type to) {
		return from.getSort() != to.getSort()
				&&
				!(from.getSort() < Type.BOOLEAN
						|| from.getSort() > Type.DOUBLE
						|| to.getSort() < Type.BOOLEAN
						|| to.getSort() > Type.DOUBLE);
	}
}
