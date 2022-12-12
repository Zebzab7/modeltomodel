package beamline.dcr.testsoftware;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.Pair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PlotResults {
    private final List<String> constraintList;
    private final List<Pair<List<Double>,String>> performanceLists;



    public PlotResults(String resultFilePath) {

        this.constraintList = new ArrayList<>();
        this.performanceLists = new ArrayList<>();

        try{

            BufferedReader br = new BufferedReader(new FileReader(resultFilePath));

            CSVParser parser = CSVFormat.DEFAULT.withDelimiter(',').withHeader().parse(br);


            for (Map.Entry<String, Integer> entry : parser.getHeaderMap().entrySet())
                if (entry.getKey().equals("illegal_traces"))
                    continue;
                else
                    this.performanceLists.add(Pair.of(new ArrayList<>(),entry.getKey()));



            for(CSVRecord record : parser) {

                this.constraintList.add(record.get(0));


                for(int i = 0; i < performanceLists.size()-1; i++){
                    //-1 to ignore identified illegal traces
                    List<Double> performanceMetricList = performanceLists.get(i).getLeft();
                    performanceMetricList.add(Double.valueOf(record.get(i+1)));

                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }


    public void createLinePlot(String filePath, String title, String[] metrics) throws IOException {

        DefaultCategoryDataset dataset = createCategoryDataset(metrics);
        final JFreeChart chart = ChartFactory.createLineChart(
                title,      // chart title
                "Setting",                      // x axis label
                "Performance",                      // y axis label
                dataset,                  // data
                PlotOrientation.VERTICAL,
                true,                     // include legend
                true,                     // tooltips
                false                     // urls
        );
        ChartUtils.saveChartAsPNG(new File(filePath+"/"+ Arrays.toString(metrics) + "_" +  java.time.LocalDate.now() +".png"), chart, 1200, 900);
        System.out.println("Plot saved");
    }

    private DefaultCategoryDataset createCategoryDataset(String[] metrics) {
        DefaultCategoryDataset dataSeries = new DefaultCategoryDataset();

        for (Pair<List<Double>,String> listToPlot : performanceLists){
            if( Arrays.asList(metrics).contains(listToPlot.getRight())){
                for (int i = 0; i<constraintList.size(); i++) {
                    dataSeries.addValue(listToPlot.getLeft().get(i), listToPlot.getRight(), constraintList.get(i));
                }
            }
        }
        return dataSeries;
    }


}
