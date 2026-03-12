package cc.alcina.framework.gwt.client.dirndl.cmp.sequence;

import java.util.List;

public interface HasFilteredSequenceElements {
	List<?> provideFilteredSequenceElements(boolean ignoreRowsLimit,
			boolean onlySelectedIfAnySelected);
}