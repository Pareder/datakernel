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

package io.datakernel.serial.net;

import io.datakernel.async.SettableStage;
import io.datakernel.async.Stage;
import io.datakernel.bytebuf.ByteBuf;
import io.datakernel.eventloop.AsyncTcpSocket;
import io.datakernel.serial.SerialConsumer;

final class SocketStreamConsumer implements SerialConsumer<ByteBuf> {
	private final AsyncTcpSocket asyncTcpSocket;
	private final SettableStage<Void> endOfStream = new SettableStage<>();

	private SettableStage<Void> write;
	private boolean writeEndOfStream;

	SocketStreamConsumer(AsyncTcpSocket asyncTcpSocket) {
		this.asyncTcpSocket = asyncTcpSocket;
	}

	public void onEndOfStream() {
		asyncTcpSocket.writeEndOfStream();
	}

	@Override
	public Stage<Void> accept(ByteBuf buf) {
		write = new SettableStage<>();
		write.thenRunEx(() -> write = null);
		if (buf != null) {
			asyncTcpSocket.write(buf);
		} else {
			writeEndOfStream = true;
			asyncTcpSocket.writeEndOfStream();
		}
		return write;
	}

	@Override
	public void closeWithError(Throwable e) {
		if (write != null) {
			write.setException(e);
		}
		endOfStream.trySetException(e);
	}

	public void onWrite() {
		assert write != null : "Received onWrite without writing anything";
		write.set(null);
		if (writeEndOfStream) {
			endOfStream.trySet(null);
		}
	}

	public Stage<Void> getEndOfStream() {
		return endOfStream;
	}

	public boolean isClosed() {
		return endOfStream.isComplete();
	}
}
