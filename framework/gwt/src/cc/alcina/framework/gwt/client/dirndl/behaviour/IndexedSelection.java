package cc.alcina.framework.gwt.client.dirndl.behaviour;

import java.util.List;

import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation;

public class IndexedSelection implements KeyboardNavigation.Navigation.Handler {
	private Host host;

	int indexSelected=-1;

	public int getIndexSelected() {
		return this.indexSelected;
	}


	public void setIndexSelected(int indexSelected) {
		this.indexSelected = indexSelected;
	}

	public Topic<Change> topicIndexChanged = Topic.create();

	public static class Change {
		public final int oldIndexSelected;

		public final int newIndexSelected;

		Change(int oldIndexSelected, int newIndexSelected) {
			this.oldIndexSelected = oldIndexSelected;
			this.newIndexSelected = newIndexSelected;
		}
	}

	public IndexedSelection(IndexedSelection.Host host) {
		this.host = host;
		indexSelected = host.getInitialSelectedIndex();
		wrapIndex();
	}
	

	@Override
	public void onNavigation(Navigation event) {
		int entryIndex = indexSelected;
		switch (event.getModel()) {
		case UP:
			indexSelected--;
			break;
		case DOWN:
			indexSelected++;
			break;
		}
		wrapIndex();
		if (entryIndex != indexSelected) {
			topicIndexChanged.publish(new Change(entryIndex, indexSelected));
		}
	}

	private void wrapIndex() {
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
	}

	public interface Host<T> {
		default int getInitialSelectedIndex() {
			return 0;
		}

		List<T> getItems();
	}
}
