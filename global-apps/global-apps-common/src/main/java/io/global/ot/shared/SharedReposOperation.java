package io.global.ot.shared;

import io.global.common.PubKey;

import java.util.Map;
import java.util.Set;

import static io.datakernel.util.CollectionUtils.map;
import static java.util.stream.Collectors.toMap;

public class SharedReposOperation {
	private final Map<String, SharedRepo> sharedRepos;

	public SharedReposOperation(Map<String, SharedRepo> sharedRepos) {
		this.sharedRepos = sharedRepos;
	}

	public static SharedReposOperation of(String id, SharedRepo repo) {
		return new SharedReposOperation(map(id, repo));
	}

	public static SharedReposOperation create(String id, Set<PubKey> participants) {
		return of(id, new SharedRepo(participants, false));
	}

	public static SharedReposOperation delete(String id, Set<PubKey> participants) {
		return of(id, new SharedRepo(participants, true));
	}

	public SharedReposOperation invert() {
		return new SharedReposOperation(sharedRepos.entrySet().stream()
				.collect(toMap(Map.Entry::getKey, e -> e.getValue().invert())));
	}

	public boolean isEmpty() {
		return sharedRepos.isEmpty();
	}

	public Map<String, SharedRepo> getSharedRepos() {
		return sharedRepos;
	}

	public static class SharedRepo {
		private final Set<PubKey> participants;
		private final boolean remove;

		public SharedRepo(Set<PubKey> participants, boolean remove) {
			this.participants = participants;
			this.remove = remove;
		}

		public Set<PubKey> getParticipants() {
			return participants;
		}

		public boolean isRemove() {
			return remove;
		}

		public SharedRepo invert() {
			return new SharedRepo(participants, !remove);
		}

		public boolean isInversion(SharedRepo other) {
			return participants.equals(other.participants) && remove != other.remove;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			SharedRepo that = (SharedRepo) o;

			if (remove != that.remove) return false;
			if (!participants.equals(that.participants)) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = participants.hashCode();
			result = 31 * result + (remove ? 1 : 0);
			return result;
		}
	}
}
