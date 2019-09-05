package io.datakernel.di.util;

import io.datakernel.di.core.Name;
import io.datakernel.di.core.Scope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;

/**
 * This is a helper class for making tag-like objects that are identified by stateless or stateful annotations.
 * <p>
 * You don't need to extend it yourself.
 *
 * @see Name
 * @see Scope
 */
public abstract class AbstractAnnotation {
	@NotNull
	private final Class<? extends Annotation> annotationType;

	@Nullable
	private final Annotation annotation;

	protected AbstractAnnotation(@NotNull Class<? extends Annotation> annotationType, @Nullable Annotation annotation) {
		this.annotationType = annotationType;
		this.annotation = annotation == null || isMarker(annotationType) ? null : annotation;
	}

	public static boolean isMarker(Class<? extends Annotation> annotationType) {
		return annotationType.getDeclaredMethods().length == 0;
	}

	@NotNull
	public Class<? extends Annotation> getAnnotationType() {
		return annotationType;
	}

	@Nullable
	public Annotation getAnnotation() {
		return annotation;
	}

	public boolean isMarkedBy(Class<? extends Annotation> metaAnnotation) {
		return annotationType.isAnnotationPresent(metaAnnotation);
	}

	public String getDisplayString() {
		if (annotation == null) {
			return "@" + ReflectionUtils.getShortName(annotationType.getName()) + "()";
		}
		return ReflectionUtils.getShortName(annotation.toString());
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AbstractAnnotation other = (AbstractAnnotation) o;
		return annotationType == other.annotationType &&
				((this.annotation == other.annotation) ||
						(this.annotation != null && this.annotation.equals(other.annotation)));
	}

	@Override
	public int hashCode() {
		return 31 * annotationType.hashCode() +
				(annotation == null ? 0 : annotation.hashCode());
	}

	@Override
	public String toString() {
		return annotation != null ? annotation.toString() : "@" + annotationType.getName() + "()";
	}
}
