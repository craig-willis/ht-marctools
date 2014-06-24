package edu.illinois.lis.marctools;

import java.io.FileInputStream;


import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.marc4j.MarcReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlReader;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

public class MarcUtil {

    @SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException 
    {
    	Map<Integer, Integer> yearFreq = new TreeMap<Integer, Integer>();
    
    	String infile = args[0];
    	String outfile = args[1];
    	int maxYear = 1923;
        MarcWriter writer = new MarcXmlWriter(new FileOutputStream(outfile), true);
        MarcReader reader = new MarcXmlReader(new FileInputStream(infile));
        Pattern p1 = Pattern.compile("[^\\d]*([\\d]{4})[^\\d]*");
        Pattern p2 = Pattern.compile("[^\\d]*([\\d]{4})[^\\d]*");
	    while (reader.hasNext()) {
            Record record = reader.next();
            List<DataField> datafields = (List<DataField>)record.getDataFields();
            for (DataField datafield: datafields)
            {
            	if (datafield.getTag().equals("260")) {
            		List<Subfield> dates = (List<Subfield>)datafield.getSubfields('c');
            		
            				
            		// Only look at the most recent;
            		int pubYear = 0;
            		for (Subfield date: dates) {
	            		Matcher m = p1.matcher(date.getData());
	            		if (m.matches())
	            		{	            			
	            			int yr = Integer.parseInt(m.group(1));
	            			if (yr > pubYear) 
	            				pubYear = yr;
	            		}
            		}
            		
            		if (pubYear > 0)
            		{
	        			Integer freq = yearFreq.get(pubYear);
	        			if (freq != null)
	        				freq++;
	        			else
	        				freq = 1;
		            			
            		    					
            			if (pubYear <= maxYear) 
            			{
            				writer.write(record);
            			}
            		}
            	}
            }
        }		 
	    writer.close();
	    
	    System.out.println("Years");
	    for (int year: yearFreq.keySet()) 
	    {
	    	int freq = yearFreq.get(year);
	    	System.out.println(year + ": " + freq);
	    }
	}
}
