package gopher.gui.entrezgenetable;



import gopher.model.GopherGene;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import gopher.framework.Signal;
import gopher.gui.popupdialog.PopupFactory;
import gopher.io.RefGeneParser;
import gopher.model.Model;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

/**
 * This is the window that appears so that the user can upload target genes. The dialog has three buttons.
 * <ol>
 *     <li>Upload causes {@link #uploadGenes(ActionEvent)} to be run, which fills the set {@link #symbols}.
 *     This set can contain valid and invalid symbols</li>
 *     <li>Validate causes {@link #validateGeneSymbols(ActionEvent)} to be run, which causes the
 *     {@link RefGeneParser} object to store {@link GopherGene} objects for each gene/distinct TSS in the RefGene.txt.gz file.
 *     only one transcriptmodel is stored per distinct transcription start site. The function also displays lists of valid and invalid
 *     gene symbols in the dialog</li>
 *     <li>Accept causes {@link #acceptGenes()} to be run, which creates a list of {@link GopherGene} objects - one for
 *     each valid symbol -- and passes this to the {@link Model}. It also causes the dialog to close</li>
 * </ol>
 * Therefore, if all goes well, the effect of this dialog is to pass a list of {@link GopherGene} objects to the model.
 * This list should then be used to create {@link gopher.model.viewpoint.ViewPoint} objects elsewhere in the code.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.2.2 (2017-11-19)
 */
public class EntrezGenePresenter implements Initializable {
    static Logger logger = Logger.getLogger(EntrezGenePresenter.class.getName());
    @FXML
    private WebView wview;
    /** A reference to the Model. We will use it to add genes information to the model.*/
    private Model model=null;
    /** This class parses {@link GopherGene} objects from the refGene.txt.gz file. */
    RefGeneParser parser=null;
    /** reference to the stage of the primary App. */
    private Stage stage = null;
    /** List of the uploaded symbols prior to validation, i.e., may contain invalid symbols. */
    private List<String> symbols=null;
    /** Signal to close the dialog. */
    private Consumer<Signal> signal;
    /** Flag as to whether the user has validated the uploaded gene symbols. */
    private boolean isvalidated=false;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        isvalidated=false;
    }

   public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }


    /**
     * setting the stage of this view
     * @param stage
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }


    public void setModel(Model mod){this.model=mod;}

    /** Closes the stage of this view. */
    private void closeStage() {
        if(stage!=null) {
            stage.close();
        }
    }

    /** Transfer the genes to the model.
     * Use the refGene.txt.gz data to validate the uploaded gene symbols.
     * @param e
     */
    @FXML public void validateGeneSymbols(ActionEvent e) {
        e.consume();
        String path = this.model.getRefGenePath();
        if (path==null) {
            logger.error("attempt to validate gene symbols before refGene.txt.gz file was downloaded");
            PopupFactory.displayError("Error retrieving refGene data","Download refGene.txt.gz file before proceeding.");
            return;
        }
        logger.info("About to parse refGene.txt.gz file to validate uploaded gene symbols. Path at "+ path);
        try {
            this.parser = new RefGeneParser(path);
            parser.checkGenes(this.symbols);
        } catch (Exception exc) {
            PopupFactory.displayException("Error while attempting to validate Gene symbols","Could not validate gene symbols",exc);
            return;
        }
        List<String>  validGeneSymbols = parser.getValidGeneSymbols();
        List<String> invalidGeneSymbols= parser.getInvalidGeneSymbols();
        int uniqueTSSpositions = parser.getTotalTSScount();
        int n_genes=parser.getTotalNumberOfRefGenes();
        int chosenGeneCount=parser.getNumberOfRefGenesChosenByUser();
        int uniqueChosenTSS=parser.getCountOfChosenTSS();
        String html = getValidatedGeneListHTML(validGeneSymbols, invalidGeneSymbols,n_genes, uniqueTSSpositions);
        this.model.setN_validGeneSymbols(validGeneSymbols.size());
        this.model.setUniqueTSScount(uniqueTSSpositions);
        this.model.setUniqueChosenTSScount(uniqueChosenTSS);
        this.model.setChosenGeneCount(chosenGeneCount);
        model.setTotalRefGeneCount(n_genes);
        setData(html);
        isvalidated=true;
    }




   /** Sets the text that will be shown in the HTML View. */
    public void setData(String html) {
        WebEngine engine = wview.getEngine();
        engine.loadContent(html);
    }

    @FXML
    public void uploadGenes(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            logger.error("Could not get name of file with gene symbols");
            return;
        } else {
            logger.info("Uploading target genes from "+file.getAbsolutePath());
            this.model.setTargetGenesPath(file.getAbsolutePath());
        }
        uploadGenesFromFile(file);
        e.consume();
    }

    /** This function is responsibble for extracting gene symbols from a file that is uploaded to VPV by the user.
     * It initializes the List {@link #symbols}, which may contain correct and/or incorrect symbols -- the validity
     * of the uploaded gene symbols will be checked in the next step.
     * @param file
     */
    public void uploadGenesFromFile(File file) {
        this.symbols = new ArrayList<>();
        try {
            BufferedReader br =new BufferedReader(new FileReader(file));
            String line =null;
            while ((line=br.readLine())!=null) {
                symbols.add(line.trim());
            }
        } catch (IOException err) {
            logger.error("I/O Error reading file with target genes");
            logger.error(err,err);
        }
        setData(getInitialGeneListHTML(symbols));
        logger.info(String.format("Uploaded a total of %d genes",this.symbols.size()));
        isvalidated=false;
    }



    private String getValidatedGeneListHTML(List<String> valid, List<String> invalid, int n_genes, int n_transcripts) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<style type=\"text/css\">\n" +
                " span.bold-red {\n" +
                "    color: red;\n" +
                "    font-weight: bold;\n" +
                "}\n" +
                 " span.blu {\n" +
                 "    color: #4e89a4;\n" +
                 "    font-weight: normal;\n" +
                "}\n" +
                "</style>");
        sb.append("<body><h3>Validated gene list</h3>");
        sb.append(String.format("<p>We parsed a total number of %d genes and found a total of %d associated transcripts.</p>",n_genes,n_transcripts));
        sb.append(String.format("<p>%d of the uploaded gene symbols were valid, and %d were invalid or could not be parsed.</p>",
                valid.size(),invalid.size()));
        if (invalid.size()>0) {
            sb.append("<p>Invalid genes:<br/>");
            for (String inv : invalid) {
                sb.append("<span class=\"bold-red\">"+ inv + "</span><br/>");
            }
            sb.append("</p>");
            sb.append("<p><i>Please correct the gene symbol in the input file before proceeding. ");
            sb.append("Otherwise, the invalid gene symbol will be ignored.</i></p>");
        }
        if (valid.size()==0) {
            sb.append("<p>Error: No valid genes found!</p>");
        } else {
            sb.append("<p>Valid genes:<br/>");
            for (String v : valid) {
                sb.append("<span class=\"blu\">"+ v + "</span><br/>");
            }
            sb.append("</p>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    /** @return list of all uploaded genes prior to validation. */
    private String getInitialGeneListHTML(List<String> genes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><h3>Uploaded gene list</h3>");
        sb.append(String.format("<p>Number of genes: %d.</p>",genes.size()));
        sb.append("<p>");
        for (String g:genes) {
            sb.append(g + "<br/>");
        }
        sb.append("</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /** This function closes the dialog for entering genes, and passes the GopherGene list to the model. */
    @FXML public void acceptGenes() {
        if (!isvalidated) {
            PopupFactory.displayError("Error","Please validate genes for accepting them!");
            return;
        }
        if (parser==null ) {
            PopupFactory.displayError("Error","Please validate genes for accepting them!");
            return;
        }
        this.model.setVPVGenes(this.parser.getVPVGeneList());
        this.model.setUniqueChosenTSScount(this.parser.getCountOfChosenTSS());
        signal.accept(Signal.DONE);
    }

}