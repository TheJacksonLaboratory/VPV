package vpvgui.model.project;

/*
 * This a utility class for the class 'ViewPoint'.
 * The constructor is called with an 'IndexedFastaSequenceFile' (htsjdk), a reference sequence ID, a genomic position,
 * a list of cutting motifs, and maximum distances to the genomic position in up and downstream direction.
 *
 * The reference sequence ID and the genomic position need to exist in the must in the 'IndexedFastaSequenceFile',
 * otherwise an error will be thrown and no object will be created.
 *
 * The most central data structure in the class a 'HashMap' with 'Strings' as keys and 'ArrayLists' of 'Integers'
 * as values. The keys are cutting site motifs and the 'ArrayList' contains all positions of occurrences
 * relative to the transcription start site. There is one special key which contains the positions for the
 * union of all positions.
 *
 * In addition the usual getter and setter functions,
 * the class provides functions to navigate through the cutting sites of the viewpoints,
 * e.g. given a position within the interval [maxDistToTssUp,maxDistToTssDown] a function returns the
 * the postion of the next cuttig site up or downstream.
 *
 * Furthermore, there might be functions that return the lengths of all restriction fragments
 * within the interval [maxDistToTssUp,maxDistToTssDown].
 *
 */

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import vpvgui.exception.IntegerOutOfRangeException;
import vpvgui.exception.NoCuttingSiteFoundUpOrDownstreamException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This a utility class for the class 'ViewPoint'.
 * @author Peter Hansen
 */
public class CuttingPositionMap {

    /* fields */

    String referenceSequenceID;
    Integer genomicPos;
    Integer maxDistToTssUp;
    Integer maxDistToTssDown;
    private static HashMap<String,ArrayList<Integer>> cuttingPositionMap;
    private static HashMap<String,Integer> cuttingPositionMapOffsets;


    /* constructor */

    public CuttingPositionMap(IndexedFastaSequenceFile fastaReader, String referenceSequenceID, Integer genomicPos, String[] cuttingPatterns, Integer maxDistToTssUp, Integer maxDistToTssDown) {


        /* set fields */

        setReferenceID(referenceSequenceID);
        setGenomicPos(genomicPos);
        setMaxDistToTssUp(maxDistToTssUp);
        setMaxDistToTssDown(maxDistToTssDown);


        /* create maps */

        cuttingPositionMap = new HashMap<String,ArrayList<Integer>>();

        // determine offsets
        cuttingPositionMapOffsets = new HashMap<String,Integer>();
        for(int i=0;i<cuttingPatterns.length;i++) {
            Integer offSet=cuttingPatterns[i].indexOf('^');
            cuttingPatterns[i] = cuttingPatterns[i].replace("^",""); // remove '^' characters
            cuttingPositionMapOffsets.put(cuttingPatterns[i],offSet);
        }



        ArrayList<Integer> cuttingPositionListUnion = new ArrayList<Integer>();
        for(int i=0;i<cuttingPatterns.length;i++) {

            // get sequence around genomic position and convert everything to uppercase
            String tssRegionString = fastaReader.getSubsequenceAt(referenceSequenceID,genomicPos - maxDistToTssUp,genomicPos+maxDistToTssDown).getBaseString().toUpperCase();

            Pattern pattern = Pattern.compile(cuttingPatterns[i]);
            Matcher matcher = pattern.matcher(tssRegionString);
            ArrayList<Integer> cuttingPositionList = new ArrayList<Integer>();

            while(matcher.find()) {

                if (matcher.start()<=genomicPos) {
                    cuttingPositionList.add(matcher.start() - maxDistToTssUp + cuttingPositionMapOffsets.get(cuttingPatterns[i]));
                    cuttingPositionListUnion.add(matcher.start()-maxDistToTssUp + cuttingPositionMapOffsets.get(cuttingPatterns[i]));
                }
                else if (genomicPos<matcher.start()) {
                    cuttingPositionList.add(matcher.start() - genomicPos + cuttingPositionMapOffsets.get(cuttingPatterns[i]));
                    cuttingPositionListUnion.add(matcher.start() - genomicPos + cuttingPositionMapOffsets.get(cuttingPatterns[i]));
                }

            }
            cuttingPositionMap.put(cuttingPatterns[i],cuttingPositionList); // push array list to map
        }

        // add an array for the union of all cutting positions with key 'ALL'
        Set<Integer> uniqueSet = new HashSet<>(); // remove duplicates
        uniqueSet.addAll(cuttingPositionListUnion);
        cuttingPositionListUnion.clear();
        cuttingPositionListUnion.addAll(uniqueSet);
        Collections.sort(cuttingPositionListUnion); // sort
        cuttingPositionMap.put("ALL",cuttingPositionListUnion); // push array list to map

    }


    /* getter and setter functions */

    public final String getReferenceID() {
        return referenceSequenceID;
    }

    public final void setReferenceID(String setReferenceID) {
        this.referenceSequenceID=setReferenceID;
    }


    public final void setGenomicPos(Integer genomicPos) { this.genomicPos=genomicPos; }

    public final Integer getGenomicPos() {
        return genomicPos;
    }


    public final void setMaxDistToTssUp(Integer maxDistToTssUp) { this.maxDistToTssUp=maxDistToTssUp; }

    public final Integer getMaxDistToTssUp() {
        return maxDistToTssUp;
    }


    public final void setMaxDistToTssDown(Integer maxDistToTssDown) { this.maxDistToTssDown=maxDistToTssDown; }

    public final Integer getMaxDistToTssDown() {
        return maxDistToTssDown;
    }


    public final HashMap<String,ArrayList<Integer>> getCuttingPositionMap() {
        return cuttingPositionMap;
    }

    public final HashMap<String,Integer> getCuttingPositionMapOffsets() {
        return cuttingPositionMapOffsets;
    }

    /* utility functions */

    /**
     * Given a position within the interval [-maxDistToTssUp,maxDistToTssDown],
     * this function returns the next cutting position in up or downstream direction.
     *
     * @param pos       Position relative to 'genomicPos'.
     * @param direction Direction in which the next cutting site will be searched.
     * @return Position of the next cutting position relative to 'genomicPos'.
     * @throws IllegalArgumentException if a value different than 'up' or 'down' is passed as 'direction' parameter.
     */
    public Integer getNextCutPos(Integer pos, String direction) throws IllegalArgumentException, IntegerOutOfRangeException, NoCuttingSiteFoundUpOrDownstreamException {

        // throw exception
        if (!(Objects.equals(direction, "up") || Objects.equals(direction, "down"))) {
            throw new IllegalArgumentException("Please pass either 'up' or 'down' for 'direction'.");
        }

        // throw exception

        if (pos < -maxDistToTssUp || pos > maxDistToTssDown) {
            throw new IntegerOutOfRangeException("Please pass a value within the interval [-maxDistToTssUp=" + -maxDistToTssUp + ",maxDistToTssDown=" + maxDistToTssDown + "].");
        }

        // get array with cutting positions
        ArrayList<Integer> cutPosArray = new ArrayList<Integer>();
        cutPosArray = cuttingPositionMap.get("ALL");
        Integer returnCutPos = cutPosArray.get(cutPosArray.size() - 1);

        // reverse array, if the functions is called with 'up'
        if (direction == "up") {
            Collections.reverse(cutPosArray);
        }

        // find the next cutting site in up or downstream direction
        try {
            Iterator<Integer> cutPosArrayIt = cutPosArray.iterator();
            boolean found = false;
            while (cutPosArrayIt.hasNext()) {
                Integer nextCutPos = cutPosArrayIt.next();
                if (direction == "down" && (pos <= nextCutPos)) {
                    found = true;
                    returnCutPos = nextCutPos;
                    break;
                }
                if (direction == "up" && (pos >= nextCutPos)) {
                    found = true;
                    returnCutPos = nextCutPos;
                    break;
                }

            }
            if (!found) {
                throw new NoCuttingSiteFoundUpOrDownstreamException("EXCEPTION in function 'getNextCutPos': No cutting site " + direction + "stream of position " + pos + ". Will return the " +
                        "outermost cutting site in " + direction + "stream direction.");
            }
        } catch (NoCuttingSiteFoundUpOrDownstreamException e) {
            System.out.println(e.getMessage());
        }

        // reverse the array for future calls
        if (direction == "up") {
            Collections.reverse(cutPosArray);
        }

        return returnCutPos;
    }
}