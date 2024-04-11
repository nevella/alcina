package cc.alcina.framework.gwt.client.dirndl.layout;

public interface HasTag {
	/**
	 * 
	 * @return the tag of the rendered Model (if null, the computed default -
	 *         generally the owning property name - will be used)
	 */
	public String provideTag();
}
