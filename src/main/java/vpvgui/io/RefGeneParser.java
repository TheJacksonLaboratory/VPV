package vpvgui.io;


import org.apache.log4j.Logger;
import vpvgui.gui.popupdialog.PopupFactory;
import vpvgui.model.VPVGene;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Parse the refGene.txt.gz file from UCSC. The format is the same for all of the genome builds we are considering.
 * <ol>
 *     <li>bin (e.g., 197) No meaning forus, itis used to index the UCSC files</li>
 *     <li>name (e.g., NM_001353310)</li>
 *     <li>chrom (e.g., chr8)</li>
 *     <li>strand (e.g., -)</li>
 *     <li>txStart (e.g., 130851838)</li>
 *     <li>txEnd (e.g.,131028943)</li>
 *     <li>cdsStart (e.g., 130854387)</li>
 *     <li>cdsEnd (e.g.,130892694) </li>
 *     <li>exonCount (e.g., 13)</li>
 *     <li>exonStarts</li>
 *     <li>exonEnds</li>
 *     <li>score (e.g., 0)</li>
 *     <li>name2 (e.g., FAM49B)</li>
 *     <li>cdsStartStat</li>
 *     <li>cdsEndStat</li>
 *     <li>exonFrames</li>
 * </ol>
 * <p>Note that we skip all gene models located on random chromosomes because we do not want to create probes for
 * random chromosome contigs at this time.</p>
 * <p> The class produces a list of {@link VPVGene} objects that represent the genes found in the UCSC files.
 * These objects convert the coordinate system in the UCSC datbase file (which is 0-start, half-open) to one-based fully closed (both endpoints
 * included, which is the way the data are shown on the UCSC browser).</p>
 * @author Peter Robinson, Peter Hansen
 * @version 0.2.1 (2017-11-12)
 */
public class RefGeneParser {

    static Logger logger = Logger.getLogger(RefGeneParser.class.getName());
    /** All genes in the refGenefile are converted into VPVGene objects. These will be used to match
     * the gene list uploaded by the user. Key: A gene symbol (e.g., FBN1), value, the corresponding {@link VPVGene}.*/
    private Map<String, VPVGene> geneSymbolMap =null;
    /** This  map contains keys like APOC3_chr11_116700608, so that if genes have positions on  in the genome, each position is chosen. */
    private Map<String, VPVGene> gene2chromosomePosMap =null;
    /** The set of gene symbols that we could not find in the {@code refGene.txt.gz} file--and ergo,that we regard as being invalid because
     * they are using nonstandard gene symbols.*/
    private Set<String> invalidGeneSymbols=null;
    /** The set of gene symbols that we could find in  the {@code refGene.txt.gz} file--and ergo,that we regard as being valid.*/
    private Set<String> validGeneSymbols=null;

    /**
     * @param path Path to the {@code refGene.txt.gz} file.
     */
    public RefGeneParser(String path) {
        geneSymbolMap =new HashMap<>();
        gene2chromosomePosMap =new HashMap<>();
        parse(path);
    }

    /** Parse the {@code refGene.txt.gz} file. Note that we parse zero-based numbers here. */
    private void parse(String path) {
        try {
            InputStream fileStream = new FileInputStream(path);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            Reader decoder = new InputStreamReader(gzipStream);
            BufferedReader br = new BufferedReader(decoder);
            String line;
            while ((line=br.readLine())!=null) {
                //System.out.println(line);
                String A[]=line.split("\t");
                String accession=A[1];
                String chrom=A[2];
                if (chrom.contains("_")) { continue;}
                if (chrom.contains("random")) { continue; } /* do not take gene models on random contigs. */
                String strand=A[3];
                Integer gPos;
                // The UCSC database files have 0-based, closed start, open end numbers
                // we want to return 1-0based, fully closed position (both endpoints included).
                if (strand.equals("+")) {
                    gPos = Integer.parseInt(A[4]) + 1;
                } else {
                    gPos = Integer.parseInt(A[5]);
                }
                String name2=A[12];
                //String key = name2.concat(chrom);
                String key=String.format("%s_%s_%d",name2,chrom,gPos);
                //System.out.println(accession +"; "+chrom+"; "+strand+"; "+gPos+"; "+name2);
                System.out.println(key);
                VPVGene gene=null;
                if (gene2chromosomePosMap.containsKey(key)) {
                    gene = gene2chromosomePosMap.get(key);
                } else {
                    gene = new VPVGene(accession, name2);
                    gene.setChromosome(chrom);
                    if (strand.equals("+")){
                        gene.setForwardStrand();
                    } else if (strand.equals("-")){
                        gene.setReverseStrand();
                    } else {
                        System.err.println("[ERROR] did not recognize strand \""+ strand + "\"");
                        System.exit(1); /* todo throw some exception. */
                    }
                    geneSymbolMap.put(name2,gene);
                    gene2chromosomePosMap.put(key,gene);
                }
                gene.addGenomicPosition(gPos);
            }
            br.close();
        } catch (IOException e) {
            logger.error("Error while attempting to parse the RefGene file from UCSC:"+path);
            logger.error(e,e);
        }
    }




    /** @return A sorted list of those symbols uploaded by the user that could NOT be found in the {@code refGene.txt.gz} file.*/
    public List<String> getInvalidGeneSymbols() {
        List<String> lst = new ArrayList<>();
        lst.addAll(invalidGeneSymbols);
        Collections.sort(lst);
        return lst;
    }

    /** @return A sorted list of those symbols uploaded by the user that could be found in the {@code refGene.txt.gz} file.*/
    public List<String> getValidGeneSymbols() {
        List<String> lst = new ArrayList<>();
        lst.addAll(validGeneSymbols);
        Collections.sort(lst);
        return lst;
    }

    /** The user uploads a list of gene symbols. This function checks this list against the gene symbols that
     * are contained in the corresponding {@code refGene.txt.gz} file. Each uploaded symbol that is found in the
     * {@code refGene.txt.gz}  file is placed in {@link #geneSymbolMap} as the key,
     * and the corresponding {@link VPVGene} object is placed in the Set {@link #validGeneSymbols},
     * and all uploaded gene symbols that are not found there are placed in the Set {@link #invalidGeneSymbols}.
     * @param genelst List of gene symbols uploaded by the user.
     */
    public void checkGenes(List<String> genelst) {
        if (genelst==null) {
            logger.error("Attempt to check genelist with null pointer");
            PopupFactory.displayError("Unable to check gene list", "Attempt to check genelist with null pointer");
            return;
        }
        this.invalidGeneSymbols = new HashSet<>();
        this.validGeneSymbols = new HashSet<>();
        for (String sym:genelst) {
            if (this.geneSymbolMap.containsKey(sym)) { // it's only checked, if a gene is in refGene or not -> use geneSymbolMap
                validGeneSymbols.add(sym);
            } else {
                invalidGeneSymbols.add(sym);
            }
        }
    }

    /**
     * Iterate through all {@link VPVGene} objects and return a list of only those {@link VPVGene}s that
     * represent valid gene symbols uploaded by the user
     * @return List of VPVGenes representing valid uploaded gene symbols.
     */
    public List<VPVGene> getVPVGeneList() {
        List<VPVGene> genelist=new ArrayList<>();
        for (VPVGene g: gene2chromosomePosMap.values()) {
            if (this.validGeneSymbols.contains(g.getGeneSymbol()))
            genelist.add(g);
        }
        return genelist; // must contain objects RNU6-2 on chr1 as well for RNU6-2 on chr10 -> use gene2chromosomePosMap
    }

    /** @return the total number of {@link VPVGene} objects created from parsing the {@code refGene.txt.gz} file. */
    public int n_totalRefGenes() { return this.geneSymbolMap.size(); } // not sure if I should use geneSymbolMap or gene2chromosomePosMap -> use geneSymbolMap

    /** Calculates the total number of distinct start points (transcript start points), which would correspond to the
     * number of viewpoints we will design for this gene. Intended for unit testing.
     * @return number of distinct transcription start sites over all VPVGenes.
     */
    public int n_totalTSSstarts() {
        int n=0;
        for (VPVGene vpg : gene2chromosomePosMap.values()) { // must contain all TSS -> use gene2chromosomePosMap
            n+=vpg.n_viewpointstarts();
        }
        return n;
    }

}
