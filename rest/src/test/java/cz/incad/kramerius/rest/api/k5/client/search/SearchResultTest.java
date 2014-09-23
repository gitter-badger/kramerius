/*
 * Copyright (C) 2013 Pavel Stastny
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.incad.kramerius.rest.api.k5.client.search;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cz.incad.kramerius.FedoraNamespaceContext;
import cz.incad.kramerius.rest.api.k5.client.JSONDecorator;
import cz.incad.kramerius.rest.api.k5.client.item.ItemResource;
import cz.incad.kramerius.rest.api.k5.client.utils.SOLRDecoratorUtils;
import cz.incad.kramerius.rest.api.k5.client.utils.SOLRUtils;
import cz.incad.kramerius.utils.IOUtils;
import cz.incad.kramerius.utils.XMLUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class SearchResultTest {

    @Test
    public void testParsedJSON() throws IOException {
        URL urlRes = SearchResultTest.class.getResource("search_group.json");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copyStreams(urlRes.openStream(), bos);
        String str = new String(bos.toByteArray(), "UTF-8");
        JSONObject changed = SearchResource.changeJSONResult(str, "", new ArrayList<JSONDecorator>());
        Stack<JSONObject> stack = new Stack<JSONObject>();
        stack.push(changed);
        while(!stack.isEmpty()) {
            JSONObject popped = stack.pop();
            Set keys = popped.keySet();
            for (Object kobj : keys) {
                String key = (String) kobj;
                Object obj = popped.get(key);
                Assert.assertFalse(obj.equals("text"));
                Assert.assertFalse(obj.equals("text_ocr"));
                
                if (obj instanceof JSONObject) {
                    stack.push((JSONObject) obj);
                }
                if (obj instanceof JSONArray) {
                    JSONArray arr = (JSONArray) obj;
                    for (Object arrObj : arr) {
                        if (arrObj instanceof JSONObject) {
                            stack.push((JSONObject) arrObj);
                        }
                    }
                }
            }
        }
    }

    
    @Test
    public void testPrasedXML() throws ParserConfigurationException,
        SAXException, IOException, TransformerException, XPathExpressionException {
        URL urlRes = SearchResultTest.class.getResource("search_group.xml");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copyStreams(urlRes.openStream(), bos);

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        
        String[] paths = new String[] {
                "//*[@name='text_ocr']",
                "//*[@name='text']"
        };
        for (String path : paths) {
            XPathExpression expr = xpath.compile(path);

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document parsedBefore = builder.parse(new ByteArrayInputStream(bos.toByteArray()));

            NodeList nlist = (NodeList) expr.evaluate(parsedBefore, XPathConstants.NODESET);
            Assert.assertTrue(nlist.getLength() > 0);
            
            Document changed = SearchResource.changeXMLResult(new String(bos.toByteArray(), "UTF-8"), "");

            nlist = (NodeList) expr.evaluate(changed, XPathConstants.NODESET);
            Assert.assertTrue(nlist.getLength() == 0);
        }
    }
    
    
    @Test
    public void testChangeViewableXML() throws ParserConfigurationException,
        SAXException, IOException, TransformerException {
        URL urlRes = SearchResultTest.class.getResource("solr1.xml");
        Document solrDoc = XMLUtils.parseDocument(urlRes.openStream());
        Element result = XMLUtils.findElement(solrDoc.getDocumentElement(), "result");
        
        List<Element> elms = XMLUtils.getElements(result,
                new XMLUtils.ElementsFilter() {
                    @Override
                    public boolean acceptElement(Element element) {
                        return (element.getNodeName().equals("doc"));
                    }
        });
        for (Element docE : elms) {
            Boolean value = SOLRUtils.value(docE, "viewable",Boolean.class);
            Assert.assertTrue(value);
        }
        
    }
    
    @Test
    public void testChangeFieldsXML() throws ParserConfigurationException,
            SAXException, IOException, TransformerException {
        URL urlRes = SearchResultTest.class.getResource("search.xml");
        Document document = XMLUtils.parseDocument(urlRes.openStream());
        Element result = XMLUtils.findElement(document.getDocumentElement(), "result");
        List<Element> elms = XMLUtils.getElements(result,
                new XMLUtils.ElementsFilter() {
                    @Override
                    public boolean acceptElement(Element element) {
                        return (element.getNodeName().equals("doc"));
                    }
        });
        for (Element docE : elms) {
            SearchResource.replacePidsInDOM(docE);
            List<String> array = SOLRUtils
                    .array(docE, "pid_path", String.class);
            for (String str : array) {
                int indexOf = str.indexOf("/@");
                Assert.assertTrue(indexOf == -1);
            }
        }

    }

    @Test
    public void testRemoveFieldsXML() throws ParserConfigurationException,
            SAXException, IOException, TransformerException {
        URL urlRes = SearchResultTest.class.getResource("search.xml");
        Document document = XMLUtils.parseDocument(urlRes.openStream());
        Element result = XMLUtils.findElement(document.getDocumentElement(),
                "result");
        List<Element> elms = XMLUtils.getElements(result,
                new XMLUtils.ElementsFilter() {

                    @Override
                    public boolean acceptElement(Element element) {
                        return (element.getNodeName().equals("doc"));
                    }
                });

        for (Element docE : elms) {
            SearchResource.filterFieldsInDOM(docE);
            Element textOCR = XMLUtils.findElement(docE,
                    new XMLUtils.ElementsFilter() {

                        @Override
                        public boolean acceptElement(Element element) {
                            return (element.hasAttribute("name") && element
                                    .getAttribute("name").equals("text_ocr"));
                        }
                    });
            Assert.assertNull(textOCR);
        }
    }

    @Test
    public void testRepairMasterPIDXML() throws ParserConfigurationException,
            SAXException, IOException, TransformerException {
        URL urlRes = SearchResultTest.class.getResource("search.xml");
        Document document = XMLUtils.parseDocument(urlRes.openStream());
        Element result = XMLUtils.findElement(document.getDocumentElement(),
                "result");
        List<Element> elms = XMLUtils.getElements(result,
                new XMLUtils.ElementsFilter() {

                    @Override
                    public boolean acceptElement(Element element) {
                        return (element.getNodeName().equals("doc"));
                    }
                });
        for (Element docE : elms) {
            SearchResource.changeMasterPidInDOM(docE);
            String str = SOLRUtils.value(docE, "PID", String.class);
            int indexOf = str.indexOf("/@");
            Assert.assertTrue(indexOf == -1);
        }
    }

    @Test
    public void testRepairMasterPID() {
        String str = "{\"PID\":\"uuid:5bb0280c-3146-060f-6b75-045d7d9648c3/@1\",\"fedora.model\":\"article\",\"dc.title\":\"Contents\",\"title_sort\":\"CONTENTS\",\"status\":\"Active\",\"handle\":\"\",\"created_date\":\"2013-09-13T23:50:10.599Z\",\"modified_date\":\"2014-01-28T14:55:11.475Z\",\"dostupnost\":\"private\",\"issn\":\"\",\"mdt\":\"\",\"ddt\":\"\",\"img_full_mime\":\"application/pdf\",\"viewable\":true,\"rels_ext_index\":0,\"root_title\":\"Contents\",\"root_pid\":\"uuid:5bb0280c-3146-060f-6b75-045d7d9648c3\",\"level\":0,\"datum_str\":\"\",\"datum\":\"1970-01-01T01:00:00Z\",\"virtual\":false,\"datum_begin\":0,\"pages_count\":0,\"rok\":0,\"datum_end\":0,\"dc.identifier\":[\"uuid:5bb0280c-3146-060f-6b75-045d7d9648c3\",\"5bb0280c-3146-060f-6b75-045d7d9648c3\"],\"pid_path\":[\"uuid:5bb0280c-3146-060f-6b75-045d7d9648c3\"],\"parent_pid\":[\"uuid:5bb0280c-3146-060f-6b75-045d7d9648c3\"],\"language\":[\"eng\"],\"model_path\":[\"article\"],\"document_type\":[\"article\"]}";
        JSONObject jsonObj = JSONObject.fromObject(str);
        SearchResource.changeMasterPidInJSON(jsonObj);
        Assert.assertTrue(jsonObj.containsKey("PID"));
        Assert.assertTrue(jsonObj.getString("PID").equals(
                "uuid:5bb0280c-3146-060f-6b75-045d7d9648c3@1"));
    }

    @Test
    public void testRepairPathsPID() {
        String str = "{\"PID\":\"uuid:5bb0280c-3146-060f-6b75-045d7d9648c3\",\"fedora.model\":\"article\",\"dc.title\":\"Contents\",\"title_sort\":\"CONTENTS\",\"status\":\"Active\",\"handle\":\"\",\"created_date\":\"2013-09-13T23:50:10.599Z\",\"modified_date\":\"2014-01-28T14:55:11.475Z\",\"dostupnost\":\"private\",\"issn\":\"\",\"mdt\":\"\",\"ddt\":\"\",\"img_full_mime\":\"application/pdf\",\"viewable\":true,\"rels_ext_index\":0,\"root_title\":\"Contents\",\"root_pid\":\"uuid:5bb0280c-3146-060f-6b75-045d7d9648c3\",\"level\":0,\"datum_str\":\"\",\"datum\":\"1970-01-01T01:00:00Z\",\"virtual\":false,\"datum_begin\":0,\"pages_count\":0,\"rok\":0,\"datum_end\":0,\"dc.identifier\":[\"uuid:5bb0280c-3146-060f-6b75-045d7d9648c3\",\"5bb0280c-3146-060f-6b75-045d7d9648c3\"],\"pid_path\":[\"uuid:5bb0280c-3146-060f-6b75-045d7d9648c3/@1\"],\"parent_pid\":[\"uuid:5bb0280c-3146-060f-6b75-045d7d9648c3\"],\"language\":[\"eng\"],\"model_path\":[\"article\"],\"document_type\":[\"article\"]}";
        JSONObject jsonObj = JSONObject.fromObject(str);
        SearchResource.replacePidsInJSON(jsonObj);
        JSONArray jsonArray = jsonObj.getJSONArray("pid_path");
        for (Object object : jsonArray) {
            if (object instanceof String) {
                String arrayVal = (String) object;
                Assert.assertFalse(arrayVal.contains("/"));
            }
        }
    }

    @Test
    public void testBadRelsExtIndex() throws ParserConfigurationException, SAXException, IOException {
        URL urlRes = SearchResultTest.class.getResource("relsext_index.xml");
        Document document = XMLUtils.parseDocument(urlRes.openStream());
        Element result = XMLUtils.findElement(document.getDocumentElement(),
                "result");
        List<Element> elms = XMLUtils.getElements(result,
                new XMLUtils.ElementsFilter() {

                    @Override
                    public boolean acceptElement(Element element) {
                        return (element.getNodeName().equals("doc"));
                    }
                });
        
        Assert.assertTrue(elms.size() == 1);
        String index = ItemResource.relsExtIndex("uuid:dfc78a5c-2542-44cf-8722-ee8a0924a855", elms.get(0));
        //cannot find actual index, always returs 0
        Assert.assertEquals("0", index);
    }
}
