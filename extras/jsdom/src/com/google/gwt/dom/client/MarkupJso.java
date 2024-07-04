package com.google.gwt.dom.client;

import java.util.List;

/**
 * <h2>LocalDom 3.0</h2>
 * <p>
 * This class is responsible for writing markup chunks to the jso (browser) dom,
 * and ensuring the resultant remote dom *exactly* matches the browser dom. It
 * also marks each remote dom node with the ref id.
 * <p>
 * Unlike previous approaches, this means a little more (ont-time) up-front cost
 * when rendering, but no more traversal-on-event, and a much simpler mutations
 * sync algorithm.
 * <p>
 * The algorithm:
 * <ul>
 * <li>Takes a container remote node, a markup string and a list of ref-ids
 * <li>Sets the inner html of the remote node, and begins to iterate (in js)
 * with the refids (passed as a string if in gwt dev mode)
 * <li>As it iterates, any adjacent text nodes generated from the markup
 * (webkit/blink oddity) will be combined
 * <li>As it iterates, it applies the elements of the ref-id list/array to the
 * nodes.
 * <li>On iteration complete, if the ref-id list size exactly matches the
 * traversed node count, all is well
 * <li>If it does *not* match - rerun iteration, this time generating client
 * ref-ids, and send the result back to the local dom ( more expensive, but
 * should work). The localdom/java code should only hit this issue when
 * inserting markup chunks anyway (such as a document segment), so it won't
 * break because it should be able to handle changes to that document segment
 * markup anyway
 * </ul>
 */
class MarkupJso {
	static class MarkupResult {
	}

	MarkupResult markup(Element container, String markup,
			List<Integer> refIds) {
		MarkupResult result = new MarkupResult();
		// build the json refid array
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		for (int idx = 0; idx < refIds.size(); idx++) {
			if (idx > 0) {
				builder.append(',');
			}
			builder.append(refIds.get(idx));
		}
		builder.append(']');
		String refIdArrayJson = builder.toString();
		ElementJso remote = (ElementJso) container.remote();
		remote.setInnerHTML(markup);
		boolean success = traverseAndMark(remote, refIdArrayJson);
		return result;
	}

	final native boolean traverseAndMark(ElementJso container,
			String refIdArrayJson) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
      this.innerHTML = html || '';
	  return true;
	}-*/;
}
