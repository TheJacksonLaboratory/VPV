package vpvgui.io.model;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.junit.Test;
import vpvgui.gui.ErrorWindow;
import vpvgui.model.project.JannovarGeneGenerator;
import vpvgui.model.project.VPVGene;
import vpvgui.model.project.ViewPoint;

import java.io.File;
import java.util.*;

/**
 * Created by peterrobinson on 7/11/17.
 */
public class TestViewPointCreation {



    /* Der folgende Code kann benutzt werden, um Darios
    Probes zu entwerfen und ist eigentlich kein Text, aber das
    JUNit Framework bietet die Moeglichkeit, Code so starten zu koennen
    ohne den restlichen Code zu veraendern.
    Du muesstest bitte die Pfade, die unten mit ??? markiert sind,
    entsprechend anpassen
     */
    @Test public void createProbesTest() throws Exception {

        // fill list of gene symbols from file
        Integer maxNumOfSymbols=5;
        Integer numOfSymbols=0;
        ArrayList<String> symbols = new ArrayList<String>();
        String transcriptfile= "/home/peter/IdeaProjects/git_vpv_workspace/VPV/hg19_ucsc.ser";// Path to Jannovar file ucsc_hg37.ser etc
        Scanner s = new Scanner(new File("/home/peter/IdeaProjects/git_vpv_workspace/VPV/src/test/resources/genelistsample.txt"));
        //Scanner s = new Scanner(new File("/home/peter/IdeaProjects/git_vpv_workspace/VPV/src/test/resources/CaptureC_gonad_gene_list_edit2.txt"));
        while (s.hasNext()) {
            symbols.add(s.next());
            numOfSymbols++;
            if (numOfSymbols == maxNumOfSymbols) {
                break;
            }
        }
        s.close();

        /*
        // write list of gene symbols
        Iterator<String> symbolsIterator = symbols.iterator();
        while (symbolsIterator.hasNext()) {
            System.out.println(symbolsIterator.next());
        }
        */

        int maxDistToGenomicPosUp = 200;// ??? Man wuerde diesen Wert vom GUI bekommen
        int maxDistToGenomicPosDown=200; // ???
        Map<String,List<TranscriptModel>> validGenes2TranscriptsMap=null;
        List<VPVGene> vpvgenelist;

        if (transcriptfile==null) {
            ErrorWindow.display("Error retrieving Jannovar transcript file","Generate Jannovar transcript file before loading genes.");
            return;
        }

        JannovarGeneGenerator jgg = new JannovarGeneGenerator(transcriptfile);
        /* key is a gene symbol,and value is a listof corresponding transcripts. */
        validGenes2TranscriptsMap = jgg.checkGenes(symbols);
        List<String> validGeneSymbols = jgg.getValidGeneSymbols();
        List<String> invalidGeneSymbols= jgg.getInvalidGeneSymbols();
        int n_transcripts = getNTranscripts(validGenes2TranscriptsMap);


        vpvgenelist = new ArrayList<>();
        for (String symbol : validGenes2TranscriptsMap.keySet()) {
            List<TranscriptModel> transcriptList=validGenes2TranscriptsMap.get(symbol);
            TranscriptModel tm = transcriptList.get(0);
            String referenceSequenceID=getChromosomeString(tm.getChr());
            String id = tm.getGeneID();
            VPVGene vpvgene=new VPVGene(id,symbol);
            vpvgene.setChromosome(referenceSequenceID);
            if (tm.getStrand().isForward()) {
                vpvgene.setForwardStrand();
            } else {
                vpvgene.setReverseStrand();
            }
            for (TranscriptModel tmod: transcriptList) {
                GenomeInterval iv = tmod.getTXRegion();
                Integer pos=null;
                if (tm.getStrand().isForward()) {
                    pos = iv.getBeginPos();
                } else {
                    pos = iv.getEndPos();
                }
                ViewPoint vp = new ViewPoint(referenceSequenceID,pos,maxDistToGenomicPosUp,maxDistToGenomicPosDown);
                vpvgene.addViewPoint(vp);
            }
            vpvgenelist.add(vpvgene);

        }

    }

    /** TODO -- stimmt nicht fuer Maus */
    private String getChromosomeString(int c) {
        if (c>0 && c<23) {
            return String.format("chr%d",c);
        } else if (c==23) {
            return "chrX";
        } else if (c==24) {
            return "chrY";
        } else if (c==25) {
            return "chrM";
        } else {
            return "???(Could not parse chromosome)";
        }
    }

    private String getChromosomeStringMouse(int c) {
        if (c>0 && c<23) {
            return String.format("chr%d",c);
        } else if (c==10) {
            return "chrX";
        } else if (c==21) {
            return "chrY";
        } else if (c==22) {
            return "chrM";
        } else {
            return "???(Could not parse chromosome)";
        }
    }


    private int getNTranscripts( Map<String,List<TranscriptModel>> mp) {
        int n=0;
        for (String s : mp.keySet()) {
            List<TranscriptModel> lst = mp.get(s);
            n += lst.size();
        }
        return n;
    }


}
