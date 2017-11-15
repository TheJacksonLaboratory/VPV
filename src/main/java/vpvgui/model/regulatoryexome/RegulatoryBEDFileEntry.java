package vpvgui.model.regulatoryexome;

import com.sun.org.apache.regexp.internal.RE;
import org.apache.log4j.Logger;
import vpvgui.model.VPVGene;

/** This class encapsulates one line in what will become the BED file to order the regulatory exome/gene panel. We
 * model this data with a class so that we can sort the BED file according to chromosomal location after we have
 * extracted all the areas to be enriched.
 * ToDo This class is still a little inelegant with respect to the way it handles chromosomes.
 * @author Peter Robinson
 * @version 0.1.3 (2017-11-14)
 */
public class RegulatoryBEDFileEntry implements Comparable<RegulatoryBEDFileEntry>{
    static Logger logger = Logger.getLogger(RegulatoryBEDFileEntry.class.getName());

        String chromosome=null;
        int fromPos;
        int toPos;
        String elementName=null;

    public RegulatoryBEDFileEntry(String chrom, int from, int to, String name){
        if (!chrom.startsWith("chr"))
            chrom=String.format("chr%s",chrom); // coming from Ensembl and does not have chr at start of chromosomes
        this.chromosome=chrom;
        this.fromPos=from;
        this.toPos=to;
        this.elementName=name;
        //
    }

    public RegulatoryBEDFileEntry(RegulatoryElement regelem) {
        this.chromosome=regelem.getChrom();
        this.fromPos=regelem.getFrom();
        this.toPos=regelem.getTo();
        this.elementName = String.format("%s[%s]",regelem.getId(),regelem.getCategory());
    }


    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof RegulatoryBEDFileEntry))return false;
        RegulatoryBEDFileEntry oth = (RegulatoryBEDFileEntry)other;
        return chromosome.equals(oth.chromosome) &&
                fromPos==oth.fromPos &&
                toPos==oth.toPos &&
                elementName.equals(oth.elementName);
    }

    public String toString() {
        return String.format("%s\t%d\t%d\t%s", chrom2string(chromosome), fromPos, toPos, elementName);
    }

    /** Add a "chr" if needed to the beginning of the string.*/
    public String chrom2string(String c) {
        if (c.startsWith("chr")) return c;
        else return (String.format("chr%s",c));
    }


    public int chromAsInt() {
        String chr=chromosome.replaceAll("chr","");
        if (chr.equalsIgnoreCase("X")) return 23;
        else if (chr.equals("Y")) return 24;
        else if (chr.equals("MT")) return 25;
        else {
            try {
                return Integer.parseInt(chr);
            } catch (NumberFormatException e) {
                logger.error(String.format("Integer parse error for chromosome \"%s\"",chromosome));
                return 42;// should never happen
            }
        }
    }


    @Override
    public int compareTo(RegulatoryBEDFileEntry other) {
        int chromcomp=this.chromAsInt() - other.chromAsInt();
        int fromcomp = this.fromPos - other.fromPos;
        if (chromcomp==0) {
            return fromcomp;
        } else {
            return chromcomp;
        }
    }
}
