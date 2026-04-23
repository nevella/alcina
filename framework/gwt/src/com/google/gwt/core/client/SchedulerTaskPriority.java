package com.google.gwt.core.client;

public enum SchedulerTaskPriority {
	_DEFAULT(10), AFTER_DEFAULT(20);

	public interface HasTaskPriority {
		int getTaskPriority();

		public interface Typed extends HasTaskPriority {
			SchedulerTaskPriority getTaskPriorityTyped();

			default int getTaskPriority() {
				return getTaskPriorityTyped().value;
			}
		}
	}

	public int value;

	private SchedulerTaskPriority(int value) {
		this.value = value;
	}
}