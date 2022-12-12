package beamline.dcr.view;

import beamline.dcr.model.relations.DcrModel;
import org.apache.commons.lang3.tuple.Triple;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;

public class DcrModelJson {
    private DcrModel model;


    public DcrModelJson(DcrModel model) {
        this.model = model;

    }

    public void toFile(String fileName) throws IOException {
        JSONArray jsonArray = new JSONArray();
        for (Triple<String, String, DcrModel.RELATION> relation : model.getRelations()) {

            JSONObject pattern = new JSONObject();
            pattern.put("type",relation.getRight().name().toLowerCase());
            pattern.put("source",relation.getLeft());
            pattern.put("target",relation.getMiddle());

            jsonArray.add(pattern);

        }
        JSONObject jsonObjectFile = new JSONObject();

        jsonObjectFile.put("Relation", jsonArray);

        FileWriter fileWriter = new FileWriter(fileName + ".json");
        fileWriter.write(jsonObjectFile.toJSONString());
        fileWriter.close();

    }
}
