package io.global.chat.roomlist;

import io.datakernel.codec.StructuredCodec;
import io.global.ot.api.RepoID;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static io.datakernel.codec.StructuredCodecs.BOOLEAN_CODEC;
import static io.datakernel.codec.StructuredCodecs.object;
import static io.global.chat.Utils.REPO_ID_HEX_CODEC;

public class RoomListOperation {
	public static final RoomListOperation EMPTY = new RoomListOperation(null, true);
	public static final StructuredCodec<RoomListOperation> ROOM_LIST_CODEC = object(RoomListOperation::new,
			"room", RoomListOperation::getRoom, REPO_ID_HEX_CODEC,
			"remove", RoomListOperation::isRemove, BOOLEAN_CODEC);

	@Nullable
	private final RepoID room;
	private final boolean remove;

	private RoomListOperation(@Nullable RepoID room, boolean remove) {
		this.room = room;
		this.remove = remove;
	}

	public static RoomListOperation add(RepoID room) {
		return new RoomListOperation(room, false);
	}

	public static RoomListOperation remove(RepoID room) {
		return new RoomListOperation(room, true);
	}

	public void apply(Set<RepoID> rooms) {
		if (isEmpty()) {
			return;
		}
		if (remove) {
			rooms.remove(room);
		} else {
			rooms.add(room);
		}
	}

	public RoomListOperation invert() {
		return new RoomListOperation(room, !remove);
	}

	public boolean isEmpty() {
		return room == null;
	}

	public boolean isRemove() {
		return remove;
	}

	public RepoID getRoom() {
		assert room != null;
		return room;
	}

	public boolean isInversionFor(RoomListOperation other) {
		return room != null && other.room != null &&
				room.equals(other.room) &&
				remove != other.remove;
	}
}
