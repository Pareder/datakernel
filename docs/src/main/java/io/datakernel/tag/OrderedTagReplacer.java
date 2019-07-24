package io.datakernel.tag;

public final class OrderedTagReplacer implements TagReplacer {
	private final int order;
	private TagReplacer replacer;

	private OrderedTagReplacer(int order, TagReplacer replacer) {
		this.order = order;
		this.replacer = replacer;
	}

	public static OrderedTagReplacer of(int order, TagReplacer replacer) {
		return new OrderedTagReplacer(order, replacer);
	}

	@Override
	public void replace(StringBuilder text) throws ReplaceException {
		replacer.replace(text);
	}

	public int getOrder() {
		return order;
	}
}
