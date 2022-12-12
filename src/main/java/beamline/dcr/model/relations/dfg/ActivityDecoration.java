package beamline.dcr.model.relations.dfg;

public class ActivityDecoration {

	double observations = 0;
	double avgIndex = 0;
	int currentIndex = 0;
	
	double observationsFirstOccurrance = 0;
	double avgFirstOccurrence = 0;


	public void addNewObservation(int currentIndex, boolean isFirstOccurrance) {
		this.currentIndex = currentIndex;

		observations++;
		avgIndex = avgIndex + ((currentIndex - avgIndex) / observations);
		
		if (isFirstOccurrance) {
			observationsFirstOccurrance++;
			avgFirstOccurrence = avgFirstOccurrence + ((currentIndex - avgFirstOccurrence) / observationsFirstOccurrance);
		}
	}
	public void decrementDecorations(boolean firstOccurrence){
		if (firstOccurrence){
			avgFirstOccurrence = avgFirstOccurrence - (1 / observationsFirstOccurrance);
		}
		avgIndex = avgIndex - ( 1 / observations);
	}
	public void removeObservation(){
		avgFirstOccurrence = avgFirstOccurrence * observationsFirstOccurrance / ( observationsFirstOccurrance - 1);
		avgIndex = avgIndex * observations / ( observations - 1);

		observations--;
		observationsFirstOccurrance--;
	}

	public void incrementNumFirstObservations(){
		observationsFirstOccurrance++;

	}
	public double getNumObservations() {
		return observations;
	}
	public double getAverageIndex() {
		return avgIndex;
	}
	public boolean appearMostOnce(){ return 1==observations/observationsFirstOccurrance;}
	public double getTraceAppearances(){ return observationsFirstOccurrance;}
	public double getAverageFirstOccurrence() {
		return avgFirstOccurrence;
	}
	
	@Override
	public String toString() {
		return "average index: " + avgIndex + " ; average first occurrence: " + avgFirstOccurrence;
	}

}
