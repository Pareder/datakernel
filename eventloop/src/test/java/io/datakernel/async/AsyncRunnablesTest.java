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

package io.datakernel.async;

import io.datakernel.eventloop.Eventloop;
import org.junit.Test;

import static java.util.Arrays.asList;

public class AsyncRunnablesTest {
	@Test
	public void test() {
		Eventloop eventloop = Eventloop.create();

		AsyncRunnable runnable1 = new AsyncRunnable() {
			@Override
			public void run(CompletionCallback callback) {
				callback.setComplete();
			}
		};
		AsyncRunnable runnable2 = new AsyncRunnable() {
			@Override
			public void run(CompletionCallback callback) {
				callback.setComplete();
			}
		};

		AsyncRunnable timeoutCallable = AsyncRunnables.runInParallel(eventloop, asList(runnable1, runnable2));
		timeoutCallable.run(new AssertingCompletionCallback() {
			@Override
			protected void onComplete() {
			}
		});

		eventloop.run();
	}
}