package io.global.chat.chatroom.roomname;

import io.datakernel.ot.OTSystem;
import io.datakernel.ot.OTSystemImpl;

import static io.datakernel.ot.TransformResult.*;
import static io.datakernel.util.Preconditions.checkState;
import static java.util.Collections.singletonList;

public final class RoomNameOTSystem {
	private RoomNameOTSystem() {
		throw new AssertionError();
	}

	public static OTSystem<SetRoomName> createOTSystem() {
		return OTSystemImpl.<SetRoomName>create()
				.withEmptyPredicate(SetRoomName.class, SetRoomName::isEmpty)
				.withInvertFunction(SetRoomName.class, op -> singletonList(new SetRoomName(op.getNext(), op.getPrev())))

				.withSquashFunction(SetRoomName.class, SetRoomName.class, (first, second) -> new SetRoomName(first.getPrev(), second.getNext()))
				.withTransformFunction(SetRoomName.class, SetRoomName.class, (left, right) -> {
					checkState(left.getPrev().equals(right.getPrev()), "Previous values of left and right operation should be equal");
					if (left.getNext().compareTo(right.getNext()) > 0)
						return left(new SetRoomName(left.getNext(), right.getNext()));
					if (left.getNext().compareTo(right.getNext()) < 0)
						return right(new SetRoomName(right.getNext(), left.getNext()));
					return empty();
				});
	}
}
