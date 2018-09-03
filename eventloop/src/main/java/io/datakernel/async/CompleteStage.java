package io.datakernel.async;

import io.datakernel.annotation.Nullable;
import io.datakernel.functional.Try;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.datakernel.eventloop.Eventloop.getCurrentEventloop;

public final class CompleteStage<T> implements MaterializedStage<T> {
	private final T result;
	private final Throwable exception = null;

	CompleteStage(T result) {
		this.result = result;
	}

	public static <T> CompleteStage<T> of(T result) {
		return new CompleteStage<>(result);
	}

	@Override
	public boolean isComplete() {
		return true;
	}

	@Override
	public boolean isResult() {
		return true;
	}

	@Override
	public boolean isException() {
		return false;
	}

	@Override
	public boolean hasResult() {
		return true;
	}

	@Override
	public boolean hasException() {
		return false;
	}

	@Override
	public T getResult() {
		return result;
	}

	@Override
	public Throwable getException() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Try<T> getTry() {
		return Try.of(result);
	}

	@Override
	public boolean setTo(BiConsumer<? super T, Throwable> consumer) {
		consumer.accept(result, exception);
		return true;
	}

	@Override
	public boolean setResultTo(Consumer<? super T> consumer) {
		consumer.accept(result);
		return true;
	}

	@Override
	public boolean setExceptionTo(Consumer<Throwable> consumer) {
		return false;
	}

	@SuppressWarnings("unchecked")
	public <U> CompleteStage<U> mold() {
		throw new AssertionError("Trying to mold a successful CompleteStage!");
	}

	@Override
	public <U, S extends BiConsumer<? super T, Throwable> & Stage<U>> Stage<U> then(S stage) {
		stage.accept(result, exception);
		return stage;
	}

	@Override
	public <U> Stage<U> thenApply(Function<? super T, ? extends U> fn) {
		return Stage.of(fn.apply(result));
	}

	@Override
	public <U> Stage<U> thenApplyEx(BiFunction<? super T, Throwable, ? extends U> fn) {
		return Stage.of(fn.apply(result, exception));
	}

	@Override
	public Stage<T> thenRun(Runnable action) {
		action.run();
		return this;
	}

	@Override
	public Stage<T> thenRunEx(Runnable action) {
		action.run();
		return this;
	}

	@Override
	public <U> Stage<U> thenCompose(Function<? super T, ? extends Stage<U>> fn) {
		return fn.apply(result);
	}

	@Override
	public <U> Stage<U> thenComposeEx(BiFunction<? super T, Throwable, ? extends Stage<U>> fn) {
		return fn.apply(result, exception);
	}

	@Override
	public Stage<T> whenComplete(BiConsumer<? super T, Throwable> action) {
		action.accept(result, exception);
		return this;
	}

	@Override
	public Stage<T> whenResult(Consumer<? super T> action) {
		action.accept(result);
		return this;
	}

	@Override
	public Stage<T> whenException(Consumer<Throwable> action) {
		return this;
	}

	@Override
	public Stage<T> thenException(Function<? super T, Throwable> fn) {
		return Stage.ofException(fn.apply(result));
	}

	@Override
	public <U> Stage<U> thenTry(ThrowingFunction<? super T, ? extends U> fn) {
		try {
			return Stage.of(fn.apply(result));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			return Stage.ofException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <U, V> Stage<V> combine(Stage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
		if (other instanceof CompleteStage) {
			return Stage.of(fn.apply(this.result, ((CompleteStage<U>) other).getResult()));
		}
		return other.then(new NextStage<U, V>() {
			@Override
			protected void onComplete(U result) {
				complete(fn.apply(CompleteStage.this.result, result));
			}
		});
	}

	@Override
	public Stage<Void> both(Stage<?> other) {
		if (other instanceof CompleteStage) {
			return Stage.complete();
		}
		return other.toVoid();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Stage<T> either(Stage<? extends T> other) {
		return this;
	}

	@Override
	public MaterializedStage<T> async() {
		SettableStage<T> result = new SettableStage<>();
		getCurrentEventloop().post(() -> result.set(this.result));
		return result;
	}

	@Override
	public Stage<Try<T>> toTry() {
		return Stage.of(Try.of(result));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Stage<Void> toVoid() {
		return Stage.complete();
	}

	@Override
	public Stage<T> timeout(@Nullable Duration timeout) {
		return this;
	}

	@Override
	public CompletableFuture<T> toCompletableFuture() {
		CompletableFuture<T> future = new CompletableFuture<>();
		future.complete(result);
		return future;
	}
}
