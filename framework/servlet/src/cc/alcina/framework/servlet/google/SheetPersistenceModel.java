package cc.alcina.framework.servlet.google;

import cc.alcina.framework.servlet.google.SheetAccessor.SheetAccess;

public abstract class SheetPersistenceModel {
	private SheetPersistence persistence;

	public SheetPersistenceModel() {
		persistence = new SheetPersistence(this, getAccess());
	}

	public abstract SheetAccess getAccess();

	public void load() {
		persistence.load();
	}

	public void save() {
		persistence.save();
	}
}