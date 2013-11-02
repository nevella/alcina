/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.gwt.client.ide.widget;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.persistence.Transient;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.JobInfo;
import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.widget.Link;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

/**
 * 
 * @author Nick Reddel
 */
public class ActionProgress extends Composite implements
		SourcesPropertyChangeEvents {
	private static final String CANCELLED = " - Cancelled";

	protected transient MutablePropertyChangeSupport propertyChangeSupport = new MutablePropertyChangeSupport(
			this);

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.propertyChangeSupport.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		this.propertyChangeSupport.addPropertyChangeListener(propertyName,
				listener);
	}

	public void firePropertyChange(PropertyChangeEvent evt) {
		this.propertyChangeSupport.firePropertyChange(evt);
	}

	public void firePropertyChange(String propertyName, boolean oldValue,
			boolean newValue) {
		this.propertyChangeSupport.firePropertyChange(propertyName, oldValue,
				newValue);
	}

	public void firePropertyChange(String propertyName, int oldValue,
			int newValue) {
		this.propertyChangeSupport.firePropertyChange(propertyName, oldValue,
				newValue);
	}

	public void firePropertyChange(String propertyName, Object oldValue,
			Object newValue) {
		this.propertyChangeSupport.firePropertyChange(propertyName, oldValue,
				newValue);
	}

	@Transient
	public PropertyChangeListener[] getPropertyChangeListeners() {
		return this.propertyChangeSupport.getPropertyChangeListeners();
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.propertyChangeSupport.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		this.propertyChangeSupport.removePropertyChangeListener(propertyName,
				listener);
	}

	private final Long id;

	private FlowPanel fp;

	private JobInfo info = new JobInfo();

	private Grid grid;

	private Label jobName;

	private Label message;

	private FlowPanel progress;

	private HTML times;

	private FlowPanel bar;

	public static final int REFRESH_DELAY_MS = 3500;

	private Link cancelLink;

	private InlineLabel cancellingStatusMessage;

	private Timer timer;

	private final AsyncCallback<JobInfo> completionCallback;

	public void minimal() {
		CellFormatter cf = grid.getCellFormatter();
		cf.setVisible(0, 0, false);
		cf.setVisible(0, 1, false);
		cf.setVisible(1, 0, false);
		cf.setVisible(1, 1, false);
		fp.addStyleName("minimal");
	}
	private void cancelJob() {
		cancelLink.setVisible(false);
		cancellingStatusMessage.setText(" - Cancelling...");
		cancellingStatusMessage.setVisible(true);
		ClientBase.getCommonRemoteServiceAsyncInstance()
				.pollJobStatus(getId(), true, new AsyncCallback<JobInfo>() {
					public void onFailure(Throwable e) {
						cancellingStatusMessage.setText(" - Error cancelling");
						throw new WrappedRuntimeException(e);
					}

					public void onSuccess(JobInfo result) {
						cancellingStatusMessage.setText(CANCELLED);
					}
				});
		return;
	}

	public ActionProgress(final Long id) {
		this(id, null);
	}

	private int maxConnectionFailure = 2;

	public int getMaxConnectionFailure() {
		return this.maxConnectionFailure;
	}

	public void setMaxConnectionFailure(int maxConnectionFailure) {
		this.maxConnectionFailure = maxConnectionFailure;
	}

	public ActionProgress(final Long id,
			AsyncCallback<JobInfo> completionCallback) {
		this.id = id;
		this.completionCallback = completionCallback;
		this.fp = new FlowPanel();
		fp.setStyleName("alcina-ActionProgress");
		grid = new Grid(4, 2);
		jobName = new InlineLabel();
		FlowPanel jobNCancel = new FlowPanel();
		jobNCancel.add(jobName);
		jobName.setStyleName("pad-right-5");
		this.cancelLink = new Link("(Cancel)", new ClickHandler() {
			public void onClick(ClickEvent event) {
				Widget sender = (Widget) event.getSource();
				cancelJob();
			}
		});
		jobNCancel.add(cancelLink);
		cancellingStatusMessage = new InlineLabel();
		cancellingStatusMessage.setVisible(false);
		jobNCancel.add(cancellingStatusMessage);
		addToGrid("Job", jobNCancel);
		times = new HTML();
		addToGrid("Time", times);
		message = new Label();
		addToGrid("Status", message);
		progress = new FlowPanel();
		progress.setStyleName("progress");
		bar = new FlowPanel();
		bar.setStyleName("bar");
		bar.add(progress);
		addToGrid("Progress", bar);
		grid.setCellSpacing(2);
		fp.add(grid);
		initWidget(fp);
		updateProgress();
		timer = new Timer() {
			boolean checking = false;

			@Override
			public void run() {
				AsyncCallback<JobInfo> callback = new AsyncCallback<JobInfo>() {
					public void onFailure(Throwable caught) {
						checking = false;
						if (maxConnectionFailure-- <= 0) {
							stopTimer();
							if (ActionProgress.this.completionCallback != null) {
								ActionProgress.this.completionCallback
										.onFailure(caught);
							}
							throw new WrappedRuntimeException(caught);
						}
					}

					public void onSuccess(JobInfo info) {
						checking = false;
						if (info.isComplete()) {
							stopTimer();
							if (ActionProgress.this.completionCallback != null) {
								ActionProgress.this.completionCallback
										.onSuccess(info);
							}
						}
						setJobInfo(info);
						fireNullPropertyChange("Updated");
					}
				};
				if (!checking) {
					ClientBase.getCommonRemoteServiceAsyncInstance()
							.pollJobStatus(id, false, callback);
					checking = true;
				}
			}
		};
	}

	@Override
	protected void onAttach() {
		super.onAttach();
		startTimer();
	}

	@Override
	protected void onDetach() {
		stopTimer();
		super.onDetach();
	}

	public void fireNullPropertyChange(String name) {
		this.propertyChangeSupport.fireNullPropertyChange(name);
	}

	private int row = 0;

	private boolean stopped;

	private void addToGrid(String label, Widget widget) {
		InlineLabel l = new InlineLabel(label + ": ");
		l.setStyleName("caption");
		grid.setWidget(row, 0, l);
		grid.setWidget(row, 1, widget);
		grid.getCellFormatter().setVerticalAlignment(row, 0,
				HasVerticalAlignment.ALIGN_TOP);
		grid.getCellFormatter().setVerticalAlignment(row, 1,
				HasVerticalAlignment.ALIGN_TOP);
		row++;
	}

	private void updateProgress() {
		jobName.setText(info.getJobName());
		String time = info.getStartTime() == null ? "" : "Start: "
				+ CommonUtils.formatDate(info.getStartTime(),
						DateStyle.AU_DATE_TIME_MS);
		if (info.getEndTime() != null) {
			time += "<br>End: "
					+ CommonUtils.formatDate(info.getEndTime(),
							DateStyle.AU_DATE_TIME_MS);
		}
		times.setHTML(time);
		String msg = info.getProgressMessage();
		if (info.getErrorMessage() != null) {
			msg = info.getErrorMessage();
			message.setStyleName("error");
		}
		if (!info.isComplete()
				&& cancellingStatusMessage.getText().equals(CANCELLED)) {
			cancellingStatusMessage.setVisible(false);
			cancelLink.setVisible(true);
		}
		message.setText(msg);
		progress.setWidth(Math.max(0,
				((int) (bar.getOffsetWidth() - 2) * info.getPercentComplete()))
				+ "px");
	}

	public void setJobInfo(JobInfo jobInfo) {
		this.info = jobInfo;
		updateProgress();
	}

	public JobInfo getJobInfo() {
		return info;
	}

	public Long getId() {
		return id;
	}

	public void ensureRunning() {
		if (timer != null && stopped) {
			startTimer();
		}
	}

	private void stopTimer() {
		stopped = true;
		timer.cancel();
	}

	private void startTimer() {
		stopped = false;
		timer.scheduleRepeating(REFRESH_DELAY_MS);
	}
}
