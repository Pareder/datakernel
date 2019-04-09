package io.global.launchers;

import io.datakernel.config.ConfigConverter;
import io.global.common.*;

import static io.datakernel.config.ConfigConverters.of;

public class GlobalConfigConverters {
	public static ConfigConverter<NodeID> ofRawServerId() {
		return of(uri -> new NodeID(null, uri)); // TODO this is a stub
	}

	public static ConfigConverter<PubKey> ofPubKey() {
		return of(PubKey::fromString);
	}

	public static ConfigConverter<PrivKey> ofPrivKey() {
		return of(PrivKey::fromString);
	}

	public static ConfigConverter<SimKey> ofSimKey() {
		return of(SimKey::fromString);
	}

	public static ConfigConverter<Hash> ofHash() {
		return of(Hash::fromString);
	}
}
