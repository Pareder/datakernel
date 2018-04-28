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

package io.datakernel.bytebuf;

import io.datakernel.util.ConcurrentStack;
import io.datakernel.util.MemSize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Integer.numberOfLeadingZeros;
import static java.lang.Math.max;

public final class ByteBufPool {
	private static final int NUMBER_SLABS = 33;

	private static final AtomicReference<ConcurrentStack.Node<ByteBuf>> cachedNodes = new AtomicReference<>();

	private static final ConcurrentStack<ByteBuf>[] slabs;
	private static final AtomicInteger[] created;

	private static final Object EMPTY_VALUE = new Object();

	static {
		//noinspection unchecked
		slabs = new ConcurrentStack[NUMBER_SLABS];
		//noinspection unchecked
		created = new AtomicInteger[NUMBER_SLABS];
		for (int i = 0; i < NUMBER_SLABS; i++) {
			slabs[i] = new ConcurrentStack<>(cachedNodes);
			created[i] = new AtomicInteger();
		}
	}

	private ByteBufPool() {
	}

	public static ByteBuf allocate(int size) {
		if (size == 0) return ByteBuf.empty();
		int index = 32 - numberOfLeadingZeros(size - 1); // index==32 for size==0
		ConcurrentStack<ByteBuf> stack = slabs[index];
		ByteBuf buf = stack.pop();
		if (buf != null) {
			buf.reset();
		} else {
			buf = ByteBuf.wrapForWriting(new byte[1 << index]);
			buf.refs++;
			assert (long) created[index].incrementAndGet() != Long.MAX_VALUE;
		}
		return buf;
	}

	public static ByteBuf allocate(MemSize size) {
		return allocate(size.toInt());
	}

	public static void recycle(ByteBuf buf) {
		if (buf.array.length == 0) return;
		ConcurrentStack<ByteBuf> stack = slabs[32 - numberOfLeadingZeros(buf.array.length - 1)];
		stack.push(buf);
	}

	public static ByteBuf recycleIfEmpty(ByteBuf buf) {
		if (buf.canRead())
			return buf;
		buf.recycle();
		return ByteBuf.empty();
	}

	static ConcurrentStack<ByteBuf> getSlab(int index) {
		return slabs[index];
	}

	public static void clear() {
		for (int i = 0; i < ByteBufPool.NUMBER_SLABS; i++) {
			slabs[i].clear();
			created[i].set(0);
		}
	}

	public static ByteBuf ensureWriteRemaining(ByteBuf buf, int newWriteRemaining) {
		return ensureWriteRemaining(buf, 0, newWriteRemaining);
	}

	public static ByteBuf ensureWriteRemaining(ByteBuf buf, int minSize, int newWriteRemaining) {
		if (newWriteRemaining == 0) return buf;
		if (buf.writeRemaining() < newWriteRemaining || buf instanceof ByteBuf.ByteBufSlice) {
			ByteBuf newBuf = allocate(max(minSize, newWriteRemaining + buf.readRemaining()));
			newBuf.put(buf);
			buf.recycle();
			return newBuf;
		} else {
			return buf;
		}
	}

	public static ByteBuf append(ByteBuf to, ByteBuf from) {
		assert !to.isRecycled() && !from.isRecycled();
		if (to.readRemaining() == 0) {
			to.recycle();
			return from;
		}
		to = ensureWriteRemaining(to, from.readRemaining());
		to.put(from);
		from.recycle();
		return to;
	}

	public static ByteBuf append(ByteBuf to, byte[] from, int offset, int length) {
		assert !to.isRecycled();
		to = ensureWriteRemaining(to, length);
		to.put(from, offset, length);
		return to;
	}

	public static ByteBuf append(ByteBuf to, byte[] from) {
		return append(to, from, 0, from.length);
	}

	private static final ByteBufPoolStats stats = new ByteBufPoolStats();

	public static ByteBufPoolStats getStats() {
		return stats;
	}

	public static int getCreatedItems() {
		int items = 0;
		for (AtomicInteger counter : created) {
			items += counter.get();
		}
		return items;
	}

	public static int getCreatedItems(int slab) {
		return created[slab].get();
	}

	public static int getPoolItems(int slab) {
		return slabs[slab].size();
	}

	public static int getPoolItems() {
		int result = 0;
		for (ConcurrentStack<ByteBuf> slab : slabs) {
			result += slab.size();
		}
		return result;
	}

	public static String getPoolItemsString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ByteBufPool.NUMBER_SLABS; ++i) {
			int createdItems = ByteBufPool.getCreatedItems(i);
			int poolItems = ByteBufPool.getPoolItems(i);
			if (createdItems != poolItems) {
				sb.append(String.format("Slab %d (%d) ", i, (1 << i)))
						.append(" created: ").append(createdItems)
						.append(" pool: ").append(poolItems).append("\n");
			}
		}
		return sb.toString();
	}

	private static long getPoolSize() {
		assert slabs.length == 33 : "Except slabs[32] that contains ByteBufs with size 0";
		long result = 0;
		for (int i = 0; i < slabs.length - 1; i++) {
			long slotSize = 1L << i;
			result += slotSize * slabs[i].size();
		}
		return result;
	}

	public interface ByteBufPoolStatsMXBean {
		int getCreatedItems();

		int getPoolItems();

		long getPoolSizeKB();

		List<String> getPoolSlabs();
	}

	public static final class ByteBufPoolStats implements ByteBufPoolStatsMXBean {
		@Override
		public int getCreatedItems() {
			return ByteBufPool.getCreatedItems();
		}

		@Override
		public int getPoolItems() {
			return ByteBufPool.getPoolItems();
		}

		@Override
		public long getPoolSizeKB() {
			return ByteBufPool.getPoolSize() / 1024;
		}

		@Override
		public List<String> getPoolSlabs() {
			assert slabs.length == 33 : "Except slabs[32] that contains ByteBufs with size 0";
			List<String> result = new ArrayList<>(slabs.length + 1);
			result.add("SlotSize,Created,InPool,Total(Kb)");
			for (int i = 0; i < slabs.length; i++) {
				long slotSize = 1L << i;
				int count = slabs[i].size();
				result.add((slotSize & 0xffffffffL) + "," + created[i] + "," + count + "," + slotSize * count / 1024);
			}
			return result;
		}
	}

	private static String formatHours(long period) {
		long milliseconds = period % 1000;
		long seconds = (period / 1000) % 60;
		long minutes = (period / (60 * 1000)) % 60;
		long hours = period / (60 * 60 * 1000);
		return String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds) + "." + String.format("%03d", milliseconds);
	}

	public static String formatDuration(long period) {
		if (period == 0)
			return "";
		return formatHours(period);
	}

	private static String extractContent(ByteBuf buf, int maxSize) {
		int to = buf.readPosition() + Math.min(maxSize, buf.readRemaining());
		return new String(Arrays.copyOfRange(buf.array(), buf.readPosition(), to));
	}
	//endregion
}
