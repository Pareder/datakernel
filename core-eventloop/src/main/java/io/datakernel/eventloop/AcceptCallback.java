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

package io.datakernel.eventloop;

import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.NotNull;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * This is a simple callback which is used for accepting connections
 * when using {@link Eventloop} at the lowest level - creating a
 * {@link ServerSocketChannel} with {@link Eventloop#listen}.
 */
@FunctionalInterface
public interface AcceptCallback {
	/**
	 * Called from the eventloop thread when {@link ServerSocketChannel} accepts a new connection.
	 *
	 * @param socketChannel accepted connection.
	 */
	@Async.Execute
	void onAccept(@NotNull SocketChannel socketChannel);
}
