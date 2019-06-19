package io.global.ot;

import io.datakernel.async.RetryPolicy;
import io.datakernel.codec.StructuredCodec;
import io.datakernel.codec.StructuredCodecs;
import io.datakernel.util.Tuple3;
import io.global.common.PubKey;
import io.global.ot.dictionary.DictionaryOperation;
import io.global.ot.dictionary.SetOperation;
import io.global.ot.name.ChangeName;
import io.global.ot.service.messaging.CreateSharedRepo;
import io.global.ot.shared.SharedReposOperation;
import io.global.ot.shared.SharedReposOperation.SharedRepo;

import java.util.Comparator;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import static io.datakernel.codec.StructuredCodecs.*;
import static io.global.Utils.PUB_KEY_HEX_CODEC;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public final class OTUtils {
	private OTUtils() {
		throw new AssertionError();
	}

	public static final RetryPolicy POLL_RETRY_POLICY = RetryPolicy.exponentialBackoff(1000, 1000 * 60);

	public static final Comparator<SharedRepo> REPO_COMPARATOR =
			comparingInt((ToIntFunction<SharedRepo>) sharedRepo -> sharedRepo.isRemove() ? 1 : 0)
					.thenComparingInt(r -> r.getParticipants().size())
					.thenComparing(repo -> repo.getParticipants().stream()
							.map(PubKey::asString)
							.sorted()
							.collect(Collectors.joining()))
					.reversed();

	public static final StructuredCodec<ChangeName> CHANGE_NAME_CODEC = object(ChangeName::new,
			"prev", ChangeName::getPrev, STRING_CODEC,
			"next", ChangeName::getNext, STRING_CODEC,
			"timestamp", ChangeName::getTimestamp, LONG_CODEC);

	@SuppressWarnings("RedundantTypeArguments") // cannot infer type arguments
	public static final StructuredCodec<SharedReposOperation> SHARED_REPO_OPERATION_CODEC = ofList(
			StructuredCodecs.<Tuple3<String, Set<PubKey>, Boolean>, String, Set<PubKey>, Boolean>object(Tuple3::new,
					"id", Tuple3::getValue1, STRING_CODEC,
					"participants", Tuple3::getValue2, ofSet(PUB_KEY_HEX_CODEC),
					"remove", Tuple3::getValue3, BOOLEAN_CODEC))
			.transform(
					list -> new SharedReposOperation(list.stream()
							.collect(toMap(
									Tuple3::getValue1,
									tuple -> new SharedRepo(tuple.getValue2(), tuple.getValue3())))),
					op -> op.getSharedRepos().entrySet().stream()
							.map(e -> new Tuple3<>(e.getKey(), e.getValue().getParticipants(), e.getValue().isRemove()))
							.collect(toList()));

	public static final StructuredCodec<CreateSharedRepo> SHARED_REPO_MESSAGE_CODEC = StructuredCodecs.object(CreateSharedRepo::new,
			"id", CreateSharedRepo::getId, STRING_CODEC,
			"participants", CreateSharedRepo::getParticipants, ofSet(PUB_KEY_HEX_CODEC));

	public static final StructuredCodec<SetOperation> SET_OPERATION_CODEC = object(SetOperation::set,
			"prev", SetOperation::getPrev, STRING_CODEC.nullable(),
			"next", SetOperation::getNext, STRING_CODEC.nullable()
	);

	public static final StructuredCodec<DictionaryOperation> DICTIONARY_OPERATION_CODEC = ofMap(STRING_CODEC, SET_OPERATION_CODEC)
			.transform(DictionaryOperation::of, DictionaryOperation::getOperations);

}
