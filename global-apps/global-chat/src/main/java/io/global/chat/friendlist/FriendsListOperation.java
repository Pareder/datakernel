package io.global.chat.friendlist;

import io.datakernel.codec.StructuredCodec;
import io.datakernel.codec.StructuredCodecs;
import io.global.common.PubKey;

import static io.datakernel.codec.StructuredCodecs.object;
import static io.global.ot.util.BinaryDataFormats.REGISTRY;

public final class FriendsListOperation {
	public static final StructuredCodec<FriendsListOperation> FRIENDS_LIST_CODEC = object(FriendsListOperation::new,
			"friend", FriendsListOperation::getFriend, REGISTRY.get(PubKey.class),
			"remove", FriendsListOperation::isRemove, StructuredCodecs.BOOLEAN_CODEC);

	private final PubKey friend;
	private final boolean remove;

	private FriendsListOperation(PubKey friend, boolean remove) {
		this.friend = friend;
		this.remove = remove;
	}

	public static FriendsListOperation add(PubKey friend) {
		return new FriendsListOperation(friend, false);
	}

	public static FriendsListOperation remove(PubKey friend) {
		return new FriendsListOperation(friend, true);
	}

	public PubKey getFriend() {
		return friend;
	}

	public boolean isRemove() {
		return remove;
	}

	public FriendsListOperation invert() {
		return new FriendsListOperation(friend, !remove);
	}
}
