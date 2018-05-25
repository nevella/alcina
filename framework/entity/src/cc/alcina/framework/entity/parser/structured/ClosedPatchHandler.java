package cc.alcina.framework.entity.parser.structured;

public interface ClosedPatchHandler {
	boolean permitInvalidClose(XmlStructuralJoin join, String tag);
}
