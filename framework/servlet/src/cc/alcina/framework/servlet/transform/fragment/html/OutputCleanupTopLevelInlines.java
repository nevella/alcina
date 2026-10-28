package cc.alcina.framework.servlet.transform.fragment.html;

import com.google.common.base.Preconditions;

import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;

/**
 * Remove redundant inlines (parent is Block.Simple, or unstyled)
 */
class OutputCleanupTopLevelInlines {
	static class CleanupStyled extends OutputCleanupSublayer<Inline.Styled> {
		@Override
		protected void process0(MeasureNode selection, Inline.Styled inline) {
			if (inline.parent() == inline.fragmentModel().getFragmentRoot()
					.getRootModel()) {
				Preconditions.checkArgument(inline.domNode().ntc().isEmpty());
				removePreservePagebreaks(selection, inline);
			}
		}
	}

	static class MergeContiguousText
			extends OutputCleanupSublayer<Inline.Text> {
		@Override
		protected void process0(MeasureNode selection, Inline.Text inline) {
			if (inline.parent() == inline.fragmentModel().getFragmentRoot()
					.getRootModel()) {
				Preconditions.checkArgument(inline.domNode().ntc().isEmpty());
				removePreservePagebreaks(selection, inline);
			}
		}
	}

	static class CleanupText extends OutputCleanupSublayer<Inline.Text> {
		@Override
		protected void process0(MeasureNode selection, Inline.Text inline) {
			if (inline.parent() == inline.fragmentModel().getFragmentRoot()
					.getRootModel()) {
				Preconditions.checkArgument(inline.domNode().ntc().isEmpty());
				removePreservePagebreaks(selection, inline);
			}
		}
	}
}