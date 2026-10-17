package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/*
 * Marker types denoting document version
 */
@Registration.Self
public interface FragmentDocumentVersion {
	default String toVersionString() {
		return getClass().getSimpleName();
	}

	public static class UNKNOWN implements FragmentDocumentVersion {
	}

	public static class _1_0 implements FragmentDocumentVersion {
	}

	static FragmentDocumentVersion valueOf(String versionStr) {
		return Registry.query(FragmentDocumentVersion.class).implementations()
				.filter(impl -> Objects.equals(impl.toVersionString(),
						versionStr))
				.findFirst().orElse(null);
	}
}
