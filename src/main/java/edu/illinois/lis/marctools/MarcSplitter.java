package edu.illinois.lis.marctools;

import gov.loc.repository.pairtree.Pairtree;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.marc4j.MarcReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlReader;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

public class MarcSplitter 
{

	public static void main(String[] args) throws IOException 
    {
		String marcFile = args[0];
		String outputDir = args[1];
    	InputStream is = null;
    	if (marcFile.endsWith(".gz")) {
    		is = new GZIPInputStream(new FileInputStream(marcFile));
    	} else {
    		is = new FileInputStream(marcFile);
    	}
        
        MarcReader reader = new MarcXmlReader(is);
        Pairtree pt = new Pairtree();
	    while (reader.hasNext()) {
            Record record = reader.next();
            
            Map<String, String> volumeIds = getVolumeIds(record); 
            
            for (String volumeId: volumeIds.keySet())
            {
	        try 
	        {
	            String sourcePart = volumeId.substring(0, volumeId.indexOf("."));
	            String volumePart = volumeId.substring(volumeId.indexOf(".")+1, volumeId.length());
	            String path = pt.mapToPPath(volumePart);
	            String volumePartClean = pt.cleanId(volumePart);
	            
	            // Construct the file name for the marc XML
	            File xmlPath = new File( outputDir + File.separator + sourcePart 
	            		+ File.separator + "pairtree_root" 
	            		+ File.separator + path 
	            		+ File.separator + volumePartClean );

	            if (! xmlPath.exists())
	            {
		            System.out.println(xmlPath.getAbsolutePath());
		            xmlPath.mkdirs();
		            MarcWriter writer = new MarcXmlWriter(new FileOutputStream(xmlPath + File.separator + volumePartClean + ".marc.xml"));
		            writer.write(record);
		            writer.close();
	            } else {
	                System.out.println("Skipping " + xmlPath + " - exists");
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
            }
	    }		 
    }
	
	private static Map<String, String> getVolumeIds(Record record) {
		Map<String, String> volumeIds = new HashMap<String, String>();
		
        List<DataField> datafields = (List<DataField>)record.getDataFields();
        for (DataField datafield: datafields)
        {
        	String volumeId = null;
        	String volume = null;
        	
        	if (datafield.getTag().equals("974")) {
        		List<Subfield> ids = (List<Subfield>)datafield.getSubfields('u');
        		for (Subfield id: ids) {
        			volumeId = id.getData();
        		}
        		
        		if (volumeId == null) {
        			ids = (List<Subfield>)datafield.getSubfields('a');
        			for (Subfield id: ids) {
        				volumeId = id.getData();
        			}
        		}
        		
        		List<Subfield> volumes = (List<Subfield>)datafield.getSubfields('z');
        		for (Subfield v: volumes) {
        			volume = v.getData();
        		}
        		
        		volumeIds.put(volumeId, volume);

        	}
        }
        return volumeIds;
	}
}
