package cc.alcina.framework.common.client.traversal;

/**
 * Layer traversal begins with a RootLayer and a corresponding RootSelection,
 * which encapsulates the initial selection to be traversed - that selection
 * might be a document or a logical entity such as 'website x'
 *
 * 
 *
 */
public class RootSelection extends AbstractSelection<Void> {
	public RootSelection(Selection parentSelection) {
		super(parentSelection, null, "root");
	}
}