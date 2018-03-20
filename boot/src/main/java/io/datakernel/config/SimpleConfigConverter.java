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

package io.datakernel.config;

import io.datakernel.annotation.Nullable;

import static io.datakernel.util.Preconditions.checkNotNull;

public abstract class SimpleConfigConverter<T> implements ConfigConverter<T> {
	@Override
	@Nullable
	public final T get(Config config) {
		String string = config.getValue();
		checkNotNull(string);
		return fromString(string);
	}

	@Override
	@Nullable
	public final T get(Config config, @Nullable T defaultValue) {
		String defaultString = defaultValue == null ? "" : toString(defaultValue);
		String value = config.getValue(defaultString);
		return value == null || value.isEmpty() ? null : fromString(value);
	}

	protected abstract T fromString(@Nullable String string);

	protected abstract String toString(T value);
}
