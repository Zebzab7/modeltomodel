package beamline.dcr.view;

import beamline.dcr.model.relations.DcrModel;

import org.apache.commons.lang3.tuple.Triple;


public class DcrModelText {

    private DcrModel model;


    public DcrModelText(DcrModel model) {
        this.model = model;

    }


    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();

        b.append("PATTERNS");
        b.append(System.lineSeparator());


        String pattern ="";
        for (Triple<String, String, DcrModel.RELATION> relation : model.getRelations()) {
            switch (relation.getRight()) {
                case CONDITION:
                    pattern = "-->*";
                    break;
                case RESPONSE:
                    pattern = "*-->";
                    break;
                case INCLUDE:
                    pattern = "-->+";
                    break;
                case EXCLUDE:
                    pattern = "-->%";
                    break;
            }
            b.append(relation.getLeft()+" " + pattern +
                    " " + relation.getMiddle());
            b.append(System.lineSeparator());
        }

        return b.toString();
    }

}
