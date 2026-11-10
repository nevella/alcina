package cc.alcina.framework.common.client.util.traversal;

import java.util.Iterator;

public interface Traversable<T extends Traversable> {
	Iterator<T> children();

	void enter();

	void exit();

	void release();
}
