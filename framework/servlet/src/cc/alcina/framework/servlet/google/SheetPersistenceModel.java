package cc.alcina.framework.servlet.google;

import cc.alcina.framework.servlet.google.SheetAccessor.SheetAccess;

public abstract class SheetPersistenceModel {
	private SheetPersistence persistence;

	public SheetPersistenceModel() {
		persistence = new SheetPersistence(this, getAccess());
	}

	public abstract SheetAccess getAccess();

	public void load() {
		load(false);
	}

	public void load(boolean useLocalCached) {
		persistence.load(useLocalCached);
	}

	public void save(boolean dryRun) {
		persistence.save(dryRun);
	}
}