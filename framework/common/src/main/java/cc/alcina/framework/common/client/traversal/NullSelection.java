package cc.alcina.framework.common.client.traversal;

/**
 * Use during development while developing the layer structure
 *
 * 
 *
 */
public class NullSelection extends AbstractSelection<String> {
	public NullSelection(Selection parent) {
		super(parent, "null");
	}
}