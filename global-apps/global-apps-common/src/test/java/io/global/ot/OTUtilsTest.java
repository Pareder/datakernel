package io.global.ot;

import io.global.common.KeyPair;
import io.global.common.PubKey;
import io.global.ot.shared.SharedReposOperation.SharedRepo;
import org.junit.Test;

import java.util.Set;
import java.util.stream.IntStream;

import static io.global.ot.OTUtils.REPO_COMPARATOR;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;

public final class OTUtilsTest {
	@Test
	public void testRepoComparator() {
		doTestRepoComparator(0,
				new SharedRepo(emptySet(), false),
				new SharedRepo(emptySet(), false));
		// remove has priority
		doTestRepoComparator(1,
				new SharedRepo(emptySet(), false),
				new SharedRepo(emptySet(), true));

		// remove still has priority
		doTestRepoComparator(1,
				new SharedRepo(getPubKeys(10), false),
				new SharedRepo(emptySet(), true));

		// smaller repo has priority
		doTestRepoComparator(-1,
				new SharedRepo(getPubKeys(10), false),
				new SharedRepo(getPubKeys(9), false));

	}

	private Set<PubKey> getPubKeys(int size) {
		return IntStream.range(0, size).mapToObj($ -> KeyPair.generate().getPubKey()).collect(toSet());
	}

	private void doTestRepoComparator(int expected, SharedRepo left, SharedRepo right) {
		assertEquals(expected, REPO_COMPARATOR.compare(left, right));
	}
}
