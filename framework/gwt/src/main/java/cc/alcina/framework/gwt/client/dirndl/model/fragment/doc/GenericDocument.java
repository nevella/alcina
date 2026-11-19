package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

public class GenericDocument extends FragmentDocument<GenericDocumentRoot> {
	@Override
	public Class<? extends SpecialisedDescendant>
			permittedSpecialisedDescendants() {
		return Descendant.class;
	}

	public interface Descendant extends FragmentDocument.SpecialisedDescendant {
	}
}
