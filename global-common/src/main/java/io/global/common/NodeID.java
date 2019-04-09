package io.global.common;

import io.datakernel.exception.ParseException;

public final class NodeID {
	private final NodeType nodeType;
	private final String uri;

	public NodeID(NodeType nodeType, String uri) {
		this.nodeType = nodeType;
		this.uri = uri;
	}

	public static NodeID parse(NodeType nodeType, String uri) throws ParseException {
		return new NodeID(nodeType, uri); // TODO
	}

	public NodeType getNodeType() {
		return nodeType;
	}

	public String getUri() {
		return uri;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		NodeID that = (NodeID) o;

		return uri.equals(that.uri);
	}

	@Override
	public int hashCode() {
		return uri.hashCode();
	}

	@Override
	public String toString() {
		return "NodeID{" + uri + '}';
	}
}
