package cc.alcina.framework.common.client.logic.domaintransform;

public class DomainTransformRequestTagProvider {
	protected DomainTransformRequestTagProvider() {
	}

	private String tag;

	private static DomainTransformRequestTagProvider domainTransformRequestTagProvider;

	public static void registerDomainTransformRequestTagProvider(
			DomainTransformRequestTagProvider domainTransformRequestTagProvider) {
		DomainTransformRequestTagProvider.domainTransformRequestTagProvider = domainTransformRequestTagProvider;
	}

	public static DomainTransformRequestTagProvider get() {
		if (domainTransformRequestTagProvider == null) {
			domainTransformRequestTagProvider = new DomainTransformRequestTagProvider();
		}
		return domainTransformRequestTagProvider;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getTag() {
		return tag;
	}

	public void clearPerInstanceState() {
		setTag(null);
	}
}
