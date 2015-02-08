package cc.alcina.framework.gwt.client.tour;

import cc.alcina.framework.gwt.client.tour.Tour.Step;

import com.google.gwt.core.client.JsArray;

public class TourModel {
	private int stepIdx;

	Step getCurrentStep() {
		return getSteps().get(stepIdx);
	}

	boolean hasNext() {
		return stepIdx < getSteps().length() - 1;
	}

	void gotoStep(int idx) {
		stepIdx = idx;
	}

	boolean hasPrevious() {
		return stepIdx > 0;
	}

	private Tour tour;

	String getName() {
		return tour.getName();
	}

	JsArray<Step> getSteps() {
		return tour.getSteps();
	}

	static TourModel fromJson(String tourJson) {
		TourModel model = new TourModel();
		model.tour = Tour.fromJson(tourJson);
		return model;
	}

	public int getCurrentStepIndex() {
		return stepIdx;
	}
}
