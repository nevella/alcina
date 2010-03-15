package cc.alcina.framework.common.client.search;

import java.util.Date;

public class AbstractDateCriterion extends SearchCriterion {
	private Date date;

	public AbstractDateCriterion() {
		super();
	}

	public AbstractDateCriterion(Date date) {
		setDate(date);
	}
	public AbstractDateCriterion(String displayName,Date date) {
		this(displayName);
		setDate(date);
	}

	public AbstractDateCriterion(String displayName) {
		super(displayName);
	}

	public AbstractDateCriterion(String displayName, String propertyName) {
		super(displayName, propertyName);
	}

	public void setDate(Date date) {
		Date old_date = this.date;
		this.date = date;
		propertyChangeSupport.firePropertyChange("date", old_date, date);
	}

	public Date getDate() {
		return date;
	}
}