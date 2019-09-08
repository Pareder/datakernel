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

package io.datakernel.codegen;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import static io.datakernel.common.Preconditions.checkNotNull;

final class ExpressionCmpNull implements PredicateDef {
	private final Expression value;

	ExpressionCmpNull(Expression value) {
		this.value = checkNotNull(value);
	}

	@Override
	public Type load(Context ctx) {
		GeneratorAdapter g = ctx.getGeneratorAdapter();

		Label labelNull = new Label();
		Label labelExit = new Label();

		value.load(ctx);
		g.ifNull(labelNull);
		g.push(false);
		g.goTo(labelExit);

		g.mark(labelNull);
		g.push(true);

		g.mark(labelExit);

		return Type.BOOLEAN_TYPE;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ExpressionCmpNull that = (ExpressionCmpNull) o;
		return value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
}
