package cc.alcina.framework.gwt.client.dirndl.behaviour;

import java.util.List;

import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation;

/**
 * Models 'indexed' (keyboard) navigation over a list of items. The
 * keyboard-selected item is 'indexselected'
 *
 * @author nick@alcina.cc
 *
 */
public class IndexedSelection implements KeyboardNavigation.Navigation.Handler {
	private Host host;

	int indexSelected = -1;

	public Topic<Change> topicIndexChanged = Topic.create();

	public IndexedSelection(IndexedSelection.Host host) {
		this.host = host;
		setIndexSelected(wrapIndex(host.getInitialSelectedIndex()));
	}

	public int getIndexSelected() {
		return this.indexSelected;
	}

	@Override
	public void onNavigation(Navigation event) {
		int newSelectedIndex = indexSelected;
		switch (event.getModel()) {
		case UP:
			newSelectedIndex--;
			break;
		case DOWN:
			newSelectedIndex++;
			break;
		case FIRST:
			newSelectedIndex = 0;
			break;
		}
		setIndexSelected(wrapIndex(newSelectedIndex));
	}

	public void setIndexSelected(int indexSelected) {
		int old_indexSelected = this.indexSelected;
		this.indexSelected = indexSelected;
		if (old_indexSelected != indexSelected) {
			topicIndexChanged
					.publish(new Change(old_indexSelected, indexSelected));
		}
	}

	private int wrapIndex(int indexSelected) {
		int size = host.getItems().size();
		if (size == 0) {
			indexSelected = -1;
		} else {
			if (indexSelected < 0) {
				indexSelected = size - 1;
			}
			if (indexSelected >= size) {
				indexSelected = 0;
			}
		}
		return indexSelected;
	}

	public static class Change {
		public final int oldIndexSelected;

		public final int newIndexSelected;

		Change(int oldIndexSelected, int newIndexSelected) {
			this.oldIndexSelected = oldIndexSelected;
			this.newIndexSelected = newIndexSelected;
		}
	}

	public interface Host<T> {
		default int getInitialSelectedIndex() {
			return 0;
		}

		List<T> getItems();
	}
}
