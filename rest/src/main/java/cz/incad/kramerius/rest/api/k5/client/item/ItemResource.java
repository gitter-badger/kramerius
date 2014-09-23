package cz.incad.kramerius.rest.api.k5.client.item;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.xml.transform.TransformerException;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import cz.incad.kramerius.FedoraAccess;
import cz.incad.kramerius.ObjectPidsPath;
import cz.incad.kramerius.ProcessSubtreeException;
import cz.incad.kramerius.SolrAccess;
import cz.incad.kramerius.rest.api.exceptions.ActionNotAllowedXML;
import cz.incad.kramerius.rest.api.exceptions.GenericApplicationException;
import cz.incad.kramerius.rest.api.k5.client.JSONDecorator;
import cz.incad.kramerius.rest.api.k5.client.JSONDecoratorsAggregate;
import cz.incad.kramerius.rest.api.k5.client.SolrMemoization;
import cz.incad.kramerius.rest.api.k5.client.SolrResultsAware;
import cz.incad.kramerius.rest.api.k5.client.item.exceptions.PIDNotFound;
import cz.incad.kramerius.rest.api.k5.client.utils.JSONUtils;
import cz.incad.kramerius.rest.api.k5.client.utils.PIDSupport;
import cz.incad.kramerius.rest.api.k5.client.utils.SOLRDecoratorUtils;
import cz.incad.kramerius.rest.api.k5.client.utils.SOLRUtils;
import cz.incad.kramerius.security.IsActionAllowed;
import cz.incad.kramerius.security.SecuredActions;
import cz.incad.kramerius.security.SecurityException;
import cz.incad.kramerius.service.ReplicateException;
import cz.incad.kramerius.service.ReplicationService;
import cz.incad.kramerius.service.replication.FormatType;
import cz.incad.kramerius.utils.ApplicationURL;
import cz.incad.kramerius.utils.FedoraUtils;
import cz.incad.kramerius.utils.IOUtils;
import cz.incad.kramerius.utils.XMLUtils;
import cz.incad.kramerius.utils.pid.LexerException;
import cz.incad.kramerius.utils.pid.PIDParser;
import cz.incad.kramerius.utils.solr.SolrUtils;

/**
 * Item endpoint
 * 
 * @author pavels
 * 
 */
@Path("/v5.0/item")
public class ItemResource {

    public static final Logger LOGGER = Logger.getLogger(ItemResource.class
            .getName());

    @Inject
    @Named("securedFedoraAccess")
    FedoraAccess fedoraAccess;

    @Inject
    SolrAccess solrAccess;

    @Inject
    Provider<HttpServletRequest> requestProvider;

    @Inject
    JSONDecoratorsAggregate decoratorsAggregate;

    
    @Inject
    SolrMemoization solrMemoization;

    @Inject
    IsActionAllowed isActionAllowed;

    @Inject
    ReplicationService replicationService;
    
    @GET
    @Path("{pid}/foxml")
    @Produces({ MediaType.APPLICATION_XML + ";charset=utf-8" })
    public Response foxml(@PathParam("pid") String pid) {
        boolean access = false;
        try {
            ObjectPidsPath[] paths = this.solrAccess.getPath(pid);
            for (ObjectPidsPath path : paths) {
                if (this.isActionAllowed.isActionAllowed(SecuredActions.READ.getFormalName(), pid, null, path)) {
                    access = true;
                    break;
                }
            }
            if (access) {
                checkPid(pid);
                byte[] bytes = replicationService.getExportedFOXML(pid, FormatType.IDENTITY);
                final ByteArrayInputStream is = new ByteArrayInputStream(bytes);
                StreamingOutput stream = new StreamingOutput() {
                    public void write(OutputStream output)
                            throws IOException, WebApplicationException {
                        try {
                            IOUtils.copyStreams(is, output);
                        } catch (Exception e) {
                            throw new WebApplicationException(e);
                        }
                    }
                };
                return Response.ok().entity(stream).build();
            } else throw new ActionNotAllowedXML("access denied");
        } catch (IOException e) {
            throw new PIDNotFound("cannot foxml for  " + pid);
        } catch (ReplicateException e) {
            throw new PIDNotFound("cannot foxml for  " + pid);
        }
    }
    
    @GET
    @Path("{pid}/streams/{dsid}")
    public Response stream(@PathParam("pid") String pid,
            @PathParam("dsid") String dsid) {
        try {
            checkPid(pid);
            if (!FedoraUtils.FEDORA_INTERNAL_STREAMS.contains(dsid)) {
                if (!PIDSupport.isComposedPID(pid)) {
                    String mimeTypeForStream = this.fedoraAccess
                            .getMimeTypeForStream(pid, dsid);
                    final InputStream is = this.fedoraAccess.getDataStream(pid,
                            dsid);
                    StreamingOutput stream = new StreamingOutput() {
                        public void write(OutputStream output)
                                throws IOException, WebApplicationException {
                            try {
                                IOUtils.copyStreams(is, output);
                            } catch (Exception e) {
                                throw new WebApplicationException(e);
                            }
                        }
                    };
                    return Response.ok().entity(stream).type(mimeTypeForStream)
                            .build();
                } else
                    throw new PIDNotFound("cannot find stream " + dsid);
            } else {
                throw new PIDNotFound("cannot find stream " + dsid);
            }
        } catch (IOException e) {
            throw new PIDNotFound(e.getMessage());
        }
    }

    @GET
    @Path("{pid}/streams")
    @Produces({ MediaType.APPLICATION_JSON + ";charset=utf-8" })
    public Response streams(@PathParam("pid") String pid) {
        try {
            checkPid(pid);
            JSONObject jsonObject = new JSONObject();
            if (!PIDSupport.isComposedPID(pid)) {
                Document datastreams = this.fedoraAccess
                        .getFedoraDataStreamsListAsDocument(pid);
                Element documentElement = datastreams.getDocumentElement();
                List<Element> elms = XMLUtils.getElements(documentElement);
                for (Element e : elms) {
                    JSONObject streamObj = new JSONObject();
                    String dsiId = e.getAttribute("dsid");

                    if (FedoraUtils.FEDORA_INTERNAL_STREAMS.contains(dsiId))
                        continue;

                    String label = e.getAttribute("label");
                    streamObj.put("label", label);

                    String mimeType = e.getAttribute("mimeType");
                    streamObj.put("mimeType", mimeType);

                    jsonObject.put(dsiId, streamObj);
                }
            }
            return Response.ok().entity(jsonObject.toString()).build();
        } catch (IOException e) {
            throw new PIDNotFound(e.getMessage());
        }
    }

    @GET
    @Path("{pid}/children")
    @Produces({ MediaType.APPLICATION_JSON + ";charset=utf-8" })
    public Response children(@PathParam("pid") String pid) {
        try {
            checkPid(pid);
            if (!PIDSupport.isComposedPID(pid)) {
                JSONArray jsonArray = new JSONArray();
                
                this.solrMemoization.clearMemo();
                
                List<String> fieldList = new ArrayList<String>();

                List<JSONDecorator> decs = this.decoratorsAggregate.getDecorators();
                for (JSONDecorator jsonDec : decs) {
                    if (jsonDec instanceof SolrResultsAware) {
                        SolrResultsAware saware = (SolrResultsAware) jsonDec;
                        List<String> fList = saware.getFieldList();
                        fieldList.addAll(fList);
                    }
                }

                List<String> children = solrChildren(pid, fieldList);
                for (String p : children) {
                    String repPid = p.replace("/", "");
                    // vrchni ma odkaz sam na sebe
                    if (repPid.equals(pid))
                        continue;
                    String uri = UriBuilder.fromResource(ItemResource.class)
                            .path("{pid}/children").build(pid).toString();
                    JSONObject jsonObject = JSONUtils.pidAndModelDesc(repPid,
                            uri.toString(),this.solrMemoization,
                            this.decoratorsAggregate, uri);
                    jsonArray.add(jsonObject);
                }
                return Response.ok().entity(jsonArray.toString()).build();
            } else {
                return Response.ok().entity(new JSONArray().toString()).build();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            return Response.ok().entity("{}").build();
        }
    }

    @GET
    @Path("{pid}/siblings")
    @Produces({ MediaType.APPLICATION_JSON + ";charset=utf-8" })
    public Response siblings(@PathParam("pid") String pid) {
        try {
            checkPid(pid);
            ObjectPidsPath[] paths = null;
            if (PIDSupport.isComposedPID(pid)) {
                paths = this.solrAccess.getPath(PIDSupport
                        .convertToSOLRType(pid));
            } else {
                paths = this.solrAccess.getPath(pid);
            }

            JSONArray sibsList = new JSONArray();
            for (ObjectPidsPath onePath : paths) {
                // metadata decorator
                sibsList.add(siblings(pid, onePath));
            }
            return Response.ok().entity(sibsList.toString()).build();

        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            return Response.ok().entity("{}").build();
        } catch (ProcessSubtreeException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return Response.ok().entity("{}").build();
        }
    }

    private JSON siblings(String pid, ObjectPidsPath onePath)
            throws ProcessSubtreeException, IOException {

        String parentPid = null;
        List<String> children = new ArrayList<String>();
        if (onePath.getLength() >= 2) {
            String[] pth = onePath.getPathFromRootToLeaf();
            parentPid = pth[pth.length - 2];
            children = solrChildren(parentPid, new ArrayList<String>());
        } else {
            children.add(pid);
        }

        JSONObject object = new JSONObject();
        JSONArray pathArray = new JSONArray();
        for (String p : onePath.getPathFromRootToLeaf()) {
            String uriString = UriBuilder.fromResource(ItemResource.class)
                    .path("{pid}/siblings").build(pid).toString();
            p = PIDSupport.convertToK4Type(p);
            JSONObject jsonObject = JSONUtils.pidAndModelDesc(p,
                    uriString,this.solrMemoization, this.decoratorsAggregate, uriString);
            pathArray.add(jsonObject);
        }
        object.put("path", pathArray);
        JSONArray jsonArray = new JSONArray();
        for (String p : children) {
            if (parentPid != null && p.equals(parentPid))
                continue;
            String uriString = UriBuilder.fromResource(ItemResource.class)
                    .path("{pid}/siblings").build(pid).toString();
            p = PIDSupport.convertToK4Type(p);
            JSONObject jsonObject = JSONUtils.pidAndModelDesc(p, uriString, this.solrMemoization, this.decoratorsAggregate, uriString);

            jsonObject.put("selected", p.equals(pid));
            jsonArray.add(jsonObject);
        }
        object.put("siblings", jsonArray);
        return object;
    }

    @GET
    @Path("{pid}/full")
    public Response full(@PathParam("pid") String pid) {
        try {
            checkPid(pid);
            if (PIDSupport.isComposedPID(pid)) {
                String fpid = PIDSupport.first(pid);
                String page = PIDSupport.rest(pid);
                int rpage = Integer.parseInt(page) - 1;
                if (rpage < 0)
                    rpage = 0;
                String suri = ApplicationURL
                        .applicationURL(this.requestProvider.get())
                        + "/img?pid="
                        + fpid
                        + "&stream=IMG_FULL&action=TRANSCODE&page=" + rpage;
                URI uri = new URI(suri);
                return Response.temporaryRedirect(uri).build();
            } else {
                String suri = ApplicationURL
                        .applicationURL(this.requestProvider.get())
                        + "/img?pid=" + pid + "&stream=IMG_FULL&action=GETRAW";
                URI uri = new URI(suri);
                return Response.temporaryRedirect(uri).build();
            }
        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new PIDNotFound("pid not found '" + pid + "'");
        }
    }

    @GET
    @Path("{pid}/preview")
    public Response preview(@PathParam("pid") String pid) {
        try {
            checkPid(pid);
            if (PIDSupport.isComposedPID(pid)) {
                String fpid = PIDSupport.first(pid);
                String page = PIDSupport.rest(pid);
                int rpage = Integer.parseInt(page) - 1;
                if (rpage < 0)
                    rpage = 0;

                String suri = ApplicationURL
                        .applicationURL(this.requestProvider.get())
                        + "/img?pid="
                        + fpid
                        + "&stream=IMG_PREVIEW&action=TRANSCODE&page=" + rpage;
                URI uri = new URI(suri);
                return Response.temporaryRedirect(uri).build();
            } else {
                String suri = ApplicationURL
                        .applicationURL(this.requestProvider.get())
                        + "/img?pid="
                        + pid
                        + "&stream=IMG_PREVIEW&action=GETRAW";
                URI uri = new URI(suri);
                return Response.temporaryRedirect(uri).build();
            }
        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new PIDNotFound("pid not found '" + pid + "'");
        }
    }

    @GET
    @Path("{pid}/thumb")
    public Response thumb(@PathParam("pid") String pid) {
        try {
            checkPid(pid);
            if (PIDSupport.isComposedPID(pid)) {
                String fpid = PIDSupport.first(pid);
                String page = PIDSupport.rest(pid);
                int rpage = Integer.parseInt(page) - 1;
                if (rpage < 0)
                    rpage = 0;

                String suri = ApplicationURL
                        .applicationURL(this.requestProvider.get())
                        + "/img?pid="
                        + fpid
                        + "&stream=IMG_THUMB&action=TRANSCODE&page=" + rpage;
                URI uri = new URI(suri);
                return Response.temporaryRedirect(uri).build();
            } else {
                String suri = ApplicationURL
                        .applicationURL(this.requestProvider.get())
                        + "/img?pid=" + pid + "&stream=IMG_THUMB&action=GETRAW";
                URI uri = new URI(suri);
                return Response.temporaryRedirect(uri).build();
            }
        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new PIDNotFound("pid not found '" + pid + "'");
        }
    }

    private void checkPid(String pid) throws PIDNotFound {
        try {
            if (PIDSupport.isComposedPID(pid)) {
                String p = PIDSupport.first(pid);
                this.fedoraAccess.getRelsExt(p);
            } else {
                this.fedoraAccess.getRelsExt(pid);
            }
        } catch (IOException e) {
            throw new PIDNotFound("pid not found");
        }
    }
    
    @GET
    @Path("{pid}")
    @Produces({ MediaType.APPLICATION_JSON + ";charset=utf-8" })
    public Response basic(@PathParam("pid") String pid) {
        try {
            if (pid != null) {
                checkPid(pid);
                if (PIDSupport.isComposedPID(pid)) {

                    JSONObject jsonObject = new JSONObject();
                    String uriString = basicURL(pid);
                    JSONUtils.pidAndModelDesc(pid, jsonObject,uriString, this.solrMemoization,
                            this.decoratorsAggregate, null);

                    return Response.ok().entity(jsonObject.toString()).build();
                } else {
                    try {
                        PIDParser pidParser = new PIDParser(pid);
                        pidParser.objectPid();

                        JSONObject jsonObject = new JSONObject();

                        String uriString = basicURL(pid);
                        JSONUtils.pidAndModelDesc(pid, jsonObject,
                                uriString, this.solrMemoization,
                                this.decoratorsAggregate, null);

                        return Response.ok().entity(jsonObject.toString())
                                .build();
                    } catch (IllegalArgumentException e) {
                        throw new GenericApplicationException(e.getMessage());
                    } catch (UriBuilderException e) {
                        throw new GenericApplicationException(e.getMessage());
                    } catch (LexerException e) {
                        throw new GenericApplicationException(e.getMessage());
                    }
                }
            } else {
                throw new PIDNotFound("pid not found '" + pid + "'");
            }
        } catch (IOException e) {
            throw new PIDNotFound("pid not found '" + pid + "'");
        }
    }

    /**
     * Basic URL
     * 
     * @param pid
     * @return
     */
    public static String basicURL(String pid) {
        String uriString = UriBuilder.fromResource(ItemResource.class)
                .path("{pid}").build(pid).toString();
        return uriString;
    }

    
    private List<String> solrChildren(String parentPid, List<String> fList) throws IOException {
        List<Document> docs = new  ArrayList<Document>();
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
            
            Document resp = this.solrAccess.request(request);
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
                this.solrMemoization.rememberIndexedDoc(docpid, docelm);

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
