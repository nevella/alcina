package cc.alcina.framework.gwt.client.tour;

import java.util.List;

public class TourModel {
	static TourModel fromJson(String tourJson) {
		TourModel model = new TourModel();
		model.tour = TourJso.fromJson(tourJson);
		return model;
	}

	private int stepIdx;

	private Tour tour;

	public TourModel() {
	}

	public TourModel(Tour tour) {
		this.tour = tour;
	}

	public int getCurrentStepIndex() {
		return stepIdx;
	}

	public List<? extends Tour.Step> getSteps() {
		return tour.getSteps();
	}

	Tour.Step getCurrentStep() {
		return getSteps().get(stepIdx);
	}

	String getName() {
		return tour.getName();
	}

	void gotoStep(int idx) {
		stepIdx = idx;
	}

	boolean hasNext() {
		return stepIdx < getSteps().size() - 1;
	}

	boolean hasPrevious() {
		return stepIdx > 0;
	}
}
