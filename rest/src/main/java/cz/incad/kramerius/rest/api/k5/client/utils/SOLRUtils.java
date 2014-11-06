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
package cz.incad.kramerius.rest.api.k5.client.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Provider;
import cz.incad.kramerius.SolrAccess;
import cz.incad.kramerius.rest.api.k5.client.JSONDecoratorsAggregate;
import cz.incad.kramerius.rest.api.k5.client.SolrMemoization;
import org.apache.commons.collections.map.HashedMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cz.incad.kramerius.utils.XMLUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.logging.Logger;

public class SOLRUtils {

    public static final Logger LOGGER = Logger.getLogger(SOLRUtils.class
            .getName());

    public static Map<Class, String> SOLR_TYPE_NAMES = new HashMap<Class, String>();
    static {
        SOLR_TYPE_NAMES.put(String.class, "str");
        SOLR_TYPE_NAMES.put(Boolean.class, "bool");
        SOLR_TYPE_NAMES.put(Integer.class, "int");
    }

    public static <T> T value(String val, Class<T> clz) {
        val = val.trim();
        if (clz.equals(String.class))
            return (T) val;
        else if (clz.equals(Boolean.class))
            return (T) new Boolean(val);
        else if (clz.equals(Integer.class))
            return (T) Integer.valueOf(val);
        else
            throw new IllegalArgumentException("unsupported type " + clz + "");
    }

    public static Element value(Document doc, String val) {
        return value(doc, null, val);
    }

    public static Element value(Document doc, String attname, String val) {
        Element strElm = doc.createElement("str");
        if (attname != null)
            strElm.setAttribute("name", attname);
        strElm.setTextContent(val);
        return strElm;
    }

    public static Element value(Document doc, Integer val) {
        return value(doc, null, val);
    }

    public static Element value(Document doc, String attname, Integer val) {
        Element strElm = doc.createElement("int");
        if (attname != null)
            strElm.setAttribute("name", attname);
        strElm.setTextContent("" + val);
        return strElm;
    }

    public static Element arr(Document doc, String attname, List vals) {
        Element arrElm = doc.createElement("arr");
        if (attname != null)
            arrElm.setAttribute("name", attname);
        for (Object obj : vals) {
            if (obj instanceof String) {
                arrElm.appendChild(value(doc, (String) obj));
            } else if (obj instanceof Integer) {
                arrElm.appendChild(value(doc, (Integer) obj));
            } else
                throw new IllegalArgumentException("unsupported type "
                        + obj.getClass().getName() + "");
        }
        return arrElm;
    }

    public static <T> T value(final Element doc, final String attributeName,
            Class<T> clz) {
        final String expectedTypeName = SOLR_TYPE_NAMES.get(clz);
        List<Element> elms = XMLUtils.getElements(doc,
                new XMLUtils.ElementsFilter() {

                    @Override
                    public boolean acceptElement(Element element) {
                        return (element.getNodeName().equals(expectedTypeName)
                                && element.hasAttribute("name") && element
                                .getAttribute("name").equals(attributeName));
                    }
                });
        Object obj = elms.isEmpty() ? null : elms.get(0).getTextContent();
        if (obj != null)
            return value(obj.toString(), clz);
        else
            return null;
    }

    public static <T> List<T> array(final Element doc,
            final String attributeName, Class<T> clz) {
        List<T> ret = new ArrayList<T>();
        List<Element> elms = XMLUtils.getElements(doc,
                new XMLUtils.ElementsFilter() {

                    @Override
                    public boolean acceptElement(Element element) {
                        return (element.getNodeName().equals("arr")
                                && element.hasAttribute("name") && element
                                .getAttribute("name").equals(attributeName));
                    }
                });
        for (Element e : elms) {
            ret.add(value(elms.get(0).getTextContent(), clz));
        }
        return ret;
    }

    
    //TODO: CDK Bugfix !! change basic array method !
    public static <T> List<T> narray(final Element doc,
            final String attributeName, Class<T> clz) {
        List<T> ret = new ArrayList<T>();
        List<Element> elms = XMLUtils.getElements(doc,
                new XMLUtils.ElementsFilter() {

                    @Override
                    public boolean acceptElement(Element element) {
                        return (element.getNodeName().equals("arr")
                                && element.hasAttribute("name") && element
                                .getAttribute("name").equals(attributeName));
                    }
                });
     
        if (elms.size() >= 1) {
            Element parentE = elms.get(0);
            NodeList chnds = parentE.getChildNodes();
            for (int i = 0,ll=chnds.getLength() ; i < ll; i++) {
                Node n = chnds.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    ret.add(value(n.getTextContent(), clz));
                }
            }
        }
        return ret;
    }

    public static List<String> solrChildren(SolrAccess solrAccess, SolrMemoization solrMemoization, String parentPid, List<String> fList) throws IOException {
        List<Document> docs = new ArrayList<Document>();
        List<Map<String, String>> ll = new ArrayList<Map<String, String>>();
        int rows = 10000;
        int size = 1; // 1 for the first iteration
        int offset = 0;
        while (offset < size) {
            // request
            String request = "q=parent_pid:\"" + parentPid
                    + "\"&rows=" + rows + "&start=" + offset;
            if (!fList.isEmpty()) {
                request+="&fl=";
                for (int i = 0,bl=fList.size(); i < bl; i++) {
                    if (i >= 0) request += ",";
                    request += fList.get(i);
                }
            }

            Document resp = solrAccess.request(request);
            docs.add(resp);

            Element resultelm = XMLUtils.findElement(resp.getDocumentElement(), "result");
            // define size
            size = Integer.parseInt(resultelm.getAttribute("numFound"));
            List<Element> elms = XMLUtils.getElements(resultelm,
                    new XMLUtils.ElementsFilter() {
                        @Override
                        public boolean acceptElement(Element element) {
                            if (element.getNodeName().equals("doc")) {
                                return true;
                            } else
                                return false;
                        }
                    });

            for (Element docelm : elms) {
                String docpid = SOLRUtils.value(docelm, "PID", String.class);
                if (docpid.equals(parentPid)) continue;
                Map<String, String> m = new HashMap<String, String>();
                m.put("pid", docpid);
                m.put("index", relsExtIndex(parentPid, docelm));
                solrMemoization.rememberIndexedDoc(docpid, docelm);

                ll.add(m);
            }
            offset = offset + rows;
        }


        Collections.sort(ll, new Comparator<Map<String, String>>() {

            @Override
            public int compare(Map<String, String> o1, Map<String, String> o2) {
                Integer i1 = new Integer(o1.get("index"));
                Integer i2 = new Integer(o2.get("index"));
                return i1.compareTo(i2);
            }

        });

        List<String> values = new ArrayList<String>();
        for (Map<String, String> m : ll) {
            values.add(m.get("pid"));
        }
        return values;
    }

    /**
     * Finds correct rels ext position
     * @param parentPid
     * @param docelm
     * @return
     */
    public static String relsExtIndex(String parentPid, Element docelm) {
        List<Integer> docindexes =  SOLRUtils.narray(docelm, "rels_ext_index", Integer.class);

        if (docindexes.isEmpty()) return "0";
        List<String> parentPids = SOLRUtils.narray(docelm, "parent_pid", String.class);
        int index = 0;
        for (int i = 0, length = parentPids.size(); i < length; i++) {
            if (parentPids.get(i).endsWith(parentPid)) {
                index =  i;
                break;
            }
        }
        if (docindexes.size() > index) {
            return ""+docindexes.get(index);
        } else {
            LOGGER.warning("bad solr document for parent_pid:"+parentPid);
            return "0";
        }
    }
}
