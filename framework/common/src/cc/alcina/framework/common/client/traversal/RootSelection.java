package cc.alcina.framework.common.client.traversal;

public class RootSelection extends AbstractSelection<Void> {
	public RootSelection(Selection parentselection) {
		super(parentselection, null, "root");
	}
}