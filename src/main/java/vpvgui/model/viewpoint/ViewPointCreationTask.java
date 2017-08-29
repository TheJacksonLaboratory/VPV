package vpvgui.model.viewpoint;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import vpvgui.model.Model;
import vpvgui.model.RestrictionEnzyme;
import vpvgui.model.VPVGene;

import javax.swing.text.View;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by peterrobinson on 7/22/17.
 */
public class ViewPointCreationTask extends Task {
    private static final Logger logger = Logger.getLogger(ViewPointCreationTask.class.getName());
    Model model=null;

    /* List of VPVGenes representing User's gene list. */
    List<VPVGene> vpvGeneList;

    List<ViewPoint> viewpointlist=null;

    /** Restriction enzyme cuttings patterns (must have at least one) */
    private  String[] cuttingPatterns;
    /** Maximum distance from central position (e.g., transcription start site) of the upstream boundary of the viewpoint.*/
    private  int maxDistanceUp;
    /** Maximum distance from central position (e.g., transcription start site) of the downstream boundary of the viewpoint.*/
    private  int maxDistanceDown;

    private int minDistToGenomicPosDown;


    /* declare viewpoint parameters as requested by Darío */

    private  Integer fragNumUp;
    private  Integer fragNumDown;
    //private  String cuttingMotif;
    private  Integer minSizeUp;

    private StringProperty currentVP=null;
    /** List of one or more restriction enzymes choseon by the user. */
    private List<RestrictionEnzyme> chosenEnzymes=null;


    private  Integer minFragSize;
    private  double maxRepContent;

    private  Integer marginSize=200; /* ToDo -- allow this to be set via the menu */

    public ViewPointCreationTask(Model model, StringProperty currentVPproperty){
        this.model=model;
        this.viewpointlist=new ArrayList<>();
        this.currentVP=currentVPproperty;
         /* We use the same enzymes for all ViewPoints; therefore, ViewPoint.chosenEnzymes is a static variable */
        ViewPoint.setChosenEnzymes(model.getChosenEnzymelist());
         /* Set up the static map of restriction enyzmes in the CuttingPositionMap class -- we are using the same
        enzymes for all viewpoints, so we can use a static variable.
         */
        CuttingPositionMap.restrictionEnzymeMap = new HashMap<>();
        List<RestrictionEnzyme> chosen = model.getChosenEnzymelist();
        if (chosen==null) {
            logger.error("Unable to retrieve list of chosen restriction enzymes");
            return;
        }
        for (RestrictionEnzyme re : chosen) {
            String site = re.getSite();
            site=site.replaceAll("^","");
            CuttingPositionMap.restrictionEnzymeMap.put(site,re);
        }
        init_parameters();
    }



    private void init_parameters() {
        this.vpvGeneList=model.getVPVGeneList();
        this.fragNumUp=model.getFragNumUp();
        this.fragNumDown=model.fragNumDown();
        this.minSizeUp=model.getMinSizeUp();
        this.minDistToGenomicPosDown=model.getMinSizeDown();
        this.maxDistanceUp =model.getMaxSizeUp();
        this.maxDistanceDown =model.getMaxSizeDown();
        this.minFragSize=model.getMinFragSize();
        this.maxRepContent=model.getMaxRepeatContent();
        //this.cuttingPatterns=model.getCuttingPatterns();
        //TODO Get the cuttings patterns from GUI!
        this.cuttingPatterns=  new String[]{"GATC"};


    }


    /** Get the total number of viewpoints we will create.This is needed in order
     * to get the progress indicator to be accurate.
     * @return
     */
    private int getTotalViewpoints() {
        int n=0;
        for (VPVGene vpvgene:this.vpvGeneList) {
            n += vpvgene.n_viewpointstarts();
        }
        return n;
    }

    /** This is the method that will create the viewpoints.
     * We have placed it in a task because it takes a while.
     * @return
     * @throws Exception
     */
    protected Object call() throws Exception {
        String cuttingMotif=this.cuttingPatterns[0];/* TODO -- Why do we need this instead of taking cutting patterns? */
        logger.trace("Creating viewpoints for cuting pattern: "+cuttingMotif);

        int total=getTotalViewpoints();
        int i=0;

        int maxSizeUp=1500;
        int maxSizeDown=1500;

        for (VPVGene vpvgene:this.vpvGeneList) {
            String referenceSequenceID = vpvgene.getContigID();/* Usually a chromosome */
            //logger.trace("Retrieving indexed fasta file for contig: "+referenceSequenceID);
            String path=this.model.getIndexFastaFilePath(referenceSequenceID);
            if (path==null) {
                logger.error("Could not retrieve faidx file for "+referenceSequenceID);
                continue;
            }
            try {
                IndexedFastaSequenceFile fastaReader = new IndexedFastaSequenceFile(new File(path));
                List<Integer> gPosList = vpvgene.getTSSlist();
                for (Integer gPos : gPosList) {
                    ViewPoint vp = new ViewPoint.Builder(referenceSequenceID,gPos).
                            targetName(vpvgene.getGeneSymbol()).
                            maxDistToGenomicPosUp(maxDistanceUp).
                            maxDistToGenomicPosDown(maxDistanceDown).
                            cuttingPatterns(this.cuttingPatterns).
                            fastaReader(fastaReader).
                            minimumSizeUp(minSizeUp).
                            maximumSizeUp(maxDistanceUp).
                            minimumSizeDown(minDistToGenomicPosDown).
                            maximumSizeDown(maxDistanceDown).
                            minimumFragmentSize(minFragSize).
                            maximumRepeatContent(maxRepContent).
                            marginSize(marginSize).
                            build();
                    updateProgress(i++,total); /* this will update the progress bar */
                    updateLabelText(this.currentVP,vpvgene.toString());
                    vp.generateViewpointLupianez(fragNumUp, fragNumDown, cuttingMotif,maxSizeUp,maxSizeDown);
                    viewpointlist.add(vp);
                    logger.trace(String.format("Adding viewpoint %s to list (size: %d)",vp.getTargetName(),viewpointlist.size()));
                }
            } catch (FileNotFoundException e) {
                logger.error("[ERROR] could not open/find faidx file for "+referenceSequenceID);
                logger.error(e,e);
                // just skip this TODO -- better error handling
            }
        }
        this.model.setViewPoints(viewpointlist);
        return true;
    }


    private void updateLabelText(StringProperty sb,String msg) {
        Platform.runLater(new Runnable(){
            @Override
            public void run() {
                sb.setValue(String.format("Creating view point for %s",msg));
            }
        });
    }



}
