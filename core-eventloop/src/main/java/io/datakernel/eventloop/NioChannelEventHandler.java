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

import org.jetbrains.annotations.Async;

/**
 * An interface for channel attachments used in {@link Eventloop eventloop}.
 * It is a callback which executes code asynchronously (in eventloop context) when a read or a write is ready.
 */
public interface NioChannelEventHandler {
	/**
	 * Callback which is called when NIO channel is ready for reading.
	 */
	@Async.Execute
	void onReadReady();

	/**
	 * Callback which is called when NIO channel is ready for writing.
	 */
	@Async.Execute
	void onWriteReady();
}
