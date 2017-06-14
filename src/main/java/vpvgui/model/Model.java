package vpvgui.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import vpvgui.exception.DownloadFileNotFoundException;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

/**
 * Created by peter on 05.05.17.
 * Modified by blauh on 06.12.17 to add Settings instance variable to Model constructor.
 */
public class Model {

    private DataSource datasource=null;

    private List<RestrictionEnzyme> enzymelist;

    private List<String> geneList;

    private Settings settings;

    /**
     * This suffix is appended to the project name to get the name of the file for storing the
     * project settings.
     */
    public static final String PROJECT_FILENAME_SUFFIX = "-settings.txt";

    /** The genome build chosen by theuser, e.g., hg19, GRCh38, mm10 */
    private StringProperty genomeBuild = new SimpleStringProperty(this, "genomeBuild");
    public String getGenomeBuild() {return genomeBuild.get();}
    public void setGenomeBuild(String newDatabase) {genomeBuild.set(newDatabase);}
    public StringProperty genomeBuildProperty() {return genomeBuild;}

    /** This is coupled to genomeTranscriptomeList in the Controller
     *  ("UCSC-hg19","UCSC-hg38", "UCSC-mm10");
     *  Consider better design
     *
     *  TODO: eliminate redundant code after Settings class is integrated into gui HB
     */

    public String genomeURL=null;
    public String getGenomeURL() {
        return this.genomeURL;
    }
    public String genomeBasename=null;
    public String getGenomeBasename(){ return genomeBasename;}

    public String transcriptsURL=null;
    public String getTranscriptsURL(){return transcriptsURL;}
    public String transcriptsBasename=null;
    public String getTranscriptsBasename(){return transcriptsBasename;}

    public String repeatsURL=null;

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings s) {
        settings = s;
    }

    public Model() {
        initializeEnzymesFromFile();
        settings = Settings.factory();    // creates empty Settings object
    }

    /** @return List of enzymes for the user to choose from. */
    public List<RestrictionEnzyme> getRestrictionEnymes() { return enzymelist; }

    /** This method expects there to be a file called enzymelist.tab in
     * src/main/resources. This file has a header line (with #) and
     * then a list of restriction enzymes structured as name\tsite
     */
    private void initializeEnzymesFromFile() {
        enzymelist = new ArrayList<>();
        String fileName="enzymelist.tab";
        //InputStream s = Model.class.getClassLoader().getResourceAsStream("/data/enzymelist.tab");
	File file = new File(getClass().getClassLoader().getResource("enzymelist.tab").getFile());
	System.err.println("&&&&&&File"+file);
        try {
        BufferedReader br =  new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/enzymelist.tab")));
        // file = new File(getClass().getClassLoader().getResource("/home/robinp/GIT/captureCpipeline/panel_design/vpv/target/data/enzymelist.tab").getFile());

        System.err.println("22 &&&&&&File"+file);
	
        //ClassLoader classLoader = getClass().getClassLoader();
	// File file = new File(classLoader.getResource("data/enzymelist.tab").getFile());
            String line;

            while ((line =br.readLine())!=null) {

                System.err.println(line);
                if (line.startsWith("#"))
                    continue; /* skip header*/
                String a[] = line.split("\\s");
                System.err.println("N="+a.length);
                RestrictionEnzyme re = new RestrictionEnzyme(a[0],a[1]);
                enzymelist.add(re);
            }

            br.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method uses {@link DataSource} as a source for collections
     * of paths and names that represent the sets of data we will need
     * to analyze any one genome.
     */
     public void adjustGenomeDownloadPaths() throws DownloadFileNotFoundException {
        String gb = getGenomeBuild();
        if (gb.equals("UCSC-hg19")){
            this.datasource = DataSource.createUCSChg19();
        } else if (gb.equals("UCSC-hg38")){
             this.datasource = DataSource.createUCSChg38();
         } else {
            throw new DownloadFileNotFoundException(String.format("Need to implement code for genome build %s.",gb));
        }
        this.genomeURL = datasource.getGenomeURL();
        //this.transcriptsURL = datasource.getTranscriptsURL();
    }

    public void setGeneSymbols(String[] symbols) {
        geneList = Arrays.asList(symbols);
        System.err.println("Genes");
        for (String g : geneList) {
            System.err.println("\t"+g);
        }
    }

}
