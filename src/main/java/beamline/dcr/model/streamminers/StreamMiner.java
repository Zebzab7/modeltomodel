package beamline.dcr.model.streamminers;

import beamline.dcr.model.relations.dfg.ExtendedDFG;

public interface StreamMiner {


    public void observeEvent(String traceId,String activityName);

    public ExtendedDFG getExtendedDFG();

    public void saveLog(String filePath);
    public int getNumberEventsSaved();

}
