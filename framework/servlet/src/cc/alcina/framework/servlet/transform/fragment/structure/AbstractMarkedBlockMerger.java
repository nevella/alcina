package cc.alcina.framework.servlet.transform.fragment.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.html.TextLayer;
import cc.alcina.framework.servlet.transform.fragment.html.TextLayer.IgnoreableWhitespace;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.IntPair;

public abstract class AbstractMarkedBlockMerger<I extends StyledMeasureSelection> {
	protected I firstInCurrentSequence = null;

	IntPair currentRun;

	MeasureSelection lastInCurrentSequence = null;

	I lastClbInCurrentSequence;

	Layer.State state;

	Class<I> blockClass;

	public AbstractMarkedBlockMerger(Class<I> blockClass, Layer.State state) {
		this.blockClass = blockClass;
		this.state = state;
	}

	protected <T extends Selection> List<T> selections(Class<T> clazz) {
		return state.traversalState.selections.get(clazz, false);
	}

	protected <T extends Selection> T selection(Class<T> clazz) {
		return selections(clazz).iterator().next();
	}

	public void select(Selection selection) {
		state.select(selection);
	}

	public void run() {
		List<MeasureSelection> selections = new ArrayList<>();
		selections(blockClass).forEach(selections::add);
		selections(IgnoreableWhitespace.class).forEach(selections::add);
		selections(TextLayer.Text.class).stream()
				.filter(s -> s.get().startContainingNode()
						.isWhitespaceOrEmptyTextContent())
				.forEach(selections::add);
		Collections.sort(selections);
		selections.forEach(sel -> {
			IntPair selectionRange = sel.get().toIntPair();
			if (sel.getClass() == blockClass) {
				I clb = (I) sel;
				if (currentRun != null
						&& currentRun.isContiguousWith(selectionRange)) {
					currentRun.expandToInclude(selectionRange);
					lastInCurrentSequence = clb;
					lastClbInCurrentSequence = clb;
				} else {
					flush();
					firstInCurrentSequence = clb;
					lastInCurrentSequence = clb;
					lastClbInCurrentSequence = clb;
					currentRun = selectionRange.copy();
				}
			} else {
				if (currentRun != null) {
					/*
					 * if not removing roots, the sequence will include text
					 * children of the marked blocks, so continue the run if the
					 * cursor is either contiguous with or contained in the run
					 */
					if (currentRun.contains(selectionRange)
							|| currentRun.isContiguousWith(selectionRange)) {
						lastInCurrentSequence = sel;
						currentRun.expandToInclude(selectionRange);
					} else {
						flush();
					}
				}
			}
		});
		flush();
	}

	void flush() {
		if (firstInCurrentSequence != null) {
			Location.Range range = new Location.Range(
					firstInCurrentSequence.get().start,
					lastClbInCurrentSequence.get().end);
			createMeasure(range);
			firstInCurrentSequence = null;
			lastInCurrentSequence = null;
			lastClbInCurrentSequence = null;
			currentRun = null;
		}
	}

	protected abstract void createMeasure(Location.Range range);
}