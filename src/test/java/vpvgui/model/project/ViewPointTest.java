package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.tools.ant.util.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

import static org.junit.Assert.*;



/**
 * This test class tests an instances of the <i>ViewPoint</i> class. The settings for the call of the constructor are specified at the head of this test class.
 * <p>
 * @author Peter Hansen
 */
public class ViewPointTest {

    /* Create data and ViewPoint object for testing */

    private String testReferenceSequenceID="chr4_ctg9_hap1";
    private Integer testGenomicPos=10000;
    private Integer testMaxUpstreamGenomicPos=100; // replace initial radius with two separate max values for up and downstream. This is more flexible.
    private Integer testMaxDownstreamPos=100;
    private Integer testStartPos=testGenomicPos-testMaxUpstreamGenomicPos;
    private Integer testEndPos=testGenomicPos+testMaxDownstreamPos;
    private String testDerivationApproach = "INITIAL";
    private String[] testCuttingPatterns = new String[]{"TCCG","CA","AAA"};
    private String testFastaFile="src/test/resources/smallgenome/chr4_ctg9_hap1.fa";

    private final File fasta = new File(testFastaFile);
    IndexedFastaSequenceFile testFastaReader = new IndexedFastaSequenceFile(fasta);
    ViewPoint testViewpoint = new ViewPoint(testReferenceSequenceID, testGenomicPos, testMaxUpstreamGenomicPos, testMaxDownstreamPos, testCuttingPatterns, testFastaReader);

    public ViewPointTest() throws FileNotFoundException {} // Not nice, but without there will be an error. Why?


    /* test getter and setter functions */

    @Test
    public void testSetAndGetReferenceID() throws Exception {
        testViewpoint.setReferenceID(testReferenceSequenceID);
        assertEquals(testReferenceSequenceID,testViewpoint.getReferenceID());
    }

    @Test
    public void testSetAndGetGenomicPos() throws Exception {
        testViewpoint.setGenomicPos(testGenomicPos);
        assertEquals(testGenomicPos,testViewpoint.getGenomicPos());
    }

    @Test
    public void testSetAndGetMaxUpstreamGenomicPos() throws Exception {
        testViewpoint.setMaxUpstreamGenomicPos(testMaxUpstreamGenomicPos);
        assertEquals(testMaxUpstreamGenomicPos,testViewpoint.getMaxUpstreamGenomicPos());
    }

    @Test
    public void testSetAndGetMaxDownstreamPos() throws Exception {
        testViewpoint.setMaxDownstreamGenomicPos(testMaxDownstreamPos);
        assertEquals(testMaxDownstreamPos,testViewpoint.getMaxDownstreamGenomicPos());
    }
    @Test
    public void testSetAndGetStartAndEndPos() throws Exception {
        testViewpoint.setStartPos(testStartPos);
        assertEquals(testStartPos,testViewpoint.getStartPos());
        testViewpoint.setEndPos(testEndPos);
        assertEquals(testEndPos,testViewpoint.getEndPos());
    }

    @Test
    public void testSetAndGetDerivationApproach() throws Exception {
        testViewpoint.setDerivationApproach(testDerivationApproach);
        assertEquals(testDerivationApproach,testViewpoint.getDerivationApproach());
    }


    @Test
    public void testGetGenomicPosFromGenomicRelativePos() throws Exception {

        for(int i=0;i<testCuttingPatterns.length;i++) {

            ArrayList<Integer> relPosIntArray = testViewpoint.getCuttingPositionMap().getArrayListForGivenMotif(testCuttingPatterns[i]);

            for(int j=0;j<relPosIntArray.size();j++) {
                Integer genomicPosition = testGenomicPos + relPosIntArray.get(j);
                assertEquals(genomicPosition, testViewpoint.getGenomicPosOfGenomicRelativePos(testGenomicPos,relPosIntArray.get(j)));
            }
        }
    }


    /* test utility and wrapper functions */

    @Test
    public void testExtendFragmentWise() throws Exception {

        System.out.println("=========================================================================================");
        System.out.println("Test function 'testExtendFragmentWise' prints to the screen ");
        System.out.println("=========================================================================================");


        /* create viewpoint for testing */

        String referenceSequenceID="chr_t1_GATC";
        Integer genomicPos=80;
        Integer maxDistToGenomicPosUp=75;
        Integer maxDistToGenomicPosDown=75;
        String[] testCuttingPatterns = new String[]{"^GATC","A^AGCTT"};
        String testFastaFile="src/test/resources/testgenome/test_genome.fa";

        File fasta = new File(testFastaFile);
        IndexedFastaSequenceFile FastaReader = new IndexedFastaSequenceFile(fasta);
        ViewPoint testViewpointGATC = new ViewPoint(referenceSequenceID, genomicPos, maxDistToGenomicPosUp, maxDistToGenomicPosDown, testCuttingPatterns, FastaReader);
        String referenceSequence = FastaReader.getSubsequenceAt(referenceSequenceID, 0, FastaReader.getSequence(referenceSequenceID).length()).getBaseString();


        /* print to screen */

        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Complete genomic sequence with 'genomicPos' (|) and cutting sites (^) and initial start and end positions (> sta, < end) of the viewpoint");
        printLabledPos(testViewpointGATC.getGenomicPos(), "genomicPos", true);
        System.out.println(referenceSequence);
        printCuttingSites(testViewpointGATC, "GATC");
        printStaEndString(testViewpointGATC.getStartPos(),testViewpointGATC.getEndPos());

        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Set new start and end positions near 'genomicPos'");
        testViewpointGATC.setStartPos(testViewpointGATC.getGenomicPos()-4);
        testViewpointGATC.setEndPos(testViewpointGATC.getGenomicPos()+4);
        printLabledPos(testViewpointGATC.getGenomicPos(), "genomicPos", true);
        System.out.println(referenceSequence);
        printCuttingSites(testViewpointGATC, "GATC");
        printStaEndString(testViewpointGATC.getStartPos(),testViewpointGATC.getEndPos());

        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Apply function '' to position in upstream directio of the viewpoint");
        Integer upstream_pos=-6;
        System.out.println("Position upstream is: " + upstream_pos);
        testViewpointGATC.extendFragmentWise(upstream_pos);
        printLabledPos(testViewpointGATC.getGenomicPos(), "genomicPos", true);
        System.out.println(referenceSequence);
        printCuttingSites(testViewpointGATC, "GATC");
        printLabledPos(testViewpointGATC.relToAbsPos(upstream_pos), "position upstream", true);
        printStaEndString(testViewpointGATC.getStartPos(),testViewpointGATC.getEndPos());

        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Apply function '' to position in downstream direction of the viewpoint");
        Integer downstream_pos=30;
        System.out.println("Position downstream is: " + downstream_pos);
        testViewpointGATC.extendFragmentWise(downstream_pos);
        printLabledPos(testViewpointGATC.getGenomicPos(), "genomicPos", true);
        System.out.println(referenceSequence);
        printCuttingSites(testViewpointGATC, "GATC");
        printLabledPos(testViewpointGATC.relToAbsPos(downstream_pos), "position downstream", true);
        printStaEndString(testViewpointGATC.getStartPos(),testViewpointGATC.getEndPos());

        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Apply function '' to position in upstream direction of the viewpoint");
        upstream_pos=-40;
        System.out.println("Position upstream is: " + upstream_pos + " (X)");
        testViewpointGATC.extendFragmentWise(upstream_pos);
        printLabledPos(testViewpointGATC.getGenomicPos(), "genomicPos", true);
        System.out.println(referenceSequence);
        printCuttingSites(testViewpointGATC, "GATC");
        printLabledPos(testViewpointGATC.relToAbsPos(upstream_pos), "position upstream", true);
        printStaEndString(testViewpointGATC.getStartPos(),testViewpointGATC.getEndPos());

        System.out.println("=========================================================================================");
        System.out.println("Test function 'testExtendFragmentWise' END");
        System.out.println("=========================================================================================");
    }


    @Test
    public void testFragmentListMap() throws Exception {

        System.out.println("=========================================================================================");
        System.out.println("Test function 'testFragmentListMap' prints to the screen ");
        System.out.println("=========================================================================================");
        

        /* create viewpoint for testing */

        String referenceSequenceID="chr_t1_GATC";
        Integer genomicPos=80;
        Integer maxDistToGenomicPosUp=75;
        Integer maxDistToGenomicPosDown=75;
        String[] testCuttingPatterns = new String[]{"^GATC","A^AGCTT"};
        String testFastaFile="src/test/resources/testgenome/test_genome.fa";

        File fasta = new File(testFastaFile);
        IndexedFastaSequenceFile FastaReader = new IndexedFastaSequenceFile(fasta);
        ViewPoint testViewpointGATC = new ViewPoint(referenceSequenceID, genomicPos, maxDistToGenomicPosUp, maxDistToGenomicPosDown, testCuttingPatterns, FastaReader);
        String referenceSequence = FastaReader.getSubsequenceAt(referenceSequenceID, 0, FastaReader.getSequence(referenceSequenceID).length()).getBaseString();


        /* print to screen */

        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Complete genomic sequence with 'genomicPos' (|) and cutting sites (^) and initial start and end positions (> sta, < end) of the viewpoint");
        System.out.println("and print all restriction fragments of the viewpoint. 'T' means selected and 'F' not selected.");
        printLabledPos(testViewpointGATC.getGenomicPos(), "genomicPos", true);
        System.out.println(referenceSequence);
        printCuttingSites(testViewpointGATC, "GATC");
        printStaEndString(testViewpointGATC.getStartPos(),testViewpointGATC.getEndPos());
        printViewPointSegments(testViewpointGATC,"GATC");

        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("If the 'selectOrDeSelectFragment' is called with a position that is within a restriction fragment,");
        System.out.println("the fragment will be swichted from 'selected' to 'deselected' or the other way around.");
        System.out.println();
        System.out.println();
        testViewpointGATC.selectOrDeSelectFragment(-20);
        printViewPointSegments(testViewpointGATC,"GATC");
        testViewpointGATC.selectOrDeSelectFragment(-20);
        printViewPointSegments(testViewpointGATC,"GATC");
        testViewpointGATC.selectOrDeSelectFragment(18);
        printViewPointSegments(testViewpointGATC,"GATC");

        System.out.println("=========================================================================================");
        System.out.println("Test function 'testFragmentListMap' END");
        System.out.println("=========================================================================================");
    }


    @Test
    public void testGenerateViewpointLupianez() throws Exception {

        System.out.println("=========================================================================================");
        System.out.println("Test function 'testGenerateViewpointLupianez' prints to the screen ");
        System.out.println("=========================================================================================");
        
        
        /* create viewpoint for testing */

        String referenceSequenceID="chr_t4_GATC_short_20bp_and_long_24bp_fragments";
        Integer genomicPos=125;
        Integer maxDistToGenomicPosUp=115;
        Integer maxDistToGenomicPosDown=115;
        String[] testCuttingPatterns = new String[]{"^GATC","A^AGCTT"};
        String testFastaFile="src/test/resources/testgenome/test_genome.fa";

        File fasta = new File(testFastaFile);
        IndexedFastaSequenceFile FastaReader = new IndexedFastaSequenceFile(fasta);
        ViewPoint testViewpointLupianez = new ViewPoint(referenceSequenceID, genomicPos, maxDistToGenomicPosUp, maxDistToGenomicPosDown, testCuttingPatterns, FastaReader);
        String referenceSequence = FastaReader.getSubsequenceAt(referenceSequenceID, 0, FastaReader.getSequence(referenceSequenceID).length()).getBaseString();

        System.out.println();
        System.out.println("Arguments used to call the constructor function 'ViewPoint':");
        System.out.println("\t" + "referenceSequenceID = " + referenceSequenceID);
        System.out.println("\t" + "genomicPos = " + genomicPos);
        System.out.println("\t" + "maxDistToGenomicPosUp = " + maxDistToGenomicPosUp);
        System.out.println("\t" + "maxDistToGenomicPosDown = " + maxDistToGenomicPosDown);
        System.out.println("\t" + "testCuttingPatterns = " + Arrays.toString(testCuttingPatterns));
        System.out.println("\t" + "testFastaFile = " + testFastaFile);
        System.out.println();

        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.println();
        System.out.println("Total length of the sequence '" + referenceSequenceID + ": " + referenceSequence.length());
        System.out.println();

        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.println();
        System.out.println("Print initial fragments:");
        printLabledPos(testViewpointLupianez.getGenomicPos(), "genomicPos", true);
        System.out.println(referenceSequence);
        printStaEndString(testViewpointLupianez.getStartPos(),testViewpointLupianez.getEndPos());
        printViewPointSegments(testViewpointLupianez, "GATC");

        Integer fragNumUp=1;
        Integer fragNumDown=1;
        String motif="GATC";
        Integer minSizeUp=20;
        Integer maxSizeUp=95;
        Integer minSizeDown=20;
        Integer maxSizeDown=95;
        Integer minFragSize=22;
        double minRepFrag=0.4;
        testViewpointLupianez.generateViewpointLupianez(fragNumUp, fragNumDown, motif,  minSizeUp, maxSizeUp, minSizeDown, maxSizeDown, minFragSize, minRepFrag);

        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.println();
        System.out.println("Arguments used to call the function 'generateViewpointLupianez':");
        System.out.println("\t" + "fragNumUp = " + fragNumUp);
        System.out.println("\t" + "fragNumDown = " + fragNumDown);
        System.out.println("\t" + "motif = " + motif);
        System.out.println("\t" + "minSizeUp = " + minSizeUp);
        System.out.println("\t" + "maxSizeUp = " + maxSizeUp);
        System.out.println("\t" + "minSizeDown = " + minSizeDown);
        System.out.println("\t" + "maxSizeDown = " + maxSizeDown);
        System.out.println("\t" + "minFragSize = " + minFragSize);
        System.out.println();

        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.println();
        System.out.println("Print fragments after application of 'generateViewpointLupianez':");
        printLabledPos(testViewpointLupianez.getGenomicPos(), "genomicPos", true);
        System.out.println(referenceSequence);
        printStaEndString(testViewpointLupianez.getStartPos(),testViewpointLupianez.getEndPos());
        printViewPointSegments(testViewpointLupianez,"GATC");

        System.out.println("=========================================================================================");
        System.out.println("Test function 'testGenerateViewpointLupianez' END");
        System.out.println("=========================================================================================");
    }


    /* test utility functions */


    private void printStaEndString(Integer sta, Integer end) {
        String s = new String("");
        s = "";
        for (int k = 0; k <= end; k++) {
            if (k == sta) {
                s += "> sta";
                k = k + 4;
            } else if (k == end) {
                s += "< end";
                break;
            } else {
                s += " ";
            }
        }
        System.out.println(s);
    }


    private void printViewPointSegments(ViewPoint vp, String motif) {

        for (int i = 0; i < vp.getRestSegListMap().get(motif).size(); i++) {
            Integer sta = vp.getRestSegListMap().get(motif).get(i).getStartPos();
            Integer end = vp.getRestSegListMap().get(motif).get(i).getEndPos();
            boolean selected = vp.getRestSegListMap().get(motif).get(i).getSelected();

            if (selected) {
                printSegement(vp.getRestSegListMap().get(motif).get(i), 'T');
            } else {
                printSegement(vp.getRestSegListMap().get(motif).get(i), 'F');

            }

            ArrayList<Segment> marginSegments = vp.getRestSegListMap().get(motif).get(i).getSegmentMargins(3);

            if (marginSegments.size() == 2) {
                printSegement(marginSegments.get(0), '>');
                printSegement(marginSegments.get(1), '<');
            }
            if (marginSegments.size() == 1) {
                printSegement(marginSegments.get(0), '>');
            }
        }
    }


    private void printLabledPos(Integer pos, String label, boolean above) {

        Integer labelOffset = label.length() / 2;

        String s1 = new String("");
        for (int k = 0; k < pos - labelOffset + 1; k++) {
            s1 += " ";
        }
        s1 += label;

        String s2 = new String("");
        for (int k = 0; k < pos; k++) {
            s2 += " ";
        }
        s2 += "|";

        if (above) {
            System.out.println(s1);
            System.out.println(s2);
        } else {
            System.out.println(s2);
            System.out.println(s1);
        }
    }


    private void printCuttingSites(ViewPoint vp, String motif) {

        String s = new String("");
        ArrayList<Integer> relPosArr = vp.getCuttingPositionMap().getHashMapOnly().get(motif);

        int l = 0;
        for (int k = 0; k < vp.relToAbsPos(vp.getMaxDownstreamGenomicPos()); k++) {
            if (k < vp.relToAbsPos(relPosArr.get(l))) {
                s += " ";
            } else {
                s += "^";
                l++;
            }
            if (l == relPosArr.size()) {
                break;
            }
        }
        System.out.println(s);
    }

    private void printSegement(Segment segment, char symbol){

        String s = new String("");

        for(int i = 0; i< segment.getStartPos();i++) {
             s += " ";
        }
        for(int i = 0; i<segment.length();i++) {
            s += symbol;
        }
        System.out.println(s);
    }
}