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

public class PubDate {

    @SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException 
    {
    
    	String infile = args[0];
        MarcReader reader = new MarcXmlReader(new FileInputStream(infile));
	    while (reader.hasNext()) {
            Record record = reader.next();
            List<DataField> datafields = (List<DataField>)record.getDataFields();
            boolean foundDate = false;
            for (DataField datafield: datafields)
            {
            	if (datafield.getTag().equals("260")) {
            		List<Subfield> dates = (List<Subfield>)datafield.getSubfields('c');
            		for (Subfield date: dates) {
            			foundDate = true;
            			System.out.println(date.getData());
            		}
            	}
            }
            if (! foundDate) {
            	System.out.println("Not found");
            }
        }		 
	}
}
