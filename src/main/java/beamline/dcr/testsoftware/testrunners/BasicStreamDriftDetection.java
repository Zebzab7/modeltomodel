package beamline.dcr.testsoftware.testrunners;

public class BasicStreamDriftDetection {
    public static void main(String[] args) {
      //Test parameters
        String eventlogNumber =args[0];
        int relationsThreshold = Integer.parseInt(args[1]);
        String[] patternList = args[2].split(" ");
        String[] transitiveReductionList = args[3].split(" ");
        boolean saveAsXml = Boolean.parseBoolean(args[4]);
        boolean compareToDisCoveR = Boolean.parseBoolean(args[5]); // false for reference model true for DisCoveR at windowsize
        boolean saveEventLogs= Boolean.parseBoolean(args[6]);
        String[] traceWindowSizesStringList = args[7].split(" ");
        String[] maxTracesStringList = args[8].split(" ");
        int observationsBeforeEvaluation = Integer.parseInt(args[9]);
        String[] dcrConstraints = args[10].split(" ");
    }
}
