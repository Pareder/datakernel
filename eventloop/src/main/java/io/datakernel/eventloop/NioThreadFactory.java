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

package io.datakernel.eventloop;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public final class NioThreadFactory {
	private static class NioThreadFactoryHolder {
		private static final ThreadFactory DEFAULT_NIO_THREAD_FACTORY = createNioThreadFactory(Thread.MAX_PRIORITY, true);
	}

	public static ThreadFactory defaultNioThreadFactory() {
		return NioThreadFactoryHolder.DEFAULT_NIO_THREAD_FACTORY;
	}

	public static ThreadFactory createNioThreadFactory(final int priority, final boolean daemon) {
		final AtomicLong count = new AtomicLong(0);
		return new ThreadFactory() {
			@Override
			public Thread newThread(Runnable runnable) {
				Thread thread = Executors.defaultThreadFactory().newThread(runnable);
				thread.setName(String.format("NioThread-%d", count.getAndIncrement()));
				thread.setDaemon(daemon);
				thread.setPriority(priority);
				return thread;
			}
		};
	}

	private NioThreadFactory() {
	}
}
