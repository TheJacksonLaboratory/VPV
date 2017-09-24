package vpvgui.vpvmain;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import vpvgui.exception.DownloadFileNotFoundException;
import vpvgui.gui.ConfirmWindow;
import vpvgui.gui.ErrorWindow;
import vpvgui.gui.Popups;
import vpvgui.gui.analysisPane.VPAnalysisPresenter;
import vpvgui.gui.analysisPane.VPAnalysisView;
import vpvgui.gui.createviewpointpb.CreateViewpointPBPresenter;
import vpvgui.gui.createviewpointpb.CreateViewpointPBView;
import vpvgui.gui.entrezgenetable.EntrezGeneViewFactory;
import vpvgui.gui.enzymebox.EnzymeViewFactory;
import vpvgui.gui.help.HelpViewFactory;
import vpvgui.gui.logviewer.LogViewerFactory;
import vpvgui.gui.proxy.SetProxyPresenter;
import vpvgui.gui.proxy.SetProxyView;
import vpvgui.gui.settings.SettingsViewFactory;
import vpvgui.io.*;
import vpvgui.model.*;
import vpvgui.model.viewpoint.SimpleViewPointCreationTask;
import vpvgui.model.viewpoint.ViewPoint;
import vpvgui.model.viewpoint.ExtendedViewPointCreationTask;
import vpvgui.model.viewpoint.ViewPointCreationTask;
import vpvgui.util.SerializationManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static vpvgui.io.Platform.getVPVDir;


/**
 * A Java app to help design probes for Capture Hi-C
 * @author Peter Robinson
 * @author Peter Hansen
 * @version 0.0.5 (2017-07-23)
 */
public class VPVMainPresenter implements Initializable {
    static Logger logger = Logger.getLogger(VPVMainPresenter.class.getName());
    /** The Model for the entire analysis. */
    private Model model = null;
    /** Convenience class that knows about the required order of operations and dependencies.*/
    private Initializer initializer=null;

    private Stage primaryStage;
    /**
     * This is the root node of the GUI and refers to the BorderPane. It can be used to
     * obtain a reference to the primary scene, which is needed by FileChooser, etc.
     */
    @FXML
    private Node rootNode;

    /** List of genome builds. Used by genomeChoiceBox*/
    @FXML
    private ObservableList<String> genomeTranscriptomeList = FXCollections.observableArrayList("hg19", "hg38", "mm9","mm10");
    /** List of genome builds. Used by genomeChoiceBox*/
    @FXML
    private ObservableList<String> approachList = FXCollections.observableArrayList("Simple", "Extended");
    @FXML
    private ChoiceBox<String> genomeChoiceBox;
    @FXML
    private ChoiceBox<String> approachChoiceBox;
    /** Clicking this button downloads the genome build and unpacks it.*/
    @FXML
    private Button downloadGenome;
    @FXML
    private Button decompressGenomeButton;
    @FXML
    private Button indexGenomeButton;
    /** Label for the genome build we want to download. */
    @FXML
    private Label genomeBuildLabel;
    /** Label for the transcripts we want to download.*/
    @FXML
    private Label transcriptsLabel;
    /** Show which design approach */
    @FXML private Label approachLabel;

    /**
     * Clicking this button will download some combination of genome and (compatible) gene definition files.
     * A file chooser will appear and the user can decide where to download everything. The paths will be stored
     * in the viewpoint settings file.
     */
    @FXML private Button downloadGenomeButton;
    /** Show progress in downloading the Genome and corresponding transcript definition file.  */
    /** Button to download RefSeq.tar.gz (transcript/gene definition file  */
    @FXML private Button downloadTranscriptsButton;
    @FXML private ProgressIndicator genomeDownloadPI;
    /** Show progress in downloading the Genome and corresponding transcript definition file.  */
    @FXML private ProgressIndicator genomeDecompressPI;
    @FXML private ProgressIndicator genomeIndexPI;
    /** Progress indicator for downloading the transcript file */
    @FXML private ProgressIndicator transcriptDownloadPI;



    @FXML private TextField fragNumUpTextField;
    @FXML private TextField fragNumDownTextField;
    @FXML private TextField minSizeUpTextField;
    @FXML private TextField maxSizeUpTextField;
    @FXML private TextField minSizeDownTextField;
    @FXML private TextField maxSizeDownTextField;
    @FXML private TextField minFragSizeTextField;
    @FXML private TextField maxRepContentTextField;
    /** Show which enzymes the user has chosen. */
    @FXML private Label restrictionEnzymeLabel;
    /** Show how many valid genes were uploaded by user. */
    @FXML private Label nValidGenesLabel;
    /** Show the name of the downloaded genome we areusing. */
    @FXML private Label downloadedGenomeLabel;
    /** Show status of unpacking the downloaded genome. */
    @FXML private Label decompressGenomeLabel;
    /** Show status of indexing the downloaded genome. */
    @FXML private Label indexGenomeLabel;
    /** Show name of downloaded transcripts file. */
    @FXML private Label downloadedTranscriptsLabel;
    @FXML RadioMenuItem tiling1;
    @FXML RadioMenuItem tiling2;
    @FXML RadioMenuItem tiling3;
    @FXML RadioMenuItem tiling4;
    @FXML RadioMenuItem tiling5;

    @FXML
    private Button showButton;

    @FXML
    private Button exitButton;

    @FXML
    private Button enterGeneListButton;

    @FXML
    Button createCaptureProbesButton;

    @FXML
    private Label textLabel;

    @FXML
    private TabPane tabpane;

    @FXML
    private AnchorPane analysisPane;

    @FXML
    private Tab setuptab;

    @FXML
    MenuItem showSettingsCurrentProject;

    @FXML MenuItem helpMenuItem;
    @FXML MenuItem openHumanGeneWindow;
    @FXML MenuItem openMouseGeneWindow;
    @FXML MenuItem openRatGeneWindow;
    @FXML MenuItem openFlyGeneWindow;
    @FXML MenuItem exportBEDFilesMenuItem;


    @FXML
    private Tab analysistab;

    /** Click this to choose the restriction enzymes with which to do the capture Hi-C cutting  */
    @FXML private Button chooseEnzymeButton;

    /** Presenter for the second tab. */
    private VPAnalysisPresenter vpanalysispresenter;
    /** View for the second tab. */
    private VPAnalysisView vpanalysisview;

    @FXML
    void exitButtonClicked(ActionEvent e) {
        e.consume();
        logger.info("Closing VPV Gui");
        serialize();
        //Model.writeSettingsToFile(this.model);
        //closeWindow(e);
        javafx.application.Platform.exit();
    }


    public boolean serialize() {
        String projectname=this.model.getProjectName();
        if (projectname==null) {
            ErrorWindow.display("Error","Could not get viewpoint name (should never happen). Will save with default");
            projectname="default";
        }
        File dir=getVPVDir();
        String serializedFilePath=Platform.getAbsoluteProjectPath(projectname);
        try {
            SerializationManager.serializeModel(this.model, serializedFilePath);
        } catch (IOException e) {
            ErrorWindow.displayException("Error","Unable to serialize VPV viewpoint",e);
            return false;
        }
        logger.trace("Serialization successful to file "+serializedFilePath);
        return true;
    }





    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.trace("initialize() called");
        genomeChoiceBox.setItems(genomeTranscriptomeList);
        genomeChoiceBox.getSelectionModel().selectFirst();
        genomeChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            resetGenomeBuild(newValue);
        });
        approachChoiceBox.setItems(approachList);
        approachChoiceBox.getSelectionModel().selectFirst();
        approachChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.approachLabel.setText(newValue);
        });
        this.approachLabel.setText(approachChoiceBox.getValue());
        initializeBindings();
        initializePromptTextsToDefaultValues();
        this.initializer=new Initializer(model);

        this.vpanalysisview = new VPAnalysisView();
        this.vpanalysispresenter = (VPAnalysisPresenter) this.vpanalysisview.getPresenter();
        this.vpanalysispresenter.setTabPaneRef(this.tabpane);
        this.analysisPane.getChildren().add(vpanalysisview.getView());
        ToggleGroup tGroup = new ToggleGroup();
        tGroup.getToggles().addAll(tiling1,tiling2,tiling3,tiling4,tiling5);
        tiling1.setOnAction(e->{this.model.setTilingFactor(1);e.consume(); });
        tiling2.setOnAction(e->{this.model.setTilingFactor(2);e.consume(); });
        tiling3.setOnAction(e->{this.model.setTilingFactor(3);e.consume(); });
        tiling4.setOnAction(e->{this.model.setTilingFactor(4);e.consume(); });
        tiling5.setOnAction(e->{this.model.setTilingFactor(5);e.consume(); });
    }

    private void setInitializedValuesInGUI() {
        String genomebuild=model.getGenomeBuild();
        if (genomebuild!=null)
            this.genomeBuildLabel.setText(genomebuild);
        String path_to_downloaded_genome_directory=model.getGenomeDirectoryPath();
        if (path_to_downloaded_genome_directory!= null) {
            this.downloadedGenomeLabel.setText(path_to_downloaded_genome_directory);
            this.genomeDownloadPI.setProgress(1.00);
        }
        String refGenePath=this.model.getRefGenePath();
        if (refGenePath!=null) {
            this.downloadedTranscriptsLabel.setText(refGenePath);
            this.transcriptDownloadPI.setProgress(1.0);
        }
    }

    /**
     * This allows a caller to set the {@link Model} object for this presenter (for instance, a default
     * {@link Model} object is set if the user chooses a new viewpoint. If the user chooses to open a previous
     * viewpoint from a serialized file, then a  {@link Model} object is initialized from the file and set here.
     * This method calls {@link #setInitializedValuesInGUI()} in order to show relevant data in the GUI.
     * @param mod A {@link Model} object.
     */
    public void setModel(Model mod) {
        this.model=mod;
        logger.trace(String.format("Setting model to %s",mod.toString()));
        setInitializedValuesInGUI();
    }


    public void setModelInMainAndInAnalysisPresenter(Model mod) {
        setModel(mod);
        this.vpanalysispresenter.setModel(mod);
    }

    public void initStage(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Initialize the bindings to Java bean properties in the model with
     * the GUI elements.
     */
    private void initializeBindings() {
       // genomeChoiceBox.valueProperty().bindBidirectional(model.genomeBuildProperty());
        /*this.fragNumUpTextField.textProperty().bindBidirectional(model.fragNumUpProperty(),new NumberStringConverter());
        this.fragNumDownTextField.textProperty().bindBidirectional(model.fragNumDownProperty(),new NumberStringConverter());
        this.minSizeUpTextField.textProperty().bindBidirectional(model.minSizeUpProperty(),new NumberStringConverter());
        this.maxSizeUpTextField.textProperty().bindBidirectional(model.getMaxSizeUp(),new NumberStringConverter());
        this.minSizeDownTextField.textProperty().bindBidirectional(model.minSizeDownProperty(),new NumberStringConverter());
        this.maxSizeDownTextField.textProperty().bindBidirectional(model.maxSizeDownProperty(),new NumberStringConverter());
        this.minFragSizeTextField.textProperty().bindBidirectional(model.minFragSizeProperty(),new NumberStringConverter());
        this.maxRepContentTextField.textProperty().bindBidirectional(model.maxRepeatContentProperty(),new NumberStringConverter());
        */
    }

    /** The prompt (gray) values of the text fields in the settings windows get set to their default values here. */
    private void initializePromptTextsToDefaultValues() {
        this.fragNumUpTextField.setPromptText(String.format("%d",Default.NUMBER_OF_FRAGMENTS_UPSTREAM));
        this.fragNumDownTextField.setPromptText(String.format("%d",Default.NUMBER_OF_FRAGMENTS_DOWNSTREAM));
        this.minSizeUpTextField.setPromptText(String.format("%d",Default.MINIMUM_SIZE_UPSTREAM));
        this.minSizeDownTextField.setPromptText(String.format("%d",Default.MINIMUM_SIZE_DOWNSTREAM));
        this.maxSizeUpTextField.setPromptText(String.format("%d",Default.MAXIMUM_SIZE_UPSTREAM));
        this.maxSizeDownTextField.setPromptText(String.format("%d",Default.MAXIMUM_SIZE_DOWNSTREAM));
        this.minFragSizeTextField.setPromptText(String.format("%d",Default.MINIMUM_FRAGMENT_SIZE));
        this.maxRepContentTextField.setPromptText(String.format("%.1f",Default.MAXIMUM_REPEAT_CONTENT));
    }


    /** This gets called when the user chooses a new genome build. They need to do download, uncompression, indexing and
     * also get the corresponding transcript file.
     * @param build Name of genome build.
     */
    private void resetGenomeBuild(String build) {
        logger.info("Setting genome build to "+build);
        this.genomeBuildLabel.setText(build);
        this.model.setGenomeBuild(build);
        this.transcriptDownloadPI.setProgress(0.0);
        this.genomeDownloadPI.setProgress(0.0);
        this.genomeIndexPI.setProgress(0.0);
        this.genomeDecompressPI.setProgress(0.0);
    }

    public Model getModel() { return this.model; }

    /** This downloads the tar-gzip genome file as chosen by the user from the UCSC download site.
     * It places the compressed file into the directory chosen by the user. The path to the directory
     * is stored in the {@link Model} object using the {@link Model#setGenomeDirectoryPath} function.
     * Following this the user needs to uncompress and index the genome files using the function
     * {@link #decompressGenome(ActionEvent)} which is called after the corresponding button
     * is clicked in the GUI.
     */
    @FXML public void downloadGenome() {
        String build = this.model.getGenomeBuild();
        logger.info("About to download genome for "+build +" (if necessary)");
        GenomeDownloader gdownloader = new GenomeDownloader(build);
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for " + build + " (will be downloaded if not found).");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath()=="") {
            logger.error("Could not set genome download path from Directory Chooser");
            ErrorWindow.display("Error","Could not get path to download genome.");
            return;
        }
        logger.info("Got back as download dir "+file.getAbsolutePath());
        gdownloader.setDownloadDirectoryAndDownloadIfNeeded(file.getAbsolutePath(),model.getGenomeBasename(),genomeDownloadPI);
        model.setGenomeDirectoryPath(file.getAbsolutePath());
        this.downloadedGenomeLabel.setText(file.getAbsolutePath());
        this.genomeDownloadPI.setProgress(1.0);


    }

    /**
     * @param e event triggeredby command to download appropriate RefGene.txt.gz file.
     */
   @FXML public void downloadRefGeneTranscripts(ActionEvent e) {
        String genome = this.model.getGenomeURL();
        if (genome==null)
            genome=genomeChoiceBox.getValue();
        RefGeneDownloader rsd = new RefGeneDownloader(genome);
        String transcriptName = rsd.getTranscriptName();
        String basename=rsd.getBaseName();
        String url=null;
        try {
            url = rsd.getURL();
        } catch (DownloadFileNotFoundException dfne) {
            ErrorWindow.display("Could not identify RefGene file for genome",dfne.getMessage());
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for " + genome + " (will be downloaded if not found).");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath()=="") {
            ErrorWindow.display("Error","Could not get path to download transcript file.");
            return;
        }
        Operation op = new RefGeneDownloadOperation(file.getPath());
        Downloader downloadTask = new Downloader(file, url, basename, transcriptDownloadPI);
        // TODO make this setOnSucceeded and then set model and GUI.
        if (downloadTask.needToDownload(op)) {
            Thread th = new Thread(downloadTask);
            th.setDaemon(true);
            th.start();
        } else {
            this.transcriptDownloadPI.setProgress(1.0);
        }
        /* ToDo-put this in setOnsucceeded. */
       String abspath=(new File(file.getAbsolutePath() + File.separator + basename)).getAbsolutePath();
       this.model.setRefGenePath(abspath);
       this.downloadedTranscriptsLabel.setText(transcriptName);
       e.consume();
    }


    /** G-unzip and un-tar the downloaded chromFa.tar.gz file.
     * @param e  Event triggered by decompress genome command
     */
    @FXML public void decompressGenome(ActionEvent e) {
        e.consume();
        GenomeGunZipper gindexer = new GenomeGunZipper(this.model.getGenomeDirectoryPath(),
                this.genomeDecompressPI);
        gindexer.setOnSucceeded( event -> {
            decompressGenomeLabel.setText(gindexer.getStatus());
            if (gindexer.OK())
                model.setGenomeUnpacked();
        });
        Thread th = new Thread(gindexer);
        th.setDaemon(true);
        th.start();
    }

    /** Create fai (fasta index files)
     * @param e Event triggered by index genome command.
     * */
    @FXML public void indexGenome(ActionEvent e) {
        e.consume();
        logger.trace("Indexing genome files...");
        FASTAIndexManager manager = new FASTAIndexManager(this.model.getGenomeDirectoryPath(),this.genomeIndexPI);
        manager.setOnSucceeded(event ->{
            indexGenomeLabel.setText("FASTA files successfully indexed.");
            logger.debug("Number of FASTA files retrieved> "+manager.getIndexedFastaFiles().size());
            model.setIndexedFastaFiles(manager.getIndexedFastaFiles());
            model.setGenomeIndexed();
            model.setContigLengths(manager.getContigLengths());
        } );
        Thread th = new Thread(manager);
        th.setDaemon(true);
        th.start();
    }



    /** This function is called after the user has chosen restriction enzymes in the
     * corresponding popup window. It passes a list of the {@link RestrictionEnzyme}
     * objects to the {@link Model}.*/
    @FXML public void chooseEnzymes() {
        List<RestrictionEnzyme> enzymes = this.model.getRestrictionEnymes();
        List<RestrictionEnzyme> chosenEnzymes = EnzymeViewFactory.getChosenEnzymes(enzymes);
        this.model.setChosenRestrictionEnzymes(chosenEnzymes);
        this.restrictionEnzymeLabel.setText(this.model.getRestrictionEnzymeString());
    }

    /**
     * Open a new dialog where the user can paste gene symbols or Entrez Gene IDs.
     * The effect of the command <pre>EntrezGeneViewFactory.display(this.model);</pre>
     * is to pass a list of {@link VPVGene} objects to the {@link Model}.
     * These objects are used with other information in the Model to create {@link vpvgui.model.viewpoint.ViewPoint}
     * objects when the user clicks on {@code Create ViewPoints}.
     * See {@link EntrezGeneViewFactory} for logic.
     *
     * @param e event triggered by enter gene command.
     */
    public void enterGeneList(ActionEvent e) {
        EntrezGeneViewFactory.display(this.model);
        /** The following command is just for debugging. We now have all VPVGenes, but still need to
         * add information about the restriction enzymes and the indexed FASTA file.
         */
        //this.model.debugPrintVPVGenes();
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",this.model.n_valid_genes(),this.model.n_viewpointStarts()));
        e.consume();
    }


    private boolean checkDataValidity() {
        if (this.model.getVPVGeneList()==null|| this.model.getVPVGeneList().size()==0) {
            ErrorWindow.display("Error","Viewpoints cannot be created before a gene list has been chosen."+
                    "\nPlease enter a gene list and try again.");
            return false;
        }
        if (this.model.getChosenEnzymelist()==null || this.model.getChosenEnzymelist().isEmpty()) {
            ErrorWindow.display("Error","Viewpoints cannot be created before a restriction enzyme has been chosen."+
                    "\nPlease choose a restriction enzyme and try again.");
            return false;
        }
        //TODO check other parameters.
        return true;
    }




    /**
     * When the user clicks this button, they should have uploaded and validated a list of gene symbols;
     * these will have been entered as {@link VPVGene} objects into the {@link Model}
     * object. This function will use the {@link VPVGene} obejcts and other information
     * to create {@link vpvgui.model.viewpoint.ViewPoint} objects that will then be displayed in the
     * {@link VPAnalysisPresenter} Tab.
     */
    public void createViewPoints() {
        logger.trace("Entering createViewPoints");
        if (! checkDataValidity()) {
            return;
        }

        String approach = this.approachChoiceBox.getValue();
        boolean doSimple=false;
        if (approach.equals("Simple")) {
            doSimple=true;
        } else if (approach.equals("Extended")) {
            doSimple=false;
        } else {
            logger.error("Could not retrieve approach, I got "+approach);
            return;
        }


        StringProperty sp=new SimpleStringProperty();
        ViewPointCreationTask task =null;
        if (doSimple) {
            task = new SimpleViewPointCreationTask(model,sp);
        } else {
            task = new ExtendedViewPointCreationTask(model,sp);
        }

        CreateViewpointPBView pbview = new CreateViewpointPBView();
        CreateViewpointPBPresenter pbpresent = (CreateViewpointPBPresenter)pbview.getPresenter();
        pbpresent.initBindings(task,sp);

        Stage window;
        String windowTitle = "Viewpoint creation";
        window = new Stage();
        window.setOnCloseRequest( event -> {
            window.close();
        } );
        window.setTitle(windowTitle);
        pbpresent.setSignal(signal -> {
            switch (signal) {
                case DONE:
                    window.close();
                    break;
                case CANCEL:
                case FAILED:
                    throw new IllegalArgumentException(String.format("Illegal signal %s received.", signal));
            }

        });

        task.setOnSucceeded(event -> {
            SingleSelectionModel<Tab> selectionModel = tabpane.getSelectionModel();
            this.vpanalysispresenter.setModel(this.model);
            this.vpanalysispresenter.showVPTable();
            selectionModel.select(this.analysistab);
            logger.trace("Finished factory.createViewPoints()");
            pbpresent.closeWindow();
        });
        logger.trace("About to run task");
        new Thread(task).start();
        window.setScene(new Scene(pbview.getView()));
        window.showAndWait();
    }

    /**
     * @param e Event triggered by close command.
     */
    public void closeWindow(ActionEvent e) {
        boolean answer = ConfirmWindow.display("Alert", "Are you sure you want to quit?");
        if (answer) {
            logger.info("Closing VPV Gui");
            serialize();
        }
        javafx.application.Platform.exit();
    }

    public void refreshViewPoints() {
        if (this.vpanalysispresenter==null) {
            logger.error("Could not refresh viewpoint table, since vpanalysispresenter was null");
            return;
        }
        this.vpanalysispresenter.refreshVPTable();
    }

    /**
     * This is called when the user starts a new viewpoint. It should erase everything from
     * the GUI as well (TODO check this!)
     * @param e Event triggered by new viewpoint command.
     */
    @FXML public void startNewProject(ActionEvent e) {
        logger.trace("Start new viewpoint");
        ObservableList<Tab> panes = this.tabpane.getTabs();
        /* collect tabs first then remove them -- avoids a ConcurrentModificationException */
        List<Tab> tabsToBeRemoved=new ArrayList<>();
        /* close all tabs except setup and analysis. */
        for (Tab tab : panes) {
            String id=tab.getId();
            if (id != null && (id.equals("analysistab") || id.equals("setuptab") )) { continue; }
            logger.trace("Closing tab "+id);
            tabsToBeRemoved.add(tab);
        }
        this.tabpane.getTabs().removeAll(tabsToBeRemoved);
        this.model=new Model();
        this.model.setDefaultValues();
        this.vpanalysisview = new VPAnalysisView();
        this.vpanalysispresenter = (VPAnalysisPresenter) this.vpanalysisview.getPresenter();
        this.vpanalysispresenter.setModel(this.model);
        this.vpanalysispresenter.setTabPaneRef(this.tabpane);
        setInitializedValuesInGUI();
        e.consume();
        /* TODO */
        logger.trace("TODO -- also re-initialize first tab");
    }

    /** Display the settings (parameters) of the current viewpoint. */
    public void showSettingsOfCurrentProject() {
        SettingsViewFactory.showSettings(model.getProperties());
    }

    /**
     * Content of {@link Model} is written to platform-dependent default location.
     *  @throws IOException caused by an error in serialization
     */
    @FXML private void saveProject() throws IOException {
       // Model.writeSettingsToFile(this.model);
        boolean result=serialize();
        if (result) { /* if it didnt work, the serialize method will show an error dialog. */
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(String.format("Successfully saved viewpoint data to %s", Platform.getAbsoluteProjectPath(model.getProjectName())));
            alert.show();
        }
    }

    /**
     * @throws IOException caused by an error in serialization
     */
    @FXML private void saveProjectAndClose() throws IOException {
        serialize();
        javafx.application.Platform.exit();
    }


    /**
     * @param e event triggered by show help command.
     */
    @FXML public void showHelpWindow(ActionEvent e) {
        HelpViewFactory.display();
        e.consume();
    }

    /**
     * @param e event triggered by set proxy command.
     */
    @FXML void setProxyDialog(ActionEvent e) {
        Stage window;
        String windowTitle = "VPV Settings";
        window = new Stage();
        window.setOnCloseRequest( event -> {window.close();} );
        window.setTitle(windowTitle);

        SetProxyView view = new SetProxyView();
        SetProxyPresenter presenter = (SetProxyPresenter) view.getPresenter();
        presenter.setSignal(signal -> {
            switch (signal) {
                case DONE:
                    window.close();
                    break;
                case CANCEL:
                case FAILED:
                    throw new IllegalArgumentException(String.format("Illegal signal %s received.", signal));
            }
        });
        if (model.getHttpProxy()!=null) {
            presenter.setProxyProperty(model.getHttpProxy());
        }
        if (model.getHttpProxyPort()!=null) {
            logger.trace(String.format("http proxy prot get: %s",model.getHttpProxyPort()));
            presenter.setPort(model.getHttpProxyPort());
        }
        window.setScene(new Scene(view.getView()));
        window.showAndWait();
        String port=presenter.getPort();
        String proxy=presenter.getProxy();
        if (proxy==null) {
            ErrorWindow.display("Error obtaining Proxy","Proxy string could not be obtained. Please try again");
            return;
        }
        this.model.setHttpProxy(proxy);
        this.model.setHttpProxyPort(port);
        logger.info("Set proxy to "+proxy);
        logger.info("Set proxy port to "+port);
        System.setProperty("http.proxyHost",proxy);
        System.setProperty("http.proxyPort",port);
        System.setProperty("https.proxyHost",proxy);
        System.setProperty("https.proxyPort",port);
    }


    @FXML public void openGeneWindowWithExampleHumanGenes() {
        File file = new File(getClass().getClassLoader().getResource("humangenesymbols.txt").getFile());
        if (file==null) {
            ErrorWindow.display("Could not open example human gene list","Please report to developers");
            return;
        }
        EntrezGeneViewFactory.displayFromFile(this.model,file);
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",this.model.n_valid_genes(),this.model.n_viewpointStarts()));
    }
    @FXML public void openGeneWindowWithExampleFlyGenes() {
        File file = new File(getClass().getClassLoader().getResource("flygenesymbols.txt").getFile());
        if (file==null) {
            ErrorWindow.display("Could not open example fly gene list","Please report to developers");
            return;
        }
        EntrezGeneViewFactory.displayFromFile(this.model,file);
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",this.model.n_valid_genes(),this.model.n_viewpointStarts()));
    }
    @FXML public void openGeneWindowWithExampleMouseGenes() {
        File file = new File(getClass().getClassLoader().getResource("mousegenesymbols.txt").getFile());
        if (file==null) {
            ErrorWindow.display("Could not open example mouse gene list","Please report to developers");
            return;
        }
        EntrezGeneViewFactory.displayFromFile(this.model,file);
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",this.model.n_valid_genes(),this.model.n_viewpointStarts()));
    }
    @FXML public void openGeneWindowWithExampleRatGenes() {
        File file = new File(getClass().getClassLoader().getResource("ratgenesymbols.txt").getFile());
        if (file==null) {
            ErrorWindow.display("Could not open example rat gene list","Please report to developers");
            return;
        }
        EntrezGeneViewFactory.displayFromFile(this.model,file);
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",this.model.n_valid_genes(),this.model.n_viewpointStarts()));
    }

    @FXML public void exportBEDFiles(ActionEvent e) {
        List<ViewPoint> vplist=this.model.getViewPointList();
        if (vplist==null || vplist.isEmpty()) {
            ErrorWindow.display("Attempt to export empty BED files","Complete generation and analysis of ViewPoints before exporting to BED!");
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for exporting BED files.");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath()=="") {
            ErrorWindow.display("Error","Could not get path to export BED files.");
            return;
        }
        String prefix="testprefix";
        BEDFileExporter exporter = new BEDFileExporter(file.getAbsolutePath(),prefix);
        try {
            exporter.printRestFragsToBed(this.model.getViewPointList(),this.model.getGenomeBuild());
        } catch (Exception exc) {
            ErrorWindow.displayException("Could not save data to BED files", exc.getMessage(),exc);
        }
        e.consume();
    }

    @FXML
    public void setProbeLength(ActionEvent e) {
        Integer len = Popups.getIntegerFromUser("Enter Probe Length",
                Default.PROBE_LENGTH,
                "Enter probe length:");
        if (len == null) {
            ErrorWindow.display("Could not get probe length", "enter an integer value!");
            return;
        }
        this.model.setProbeLength(len);
        logger.trace(String.format("We just set probe length to %d", model.getProbeLength()));
    }

    @FXML
    public void setMaximumAllowedRepeatOverlap(ActionEvent e) {
        Integer len = Popups.getIntegerFromUser("Enter Maximum allowed repeat overlap",
                Default.MAXIMUM_ALLOWED_REPEAT_OVERLAP,
                "Maximum allowed repeat overlap:");
        if (len == null) {
            ErrorWindow.display("Could not get Maximum allowed repeat length", "enter an integer value!");
            return;
        }
        this.model.setMaximumAllowedRepeatOverlap(len);
        logger.trace(String.format("We just set MaximumAllowedRepeatOverlap to %d", model.getMaximumAllowedRepeatOverlap()));
    }

    @FXML
    public void setMarginSize(ActionEvent e) {
        Integer msize = Popups.getIntegerFromUser("Enter margin size",
                Default.MARGIN_SIZE,
                "Margin size for calculating repeat content");
        if (msize==null) {
            ErrorWindow.display("Could not get Margin size", "enter an integer value!");
            return;
        }
        this.model.setMarginSize(msize);
        logger.trace(String.format("We just set MarginSize to %d", model.getMarginSize()));
    }

    @FXML
    public void showLog() {
        LogViewerFactory factory = new LogViewerFactory();
        factory.display();
    }

    @FXML
    public void about(ActionEvent e) {
        Popups.showAbout(model.getVersion(), model.getLastChangeDate());
        e.consume();
    }


}





