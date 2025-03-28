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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
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

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.logic.LazyPropertyChangeSupport;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.DateStyle;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.widget.Link;

/**
 *
 * @author Nick Reddel
 */
public class ActionProgress extends Composite
		implements SourcesPropertyChangeEvents {
	private static final String CANCELLED = " - Cancelled";

	public static int REFRESH_DELAY_MS = 3500;

	protected transient LazyPropertyChangeSupport propertyChangeSupport = new LazyPropertyChangeSupport(
			this);

	private FlowPanel fp;

	private JobTracker jobTracker = new JobTracker();

	private Grid grid;

	private Label jobName;

	private Label message;

	private FlowPanel progress;

	private HTML times;

	private FlowPanel bar;

	private Link cancelLink;

	private InlineLabel cancellingStatusMessage;

	private Timer timer;

	private final AsyncCallback<JobTracker> completionCallback;

	private int maxConnectionFailure = 2;

	private String id;

	private int row = 0;

	private boolean stopped;

	private int timerCallCount = 0;

	public ActionProgress(String id) {
		this(id, null);
	}

	public ActionProgress(String id,
			AsyncCallback<JobTracker> completionCallback) {
		this.id = id;
		this.completionCallback = completionCallback;
		this.fp = new FlowPanel();
		fp.setStyleName("alcina-ActionProgress");
		grid = new Grid(4, 2);
		jobName = new InlineLabel();
		FlowPanel jobNActions = new FlowPanel();
		jobNActions.add(jobName);
		jobName.setStyleName("pad-right-5");
		Link detailLink = new Link("(Detail)", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Widget sender = (Widget) event.getSource();
				navigateToDetail();
			}
		});
		detailLink.setTarget("_blank");
		detailLink.setStyleName("pad-right-5");
		this.cancelLink = new Link("(Cancel)", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Widget sender = (Widget) event.getSource();
				cancelJob();
			}
		});
		jobNActions.add(detailLink);
		jobNActions.add(cancelLink);
		cancellingStatusMessage = new InlineLabel();
		cancellingStatusMessage.setVisible(false);
		jobNActions.add(cancellingStatusMessage);
		addToGrid("Job", jobNActions);
		times = new HTML();
		addToGrid("Time", times);
		message = new Label();
		message.setStyleName("message");
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
				AsyncCallback<JobTracker> callback = new AsyncCallback<JobTracker>() {
					@Override
					public void onFailure(Throwable caught) {
						onReturned();
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

					private void onReturned() {
						checking = false;
						scheduleNext();
					}

					@Override
					public void onSuccess(JobTracker tracker) {
						onReturned();
						if (tracker == null) {
							tracker = new JobTracker();
							tracker.setJobName("Unknown job");
							tracker.setComplete(true);
							tracker.setProgressMessage("---");
						}
						if (tracker.isComplete()) {
							stopTimer();
							if (ActionProgress.this.completionCallback != null) {
								ActionProgress.this.completionCallback
										.onSuccess(tracker);
							}
						}
						setJobTracker(tracker);
						fireUnspecifiedPropertyChange("Updated");
					}
				};
				if (!checking) {
					Client.commonRemoteService().pollJobStatus(id, false,
							callback);
					checking = true;
				}
			}
		};
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.propertyChangeSupport.addPropertyChangeListener(listener);
	}

	@Override
	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		this.propertyChangeSupport.addPropertyChangeListener(propertyName,
				listener);
	}

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

	private void cancelJob() {
		cancelLink.setVisible(false);
		cancellingStatusMessage.setText(" - Cancelling...");
		cancellingStatusMessage.setVisible(true);
		Client.commonRemoteService().pollJobStatus(getId(), true,
				new AsyncCallback<JobTracker>() {
					@Override
					public void onFailure(Throwable e) {
						cancellingStatusMessage.setText(" - Error cancelling");
						throw new WrappedRuntimeException(e);
					}

					@Override
					public void onSuccess(JobTracker result) {
						cancellingStatusMessage.setText(CANCELLED);
					}
				});
		return;
	}

	public void ensureRunning() {
		if (timer != null && stopped) {
			startTimer();
		}
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

	public void fireUnspecifiedPropertyChange(String name) {
		this.propertyChangeSupport.fireUnspecifiedPropertyChange(name);
	}

	public String getId() {
		return this.id;
	}

	public JobTracker getJobTracker() {
		return jobTracker;
	}

	public int getMaxConnectionFailure() {
		return this.maxConnectionFailure;
	}

	public void minimal() {
		CellFormatter cf = grid.getCellFormatter();
		cf.setVisible(0, 0, false);
		cf.setVisible(0, 1, false);
		cf.setVisible(1, 0, false);
		cf.setVisible(1, 1, false);
		fp.addStyleName("minimal");
	}

	private void navigateToDetail() {
		Window.open(Ax.format("/job.do?action=detail&id=%s", getId()), "_blank",
				"");
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

	@Override
	public PropertyChangeListener[] propertyChangeListeners() {
		return this.propertyChangeSupport.getPropertyChangeListeners();
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.propertyChangeSupport.removePropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		this.propertyChangeSupport.removePropertyChangeListener(propertyName,
				listener);
	}

	private void scheduleNext() {
		if (stopped) {
			return;
		}
		timerCallCount++;
		if (timerCallCount <= 2) {
			// do 2 quick initial calls, then slower repeat
			timer.schedule(200);
		} else {
			timer.schedule(2000);
		}
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setJobTracker(JobTracker jobTracker) {
		this.jobTracker = jobTracker;
		updateProgress();
	}

	public void setMaxConnectionFailure(int maxConnectionFailure) {
		this.maxConnectionFailure = maxConnectionFailure;
	}

	private void startTimer() {
		stopped = false;
		timerCallCount = 0;
		scheduleNext();
	}

	private void stopTimer() {
		stopped = true;
		timerCallCount = 0;
		timer.cancel();
	}

	private void updateProgress() {
		jobName.setText(jobTracker.getJobName());
		String time = jobTracker.getStartTime() == null ? ""
				: "Start: " + DateStyle.DATE_TIME_MS
						.format(jobTracker.getStartTime());
		if (jobTracker.getEndTime() != null) {
			time += "<br>End: "
					+ DateStyle.DATE_TIME_MS.format(jobTracker.getEndTime());
		}
		times.setHTML(time);
		String messageText = jobTracker.getProgressMessage();
		if (useTreeProgress) {
			messageText = jobTracker.getLeafCount();
		}
		if (jobTracker.getJobResultType() == JobResultType.FAIL) {
			messageText = jobTracker.getJobResult();
			message.addStyleName("error");
		}
		if (!jobTracker.isComplete()
				&& cancellingStatusMessage.getText().equals(CANCELLED)) {
			cancellingStatusMessage.setVisible(false);
			cancelLink.setVisible(true);
		}
		message.setText(messageText);
		if (jobTracker.isComplete()) {
			jobTracker.setPercentComplete(
					Math.max(1.0, jobTracker.getPercentComplete()));
		}
		if (isAttached()) {
			progress.setWidth(Math.max(0, ((int) (bar.getOffsetWidth() - 2)
					* jobTracker.getPercentComplete())) + "px");
		}
	}

	public boolean useTreeProgress;
}
