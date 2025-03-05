package cc.alcina.framework.common.client.traversal;

/**
 * A support class for traversals which transform dom documents (and thus have
 * specific input + output types)
 */
public class DocumentTransformationTraversal {
	SelectionTraversal traversal;

	public DocumentTransformationTraversal(SelectionTraversal traversal) {
		this.traversal = traversal;
	}

	/*
	 * Not used by traversal browser (which needs node-level access to the
	 * documents, not just markup)
	 */
	public String getDocumentMarkup(boolean input) {
		if (input) {
			Object rootValue = traversal.getRootSelection().get();
			return rootValue instanceof HasMarkup
					? ((HasMarkup) rootValue).provideMarkup()
					: null;
		} else {
			return traversal.outputContainer instanceof HasMarkup
					? ((HasMarkup) traversal.outputContainer).provideMarkup()
					: null;
		}
	}

	public String getDocumentCss(boolean input) {
		if (input) {
			Object rootValue = traversal.getRootSelection().get();
			return rootValue instanceof HasCss
					? ((HasCss) rootValue).provideCss()
					: null;
		} else {
			return traversal.outputContainer instanceof HasCss
					? ((HasCss) traversal.outputContainer).provideCss()
					: null;
		}
	}

	public interface HasMarkup {
		String provideMarkup();
	}

	public interface HasCss {
		String provideCss();

		String provideMarkupContainerClassNames();
	}

	public String getMarkupContainerClassnames(boolean input) {
		if (input) {
			Object rootValue = traversal.getRootSelection().get();
			return rootValue instanceof HasCss
					? ((HasCss) rootValue).provideMarkupContainerClassNames()
					: null;
		} else {
			return traversal.outputContainer instanceof HasCss
					? ((HasCss) traversal.outputContainer)
							.provideMarkupContainerClassNames()
					: null;
		}
	}
}