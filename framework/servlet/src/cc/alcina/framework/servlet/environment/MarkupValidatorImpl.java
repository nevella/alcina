package cc.alcina.framework.servlet.environment;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HtmlParser;
import com.google.gwt.dom.client.LocalDom;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;

@Registration(LocalDom.MarkupValidator.class)
public class MarkupValidatorImpl implements LocalDom.MarkupValidator {
	DomNode node;

	TagContainer container;

	List<TagContainer> containers;

	boolean hadChanges;

	@Override
	public String validate(String markup) {
		String outer = Ax.format("<div>%s</div>", markup);
		Element elem = HtmlParser.parseMarkup(outer);
		this.node = elem.asDomNode();
		fixTableStructure();
		return hadChanges ? elem.getInnerHTML() : markup;
	}

	void fixTableStructure() {
		fixTableStructure("tr", "tbody", "thead", "tfoot");
		fixTableStructure("thead", "table");
		fixTableStructure("tbody", "table");
		fixTableStructure("tfoot", "table");
	}

	void fixTableStructure(String tag, String... validParents) {
		container = null;
		containers = new ArrayList<>();
		node.stream().filter(n -> n.tagIs(tag)).forEach(n -> {
			if (!n.ancestors().has(validParents)) {
				if (container == null || !container.canExtendWith(n)) {
					hadChanges = true;
					container = new TagContainer(validParents[0]);
					containers.add(container);
				}
				container.extendWith(n);
			}
		});
		containers.forEach(TagContainer::flush);
	}

	static class TagContainer {
		List<DomNode> nodes = new ArrayList<>();

		String containerTag;

		TagContainer(String containerTag) {
			this.containerTag = containerTag;
		}

		boolean canExtendWith(DomNode n) {
			return nodes.isEmpty()
					|| Ax.last(nodes).relative().nextSiblingElement() == n;
		}

		void extendWith(DomNode n) {
			nodes.add(n);
		}

		void flush() {
			DomNode first = nodes.get(0);
			DomNode table = first.builder().tag(containerTag)
					.insertBeforeThis();
			table.children.append(nodes);
		}
	}
}
