/*
 * Copyright (C) 2015-2019 SoftIndex LLC.
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

package io.datakernel.async;

import io.datakernel.exception.UncheckedException;
import io.datakernel.functional.Try;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

abstract class AbstractPromise<T> implements Promise<T> {

	private static final Callback<Object> COMPLETED_PROMISE =
			(value, e) -> { throw new UnsupportedOperationException();};

	private static final Callback<Object> COMPLETED_EXCEPTIONALLY_PROMISE =
			(value, e) -> { throw new UnsupportedOperationException();};

	@Nullable
	protected Callback<? super T> next;

	@Override
	public boolean isComplete() {
		return next == COMPLETED_PROMISE || next == COMPLETED_EXCEPTIONALLY_PROMISE;
	}

	@Override
	public boolean isResult() {
		return next == COMPLETED_PROMISE;
	}

	@Override
	public boolean isException() {
		return next == COMPLETED_EXCEPTIONALLY_PROMISE;
	}

	@Async.Execute
	protected void complete(@Nullable T value, @Nullable Throwable e) {
		assert next != COMPLETED_PROMISE && next != COMPLETED_EXCEPTIONALLY_PROMISE;
		if (e == null) {
			complete(value);
		} else {
			completeExceptionally(e);
		}
	}

	@Async.Execute
	protected void complete(@Nullable T value) {
		assert next != COMPLETED_PROMISE && next != COMPLETED_EXCEPTIONALLY_PROMISE;
		if (next != null) {
			Callback<? super T> next = this.next;
			this.next = COMPLETED_PROMISE;
			next.accept(value, null);
		}
	}

	@Async.Execute
	protected void completeExceptionally(@Nullable Throwable e) {
		assert next != COMPLETED_PROMISE && next != COMPLETED_EXCEPTIONALLY_PROMISE;
		if (next != null) {
			Callback<? super T> next = this.next;
			this.next = COMPLETED_EXCEPTIONALLY_PROMISE;
			next.accept(null, e);
		}
	}

	protected void tryComplete(@Nullable T value, @Nullable Throwable e) {
		if (!isComplete()) {
			complete(value, e);
		}
	}

	protected void tryComplete(@Nullable T value) {
		if (!isComplete()) {
			complete(value);
		}
	}

	protected void tryCompleteExceptionally(@NotNull Throwable e) {
		if (!isComplete()) {
			completeExceptionally(e);
		}
	}

	@NotNull
	@Override
	public <U, P extends Callback<? super T> & Promise<U>> Promise<U> next(@Async.Schedule @NotNull P promise) {
		subscribe(promise);
		return promise;
	}

	protected void subscribe(@Async.Schedule @NotNull Callback<? super T> consumer) {
		if (next == null) {
			next = consumer;
		} else {
			assert !isComplete() : "Promise has already been completed";
			Callback<? super T> finalNext = next;
			next = (Callback<T>) (result, e) -> {
				finalNext.accept(result, e);
				consumer.accept(result, e);
			};
		}
	}

	@NotNull
	@Override
	public <U> Promise<U> map(@Async.Schedule @NotNull Function<? super T, ? extends U> fn) {
		return next(new NextPromise<T, U>() {
			@Override
			public void accept(T result, @Nullable Throwable e) {
				if (e == null) {
					U newResult;
					try {
						newResult = fn.apply(result);
					} catch (UncheckedException u) {
						completeExceptionally(u.getCause());
						return;
					}
					complete(newResult);
				} else {
					completeExceptionally(e);
				}
			}
		});
	}

	@NotNull
	@Override
	public <U> Promise<U> mapEx(@Async.Schedule @NotNull BiFunction<? super T, Throwable, ? extends U> fn) {
		return next(new NextPromise<T, U>() {
			@Override
			public void accept(T result, Throwable e) {
				if (e == null) {
					U newResult;
					try {
						newResult = fn.apply(result, null);
					} catch (UncheckedException u) {
						completeExceptionally(u.getCause());
						return;
					}
					complete(newResult);
				} else {
					U newResult;
					try {
						newResult = fn.apply(null, e);
					} catch (UncheckedException u) {
						completeExceptionally(u.getCause());
						return;
					}
					complete(newResult);
				}
			}
		});
	}

	@NotNull
	@Override
	public <U> Promise<U> then(@Async.Schedule @NotNull Function<? super T, ? extends Promise<? extends U>> fn) {
		return next(new NextPromise<T, U>() {
			@Override
			public void accept(T result, @Nullable Throwable e) {
				if (e == null) {
					Promise<? extends U> promise;
					try {
						promise = fn.apply(result);
					} catch (UncheckedException u) {
						completeExceptionally(u.getCause());
						return;
					}
					promise.whenComplete(this::complete);
				} else {
					completeExceptionally(e);
				}
			}
		});
	}

	@NotNull
	@Override
	public <U> Promise<U> thenEx(@Async.Schedule @NotNull BiFunction<? super T, Throwable, ? extends Promise<? extends U>> fn) {
		return next(new NextPromise<T, U>() {
			private void accept(@Async.Execute BiFunction<? super T, Throwable, ? extends Promise<? extends U>> fn, T result, Throwable e) {
				if (e == null) {
					Promise<? extends U> promise;
					try {
						promise = fn.apply(result, null);
					} catch (UncheckedException u) {
						completeExceptionally(u.getCause());
						return;
					}
					promise.whenComplete(this::complete);
				} else {
					Promise<? extends U> promise;
					try {
						promise = fn.apply(null, e);
					} catch (UncheckedException u) {
						completeExceptionally(u.getCause());
						return;
					}
					promise.whenComplete(this::complete);
				}
			}

			@Override
			public void accept(T result, @Nullable Throwable e) {
				accept(fn, result, e);
			}
		});
	}

	@NotNull
	@Override
	public Promise<T> whenComplete(@NotNull Callback<? super T> action) {
		subscribe(action);
		return this;
	}

	@NotNull
	@Override
	public Promise<T> whenComplete(@NotNull Runnable action) {
		return whenComplete((result, e) -> action.run());
	}

	@NotNull
	@Override
	public Promise<T> whenResult(@Async.Schedule @NotNull Consumer<? super T> action) {
		return whenComplete(new Callback<T>() {
			private void accept(@Async.Execute Consumer<? super T> action, T result) {
				action.accept(result);
			}

			@Override
			public void accept(T result, @Nullable Throwable e) {
				if (e == null) {
					accept(action, result);
				}
			}
		});
	}

	@Override
	public Promise<T> whenException(@Async.Schedule @NotNull Consumer<Throwable> action) {
		return whenComplete((result, e) -> {
			if (e != null) {
				action.accept(e);
			}
		});
	}

	private static final Object NO_RESULT = new Object();

	@SuppressWarnings("unchecked")
	private static class PromiseCombine<T, V, U> extends NextPromise<T, V> {
		final BiFunction<? super T, ? super U, ? extends V> fn;
		@Nullable
		T thisResult = (T) NO_RESULT;
		@Nullable
		U otherResult = (U) NO_RESULT;

		PromiseCombine(BiFunction<? super T, ? super U, ? extends V> fn) {
			this.fn = fn;
		}

		@Override
		public void accept(T result, @Nullable Throwable e) {
			if (e == null) {
				if (otherResult != NO_RESULT) {
					onBothResults(result, otherResult);
				} else {
					thisResult = result;
				}
			} else {
				onAnyException(e);
			}
		}

		protected void onOtherComplete(@Nullable U otherResult) {
			if (thisResult != NO_RESULT) {
				onBothResults(thisResult, otherResult);
			} else {
				this.otherResult = otherResult;
			}
		}

		void onBothResults(@Nullable T thisResult, @Nullable U otherResult) {
			this.thisResult = null;
			this.otherResult = null;
			if (!isComplete()) {
				complete(fn.apply(thisResult, otherResult));
			}
		}

		void onAnyException(@NotNull Throwable e) {
			tryCompleteExceptionally(e);
		}
	}

	@NotNull
	@SuppressWarnings("unchecked")
	@Override
	public <U, V> Promise<V> combine(@NotNull Promise<? extends U> other, @NotNull BiFunction<? super T, ? super U, ? extends V> fn) {
		if (other instanceof CompletePromise) {
			return map(result -> fn.apply(result, ((CompletePromise<U>) other).getResult()));
		}
		@NotNull PromiseCombine<T, V, U> resultPromise = new PromiseCombine<>(fn);
		other.whenComplete((result, e) -> {
			if (e == null) {
				resultPromise.onOtherComplete(result);
			} else {
				resultPromise.onAnyException(e);
			}
		});
		return next(resultPromise);
	}

	private static class PromiseBoth<T> extends NextPromise<T, Void> {
		int counter = 2;

		@Override
		public void accept(T result, @Nullable Throwable e) {
			if (e == null) {
				if (--counter == 0) {
					complete(null);
				}
			} else {
				tryCompleteExceptionally(e);
			}
		}
	}

	@NotNull
	@Override
	public Promise<Void> both(@NotNull Promise<?> other) {
		if (other instanceof CompletePromise) return toVoid();
		PromiseBoth<T> resultPromise = new PromiseBoth<>();
		other.whenComplete((result, e) -> {
			if (e == null) {
				if (--resultPromise.counter == 0) {
					resultPromise.complete(null);
				}
			} else {
				resultPromise.tryCompleteExceptionally(e);
			}
		});
		return next(resultPromise);
	}

	private static final class EitherPromise<T> extends NextPromise<T, T> {
		int errors;

		@Override
		public void accept(T result, @Nullable Throwable e) {
			if (e == null) {
				tryComplete(result);
			} else {
				if (++errors == 2) {
					completeExceptionally(e);
				}
			}
		}
	}

	@NotNull
	@Override
	public Promise<T> either(@NotNull Promise<? extends T> other) {
		if (other instanceof CompletePromise) {
			@SuppressWarnings("unchecked") CompletePromise<T> otherCompletePromise = (CompletePromise<T>) other;
			if (otherCompletePromise.isException()) return this;
			return otherCompletePromise;
		}
		EitherPromise<T> resultPromise = new EitherPromise<>();
		other.whenComplete((result, e) -> {
			if (e == null) {
				resultPromise.tryComplete(result);
			} else {
				if (++resultPromise.errors == 2) {
					resultPromise.completeExceptionally(e);
				}
			}
		});
		return next(resultPromise);
	}

	@NotNull
	@Override
	public Promise<Try<T>> toTry() {
		return next(new NextPromise<T, Try<T>>() {
			@Override
			public void accept(T result, @Nullable Throwable e) {
				if (e == null) {
					complete(Try.of(result));
				} else {
					complete(Try.ofException(e));
				}
			}
		});
	}

	@NotNull
	@Override
	public Promise<Void> toVoid() {
		return map($ -> null);
	}

	@NotNull
	@Override
	public CompletableFuture<T> toCompletableFuture() {
		CompletableFuture<T> future = new CompletableFuture<>();
		subscribe((result, e) -> {
			if (e == null) {
				future.complete(result);
			} else {
				future.completeExceptionally(e);
			}
		});
		return future;
	}
}
