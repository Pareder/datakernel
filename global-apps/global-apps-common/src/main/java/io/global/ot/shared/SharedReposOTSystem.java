package io.global.ot.shared;

import io.datakernel.ot.OTSystem;
import io.datakernel.ot.OTSystemImpl;
import io.datakernel.ot.TransformResult;
import io.global.ot.shared.SharedReposOperation.SharedRepo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static io.datakernel.util.CollectionUtils.intersection;
import static io.datakernel.util.CollectorsEx.toMap;
import static io.global.ot.OTUtils.REPO_COMPARATOR;
import static java.util.Collections.singletonList;

public final class SharedReposOTSystem {
	private SharedReposOTSystem() {
		throw new AssertionError();
	}

	public static OTSystem<SharedReposOperation> createOTSystem() {
		return OTSystemImpl.<SharedReposOperation>create()
				.withTransformFunction(SharedReposOperation.class, SharedReposOperation.class, (left, right) -> {
					Map<String, SharedRepo> leftRepos = left.getSharedRepos();
					Map<String, SharedRepo> rightRepos = right.getSharedRepos();

					Set<String> intersection = intersection(leftRepos.keySet(), rightRepos.keySet());
					if (intersection.isEmpty()) return TransformResult.of(right, left);

					List<SharedReposOperation> leftTransformed = new ArrayList<>();
					List<SharedReposOperation> rightTransformed = new ArrayList<>();

					Map<String, SharedRepo> diffLeft = difference(rightRepos, intersection);
					Map<String, SharedRepo> diffRight = difference(leftRepos, intersection);
					if (!diffLeft.isEmpty()) leftTransformed.add(new SharedReposOperation(diffLeft));
					if (!diffRight.isEmpty()) rightTransformed.add(new SharedReposOperation(diffRight));

					for (String key : intersection) {
						SharedRepo leftRepo = leftRepos.get(key);
						SharedRepo rightRepo = rightRepos.get(key);

						int compare = REPO_COMPARATOR.compare(leftRepo, rightRepo);
						if (compare > 0) {
							rightTransformed.add(SharedReposOperation.of(key, rightRepo.invert()));
							rightTransformed.add(SharedReposOperation.of(key, leftRepo));
						} else if (compare < 0) {
							leftTransformed.add(SharedReposOperation.of(key, leftRepo.invert()));
							leftTransformed.add(SharedReposOperation.of(key, rightRepo));
						}
					}

					return TransformResult.of(leftTransformed, rightTransformed);
				})
				.withInvertFunction(SharedReposOperation.class, op -> singletonList(op.invert()))
				.withSquashFunction(SharedReposOperation.class, SharedReposOperation.class, (op1, op2) -> {
					if (op1.isEmpty()) return op2;
					if (op2.isEmpty()) return op1;

					Map<String, SharedRepo> repos1 = op1.getSharedRepos();
					Map<String, SharedRepo> repos2 = op2.getSharedRepos();
					Set<String> intersection = intersection(repos1.keySet(), repos2.keySet());

					Map<String, SharedRepo> result = Stream
							.concat(repos1.entrySet().stream(), repos2.entrySet().stream())
							.filter(sharedRepo -> !intersection.contains(sharedRepo.getKey()))
							.collect(toMap());

					for (String key : intersection) {
						SharedRepo intersected1 = repos1.get(key);
						SharedRepo intersected2 = repos2.get(key);
						if (!intersected1.isInversion(intersected2)) {
							result.put(key, intersected2);
						}
					}

					return new SharedReposOperation(result);
				})
				.withEmptyPredicate(SharedReposOperation.class, SharedReposOperation::isEmpty);
	}

	private static Map<String, SharedRepo> difference(Map<String, SharedRepo> repos, Set<String> intersection) {
		return repos.entrySet()
				.stream()
				.filter(entry -> !intersection.contains(entry.getKey()))
				.collect(toMap());
	}

}
