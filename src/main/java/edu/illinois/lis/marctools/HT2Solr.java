package edu.illinois.lis.marctools;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.marc4j.MarcReader;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class HT2Solr {

	public static void main(String[] args) throws Exception
	{
		//String zip = "test/ark+=13960=t87h1w287.zip";
		//String mets = "test/ark+=13960=t87h1w287.mets.xml";
		String outDir = "/Users/cwillis/dev/solr/solr-4.4.0/example/htrc/";
		
		String indir = "test/gpd";
		FileFilter fileFilter = new WildcardFileFilter("*mets.xml");
        File[] files = new File(indir).listFiles(fileFilter);
		
        for (File metsFile: files)
        {
        	String name = metsFile.getName();
        	String zipName = name.replace("mets.xml", "zip");
        	String marcName = name.replace("mets.xml", "marc.xml");
        	String outName = name.replace("mets.xml", "xml");
        			
		    ZipFile zipFile = new ZipFile(metsFile.getParentFile().getAbsolutePath() + File.separator + zipName);
			FileWriter out = new FileWriter(new File(outDir + File.separator + outName));
		    DocumentBuilderFactory domFactory = 
		    DocumentBuilderFactory.newInstance();
		    domFactory.setNamespaceAware(true); 
		    DocumentBuilder builder = domFactory.newDocumentBuilder();
		    Document doc = builder.parse(metsFile);
		    XPath xpath = XPathFactory.newInstance().newXPath();
		    xpath.setNamespaceContext(new MyNamespaceContext());	    
		    //<METS:fileGrp ID="FG3" USE="ocr">
	
		    String volumeId = getVolumeId(doc, xpath);
		    
		    Map<String, String> pageIdMap = getPageIdMap(doc, xpath);
		    Map<String, String> pageLabels = getPageLabels(doc, xpath, pageIdMap);
		    
		    File marcFile = new File(metsFile.getParentFile().getAbsolutePath() + File.separator + marcName);
		    String marc = getMarcData(marcFile);
		    
		    Enumeration<? extends ZipEntry> entries = zipFile.entries();
	
		    out.write("<add>\n");
		    while(entries.hasMoreElements()){
		        ZipEntry entry = entries.nextElement();
	
		    	if (entry.isDirectory())
		        	continue;
		    	
		   
		        String fileName = entry.getName();
		        String dirPart = fileName.substring(0, fileName.indexOf("/"));
		        String namePart = fileName.substring( fileName.indexOf("/") + 1, fileName.length());
		        if (namePart.contains(dirPart)) 
		        	continue;
		        String pageId = pageIdMap.get(namePart);
		        String pageNum = namePart.replaceAll("\\..*", "");
		        String documentId = volumeId + "-" + pageId;
		        
		        InputStream is = zipFile.getInputStream(entry);
		        String text = "";
		        BufferedReader br = new BufferedReader(new InputStreamReader(is));
		        String line;
		        while ((line = br.readLine()) != null) 
		        	text += line + "\n";
		        
		        // Skip blank pages
		        if (StringUtils.isBlank(text))
		        	continue;
		      
		    	out.write("<doc>\n");
		        out.write("<field name=\"volumeId\">" + StringEscapeUtils.escapeXml(volumeId) + "</field>\n");
		        out.write("<field name=\"id\">" + StringEscapeUtils.escapeXml(documentId) + "</field>\n");
		        out.write("<field name=\"pageNum\">" + Integer.valueOf(pageNum) + "</field>\n");
		        String label = pageLabels.get(pageId);
		        out.write("<field name=\"label\">" + StringEscapeUtils.escapeXml(label) + "</field>\n");
		        out.write("<field name=\"marc\">" + StringEscapeUtils.escapeXml(marc) + "</field>\n");
	

		        
		        out.write("<field name=\"text\">\n" + StringEscapeUtils.escapeXml(text) + "\n</field>\n");
		    	out.write("</doc>\n");
	
		    }
		    out.write("</add>");
		    out.close();
        }
	    
	}
	
	public static Map<String, String> getPageIdMap(Document doc, XPath xpath) throws XPathExpressionException
	{
	    XPathExpression expr = xpath.compile("/METS:mets/METS:fileSec/METS:fileGrp[@USE='ocr']");

	    Map<String, String> getPageIdMap = new HashMap<String, String>();
	    
	    Object result = expr.evaluate(doc, XPathConstants.NODESET);
	    NodeList nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	    	Node node = nodes.item(i);
	    	NodeList files = node.getChildNodes();
	    	for (int j = 0; j < files.getLength(); j++) {
	    		Node file = files.item(j);
	    		NamedNodeMap fileAttr = file.getAttributes();
	    		
	    		if (fileAttr != null)
	    		{
	    			NodeList locations = file.getChildNodes();
	    			Node fLocat = locations.item(1);
		    		NamedNodeMap fLocatAttr = fLocat.getAttributes();
	    			String id = fileAttr.getNamedItem("ID").getTextContent();
	    			String seq = fileAttr.getNamedItem("SEQ").getTextContent();		    		
	    			String href = fLocatAttr.getNamedItem("xlink:href").getTextContent();
	    			getPageIdMap.put(id, href);
	    			getPageIdMap.put(href, id);
	    			//System.out.println(id + "," + seq + "," + href);
	    		}
	    	}
	    	
	    }
	    return getPageIdMap;
		
	}
	
	public static Map<String, String> getPageLabels(Document doc, XPath xpath, Map<String, String> pageIdMap) 
			throws XPathExpressionException
	{
	    Map<String, String> pageLabelMap = new HashMap<String, String>();
	    XPathExpression expr = xpath.compile("/METS:mets/METS:structMap[@TYPE='physical']/METS:div[@TYPE='volume']");
	    Object result = expr.evaluate(doc, XPathConstants.NODESET);
	    NodeList nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	    	Node node = nodes.item(i);
	    	NodeList divisions = node.getChildNodes();
	    	for (int j = 0; j < divisions.getLength(); j++) {
	    		Node div = divisions.item(j);
	    		NamedNodeMap divAttr = div.getAttributes();
	    		
	    		if (divAttr != null)
	    		{
	    			//String order = divAttr.getNamedItem("ORDER").getTextContent();	
	    			Node labelNode = divAttr.getNamedItem("LABEL");
	    			String label = "";
	    			if (labelNode != null) 
	    				label = labelNode.getTextContent();	
	    			//String type = divAttr.getNamedItem("TYPE").getTextContent();	
	    			//String orderlabel = divAttr.getNamedItem("ORDERLABEL").getTextContent();	
	    			NodeList pointers = div.getChildNodes();
	    			
	    	    	for (int k = 0; k < pointers.getLength(); k++) {
	    	    		Node ptr = pointers.item(k);
	    	    	
	    	    		NamedNodeMap ptrAttr = ptr.getAttributes();
	    	    		if (ptrAttr != null)
	    	    		{
		    	    		String fileId = ptrAttr.getNamedItem("FILEID").getTextContent();
		    	    		if (pageIdMap.get(fileId) != null) {
		    	    			pageLabelMap.put(fileId, label);
		    	    			//System.out.println(fileId + "," + order + "," + label + "," + type);
		    	    		}
	    	    		}
	    	    	}
	    		}
	    	}	    	
	    }
	    return pageLabelMap;
	}
	
	public static String getVolumeId(Document doc, XPath xpath) 
			throws XPathExpressionException
	{
		String volumeId = null;
		
	    XPathExpression expr = xpath.compile("/METS:mets");
	    Object result = expr.evaluate(doc, XPathConstants.NODESET);
	    NodeList nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	    	Node node = nodes.item(i);
	    	NamedNodeMap attr = node.getAttributes();
    		
    		if (attr != null)
    			volumeId = attr.getNamedItem("OBJID").getTextContent();	
    	}
	    return volumeId;
	}
	
	static String getMarcData(File marcXml) throws IOException
	{		
		StringBuffer marcData = new StringBuffer();
		MarcReader reader = new MarcXmlReader(new FileInputStream(marcXml));
		if (reader.hasNext()) {
			Record record = reader.next();
			String title = getTitle(record);
			String author = getAuthor(record);
			String pubinfo = getPubInfo(record);
			String ids = getIdentifiers(record);
			marcData.append(author);
			marcData.append(title);
			marcData.append(pubinfo);
			marcData.append(ids);

			
		}
		System.out.println(marcData.toString());
		return marcData.toString();
	}
	
	static public String getFields(Record record, Map<String, char[]> fields) {
		StringBuffer fieldData = new StringBuffer();
        List<DataField> datafields = (List<DataField>)record.getDataFields();
        for (DataField datafield: datafields)
        {
        	String tag2 = datafield.getTag();
        	for (String tag1: fields.keySet()) 
        	{
        		if (tag2.equals(tag1))
        		{
        			char[] subfields = fields.get(tag1);
        			for (char subfield: subfields) {
        				List<Subfield> values = (List<Subfield>)datafield.getSubfields(subfield);
        				for (Subfield value: values) {
        					fieldData.append(" " + value.getData());
        				}
        			}	
        			fieldData.append("\n");
        		}
        		
        	}
        }
        return fieldData.toString();
	}
	
	public static String getAuthor(Record record)  {
		Map<String, char[]> fields = new HashMap<String, char[]>();
		
		fields.put("100", "abcdegqu".toCharArray());
		fields.put("110", "abcdegnu".toCharArray());
		fields.put("111", "acdegjnqu".toCharArray());
		//fields.put("700", "abcegqu".toCharArray());
		//fields.put("710", "abcdegnu".toCharArray());
		//fields.put("711", "acdegjnqu".toCharArray());
		
		return getFields(record, fields);
	}
	
	public static String getTitle(Record record)  {
		Map<String, char[]> fields = new HashMap<String, char[]>();
		
		fields.put("130", "adfghklmnoprst".toCharArray());
		fields.put("210", "ab".toCharArray());
		fields.put("222", "ab".toCharArray());
		fields.put("240", "adfghklmnoprs".toCharArray());
		fields.put("242", "abnp".toCharArray());
		fields.put("243", "adfghklmnoprs".toCharArray());
		fields.put("245", "abcnps".toCharArray());
		fields.put("246", "abfgnp".toCharArray());
		fields.put("247", "abfgnp".toCharArray());
		fields.put("440", "anpv".toCharArray());
		fields.put("490", "av".toCharArray());
		fields.put("730", "adfgklmnoprst".toCharArray());
		fields.put("740", "anp".toCharArray());
		
		return getFields(record, fields);
	}
	
	public static String getPubInfo(Record record)  {
		Map<String, char[]> fields = new HashMap<String, char[]>();
		
		fields.put("250", "ab".toCharArray());
		fields.put("260", "abcefg".toCharArray());
		
		return getFields(record, fields);
	}
	
	public static String getIdentifiers(Record record)  {
		Map<String, char[]> fields = new HashMap<String, char[]>();
		
		//ISBN
		fields.put("020", "az".toCharArray());
		// ISSN
		fields.put("022", "almyz".toCharArray());
		// Call number
		fields.put("050", "ab".toCharArray());
		// SUDOC
		fields.put("086", "az".toCharArray()); 
		//Call number
		fields.put("090", "ab".toCharArray());
		
		return getFields(record, fields);
	}
	
}

class MyNamespaceContext implements NamespaceContext
{
    public String getNamespaceURI(String prefix)
    {

        if (prefix.equals("METS"))
            return "http://www.loc.gov/METS/";
        else if (prefix.equals("xsi"))
            return "http://www.w3.org/2001/XMLSchema-instance";
        else if (prefix.equals("xlink"))
            return "http://www.w3.org/1999/xlink";
        else if (prefix.equals("PREMIS"))
            return "http://www.loc.gov/standards/premis";
        else
            return XMLConstants.NULL_NS_URI;
    }
    
    public String getPrefix(String namespace)
    {
        if (namespace.equals("http://www.loc.gov/METS/"))
            return "METS";
        else if (namespace.equals("http://www.w3.org/2001/XMLSchema-instance"))
            return "xsi";
        else if (namespace.equals("http://www.w3.org/1999/xlink"))
            return "xlink";
        else if (namespace.equals("http://www.loc.gov/standards/premis"))
            return "PREMIS"; 
        else
            return null;
    }

    public Iterator getPrefixes(String namespace)
    {
        return null;
    }
} 
