package edu.illinois.lis.marctools;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.marc4j.MarcReader;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Leader;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

public class Marc2SQL {

    @SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException 
    {

    	String indir = args[0]; // "/Users/cwillis/dev/hathitrust/marc/sample.xml"; //args[0];
    	String leaderFile = args[1]; //"/Users/cwillis/dev/hathitrust/marc/leader.sql"; 
    	String controlFile = args[2]; //"/Users/cwillis/dev/hathitrust/marc/control.sql"; 
    	String tagFile = args[3]; // "/Users/cwillis/dev/hathitrust/marc/tag.sql"; 
    	String subfieldFile =  args[4]; //"/Users/cwillis/dev/hathitrust/marc/subfield.sql";
    	
        FileWriter leaderOut = new FileWriter(new File(leaderFile));
        FileWriter controlOut = new FileWriter(new File(controlFile));
        FileWriter tagOut = new FileWriter(new File(tagFile));
        FileWriter subfieldOut = new FileWriter(new File(subfieldFile));
        
        FileFilter fileFilter = new WildcardFileFilter("meta*.gz");
        File[] files = new File(indir).listFiles(fileFilter);
	int recordId = 1;
	int controlId = 1;
	int tagId = 1;
	int subfieldId = 1;
        for (int i = 0; i < files.length; i++) {

	        InputStream is = new GZIPInputStream(new FileInputStream(files[i]));
	        MarcReader reader = new MarcXmlReader(is);
		    while (reader.hasNext()) {
	            Record record = reader.next();
	            Leader leader = record.getLeader();
	            leaderOut.write(
	            		recordId + "|" +
	            		leader.getRecordLength() + "|" +
	            		leader.getRecordStatus() + "|" +
	            		leader.getTypeOfRecord() + "|" +
	            		new String(leader.getImplDefined1()) + "|" +
	            		leader.getCharCodingScheme() + "|" +
	            		leader.getIndicatorCount() + "|" +
	            		leader.getSubfieldCodeLength() + "|" +
	            		new String(leader.getImplDefined2()) + "|" +
	            		leader.getBaseAddressOfData() + "|" +
	            		new String(leader.getEntryMap()) + "\n");
	            
	           
	            List<ControlField> controlFields = record.getControlFields();
	            for (ControlField controlField: controlFields)
	            { 	
	            	controlOut.write(
	            			recordId + "|" + controlId + "|" + 
	            			controlField.getTag() + "|" + 
	            		    controlField.getData() + "\n");
	            	controlId++;
	            }
	            
	            List<DataField> datafields = (List<DataField>)record.getDataFields();
	            for (DataField dataField: datafields)
	            {
	            	
	            	tagOut.write(
	            			recordId + "|" + tagId + "|" +
	            			dataField.getTag() + "|" + 
	            		    dataField.getIndicator1() + "|" + 
	            		    dataField.getIndicator2() + "\n");
	            	
	            	List<Subfield> subfields = dataField.getSubfields();
	            	for (Subfield subfield: subfields) {            		
	            		subfieldOut.write(
	            				recordId + "|" + tagId + "|" + subfieldId + "|" +
	            				subfield.getCode() + "|" + subfield.getData() + "\n");
	            		subfieldId++;
	
	            	}
	            	tagId++;
	            	
	            }
	            recordId++;
		    }		 
        }
	    leaderOut.close();
	    controlOut.close();
	    tagOut.close();
	    subfieldOut.close();
    }
}
