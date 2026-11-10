package cc.alcina.framework.common.client.dom;

public enum DomNodeType {
	//@formatter:off
	ELEMENT(1),
	ATTRIBUTE(2),
	TEXT(3),
	CDATA_SECTION(4),
	ENTITY_REFERENCE(5),
	ENTITY(6),
	PROCESSING_INSTRUCTION(7),
	COMMENT(8),
	DOCUMENT(9),
	DOCUMENT_TYPE(10),
	DOCUMENT_FRAGMENT(11),
	NOTATION(12);
	//@formatter:on

	public static DomNodeType fromW3cNode(org.w3c.dom.Node node) {
		return values()[node.getNodeType() - 1];
	}

	private int id;

	//@formatter:on
	private DomNodeType(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public boolean hasVariantName() {
		return w3cNodeTypeString() == null;
	}

	public String w3cNodeTypeString() {
		switch (this) {
		case CDATA_SECTION:
			return NodeNames.CDATA_SECTION;
		case COMMENT:
			return NodeNames.COMMENT;
		case DOCUMENT:
			return NodeNames.DOCUMENT;
		case DOCUMENT_FRAGMENT:
			return NodeNames.DOCUMENT_FRAGMENT;
		case TEXT:
			return NodeNames.TEXT;
		default:
			return null;
		}
	}

	public static interface NodeNames {
		public static final String TEXT = "#text";

		public static final String CDATA_SECTION = "#cdata-section";

		public static final String COMMENT = "#comment";

		public static final String DOCUMENT = "#document";

		public static final String DOCUMENT_FRAGMENT = "#document-fragment";
	}
}