package com.cordova.appUpdate;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;

public class ParseXmlService
{
    public HashMap<String, String> parseXml(InputStream inStream) throws Exception
    {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        
        // Instantiate a document builder factory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Through document builder factory to obtain a document builder
        DocumentBuilder builder = factory.newDocumentBuilder();
        // Through document by document builder to build a document instance
        Document document = builder.parse(inStream);
        // Access to XML document root node
        Element root = document.getDocumentElement();
        // Get all child nodes
        NodeList childNodes = root.getChildNodes();
        for (int j = 0; j < childNodes.getLength(); j++)
        {
            //Traverse the child nodes
            Node childNode = (Node) childNodes.item(j);
            if (childNode.getNodeType() == Node.ELEMENT_NODE)
            {
                Element childElement = (Element) childNode;
                if ("version".equals(childElement.getNodeName()))
                {
                    hashMap.put("version",childElement.getFirstChild().getNodeValue());
                }
                else if (("name".equals(childElement.getNodeName())))
                {
                    hashMap.put("name",childElement.getFirstChild().getNodeValue());
                }
                else if (("url".equals(childElement.getNodeName())))
                {
                    hashMap.put("url",childElement.getFirstChild().getNodeValue());
                }
                else if (("title".equals(childElement.getNodeName())))
                {
                    hashMap.put("title",childElement.getFirstChild().getNodeValue());
                }
                else if (("description".equals(childElement.getNodeName())))
                {
                    hashMap.put("description",childElement.getFirstChild().getNodeValue());
                }
            }
        }
        return hashMap;
    }
}