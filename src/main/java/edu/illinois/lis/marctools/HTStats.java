package edu.illinois.lis.marctools;

import gov.loc.repository.pairtree.Pairtree;

import java.io.BufferedReader;


import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class HTStats {

	public static void main(String[] args) throws Exception
	{
		String basePath = args[0];
		String id = args[1]; //test/gpd
		
		Pairtree pt = new Pairtree();
		String sourcePart = id.substring(0, id.indexOf("."));
        String volumePart = id.substring(id.indexOf(".")+1, id.length());
        String path = pt.mapToPPath(volumePart);
        String volumePartClean = pt.cleanId(volumePart);
		
        String indir = basePath 
        		+ File.separator + sourcePart 
        		+ File.separator + "pairtree_root"
        		+ File.separator + path 
        		+ File.separator + volumePartClean;
        System.out.println(indir);
		FileFilter fileFilter = new WildcardFileFilter("*mets.xml");
        File[] files = new File(indir).listFiles(fileFilter);
		
        for (File metsFile: files)
        {
        	String name = metsFile.getName();
        	String zipName = name.replace("mets.xml", "zip");
        			
		    ZipFile zipFile = new ZipFile(metsFile.getParentFile().getAbsolutePath() + File.separator + zipName);
		    DocumentBuilderFactory domFactory = 
		    DocumentBuilderFactory.newInstance();
		    domFactory.setNamespaceAware(true); 
		    DocumentBuilder builder = domFactory.newDocumentBuilder();
		    Document doc = builder.parse(metsFile);
		    XPath xpath = XPathFactory.newInstance().newXPath();
		    xpath.setNamespaceContext(new MyNamespaceContext());	    
	
		    String volumeId = getVolumeId(doc, xpath);		    
		    	    
		    Map<String, String> pageIdMap = getPageIdMap(doc, xpath);
		    Map<String, String> pageLabels = getPageLabels(doc, xpath, pageIdMap);
		   
		    Enumeration<? extends ZipEntry> entries = zipFile.entries();
	
		    int pages = 0;
		    long totalTokens = 0;
		    long totalLen = 0;
		    while(entries.hasMoreElements())
		    {
		    	long tokens = 0;
		    	long len = 0;
		    	
		        ZipEntry entry = entries.nextElement();
	
		    	if (entry.isDirectory())
		        	continue;
		    
		        String fileName = entry.getName();
		        String namePart = fileName.substring( fileName.indexOf("/") + 1, fileName.length());
		        String dirPart = fileName.substring(0, fileName.indexOf("/"));
		        String pageNum = namePart.replaceAll("\\..*", "");
		        
		        if (namePart.contains(dirPart)) 
		        	continue;
		        
		        String pageId = pageIdMap.get(namePart);
		        String label = pageLabels.get(pageId);
		        
		        InputStream is = zipFile.getInputStream(entry);
		        String text = "";
		        BufferedReader br = new BufferedReader(new InputStreamReader(is));
		        String line;
		        while ((line = br.readLine()) != null) 
		        	text += line + "\n";
		        
		        len = text.length();
		        
		        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_44);
		        TokenStream ts = analyzer.tokenStream("text", new StringReader(text));
		        try {
		            ts.reset(); // Resets this stream to the beginning. (Required)
		            while (ts.incrementToken()) {
		            	tokens++;
		            }
		            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
		          } finally {
		            ts.close(); // Release resources associated with this stream.
		          }
		   
		        System.out.println("page|" + volumeId + "|" + pageNum + "|" + len + "|" + tokens);
		        totalLen += len;
		        totalTokens += tokens;
		        pages++;
		    }
	        System.out.println("volume|" + volumeId + "|" + pages + "|" + totalLen + "|" + totalTokens);
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
	
}
