package beamline.dcr.modeltomodelcomparison.testrunners;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ModelViewer {
    
    public static void main(String[] args) throws MalformedURLException {
        int modelId = 1480516;
        int startVersion = 30;

        String rootPath = System.getProperty("user.dir");
        String modelPath = rootPath + "/src/main/java/beamline/dcr/testsoftware/publicrepodataset" + "/model" + 1480516;

        String response = DataSetLoader.httpRequest(new URL("https://repository.dcrgraphs.net/api/graphs/" + modelId + "/image"));

        System.out.println(response);

        // String response = DataSetLoader.httpRequest(new URL("https://repository.dcrgraphs.net/api/graphs/" + modelId + "/versions/" + startVersion + "/graph"));
        
        BufferedImage image = DataSetLoader.getModelImage(response);
        
        JFrame frame = new JFrame();
        frame.getContentPane().add(new JLabel(new ImageIcon(image)));
        frame.pack();
        frame.setVisible(true);
    }
}