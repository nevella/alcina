package cc.alcina.framework.gwt.client.tour;

import com.google.gwt.core.client.JsArray;

import cc.alcina.framework.gwt.client.tour.Tour.Step;

public class TourModel {
	static TourModel fromJson(String tourJson) {
		TourModel model = new TourModel();
		model.tour = Tour.fromJson(tourJson);
		return model;
	}

	private int stepIdx;

	private Tour tour;

	public int getCurrentStepIndex() {
		return stepIdx;
	}

	Step getCurrentStep() {
		return getSteps().get(stepIdx);
	}

	String getName() {
		return tour.getName();
	}

	JsArray<Step> getSteps() {
		return tour.getSteps();
	}

	void gotoStep(int idx) {
		stepIdx = idx;
	}

	boolean hasNext() {
		return stepIdx < getSteps().length() - 1;
	}

	boolean hasPrevious() {
		return stepIdx > 0;
	}
}
