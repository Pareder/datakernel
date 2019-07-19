package datakernel.tag;

public final class OrderedTagReplacer implements TagReplacer {
	private final int order;
	private TagReplacer replacer;

	public OrderedTagReplacer(int order, TagReplacer replacer) {
		this.order = order;
		this.replacer = replacer;
	}

	@Override
	public void replace(StringBuilder text) throws ReplaceException {
		replacer.replace(text);
	}

	public int getOrder() {
		return order;
	}
}
