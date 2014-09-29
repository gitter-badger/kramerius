package cz.incad.kramerius.rest.api.k5.client.feeder;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import cz.incad.kramerius.FedoraAccess;
import cz.incad.kramerius.MostDesirable;
import cz.incad.kramerius.SolrAccess;
import cz.incad.kramerius.rest.api.exceptions.GenericApplicationException;
import cz.incad.kramerius.rest.api.k5.client.JSONDecoratorsAggregate;
import cz.incad.kramerius.rest.api.k5.client.SolrMemoization;
import cz.incad.kramerius.rest.api.k5.client.utils.JSONUtils;
import cz.incad.kramerius.rest.api.k5.client.utils.SOLRUtils;
import cz.incad.kramerius.security.User;
import cz.incad.kramerius.users.UserProfileManager;
import cz.incad.kramerius.utils.ApplicationURL;
import cz.incad.kramerius.utils.XMLUtils;
import cz.incad.kramerius.utils.conf.KConfiguration;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/v5.0/feed")
public class FeederResource {

    private static final int LIMIT = 18;

    public static final Logger LOGGER = Logger.getLogger(FeederResource.class
            .getName());

    @Inject
    MostDesirable mostDesirable;

    @Inject
    @Named("securedFedoraAccess")
    FedoraAccess fedoraAccess;

    @Inject
    Provider<HttpServletRequest> requestProvider;

    @Inject
    JSONDecoratorsAggregate decoratorsAggregate;

    @Inject
    SolrAccess solrAccess;

    @Inject
    SolrMemoization solrMemo;
    
    @Inject
    Provider<User> userProvider;

    @Inject
    UserProfileManager userProfileManager;

    @Inject
    KConfiguration configuration;

    @GET
    @Path("newest")
    @Produces({ MediaType.APPLICATION_JSON + ";charset=utf-8" })
    public Response newest(
            @QueryParam("vc") @DefaultValue("") String virtualCollection,
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset,
            @QueryParam("type") String documentType,
            @QueryParam("policy") String policy) {
        try {
            if (limit == null) {
                limit = LIMIT;
            }
            policy = policy == null ? "all" : policy;
            int start = (offset == null) ? 0 : offset * limit;

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("rss",
                    ApplicationURL.applicationURL(requestProvider.get())
                            + "/inc/home/newest-rss.jsp");

            StringBuilder req = new StringBuilder("q=level%3a0");
            if (documentType != null) {
                req.append("&fq=document_type:" + documentType);
            }
            req.append("&rows=").append(limit).append("&start=").append(start)
                    .append("&sort=level+asc%2c+created_date+desc");

            Document document = this.solrAccess.request(req.toString());
            Element result = XMLUtils.findElement(
                    document.getDocumentElement(), "result");
            JSONObject jsonData = new JSONObject();
            List<Element> docs = XMLUtils.getElements(result,
                    new XMLUtils.ElementsFilter() {
                        @Override
                        public boolean acceptElement(Element element) {
                            return (element.getNodeName().equals("doc"));
                        }
                    });

            for (Element doc : docs) {
                String pid = SOLRUtils.value(doc, "PID", String.class);
                if (pid != null) {
                    try {
                        String uriString = UriBuilder
                                .fromResource(FeederResource.class)
                                .path("newest").build(pid).toString();
                        JSONObject mdis = JSONUtils.pidAndModelDesc(pid,
                                uriString, this.solrMemo,
                                this.decoratorsAggregate, uriString);
                        if(policy.equals("all") || (mdis.containsKey("policy") && mdis.get("policy").equals(policy))) {
                            addToJSON(jsonData, mdis, mdis.getString("model"));
                        }
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                        JSONObject error = new JSONObject();
                        error.put("pid", pid);
                        error.put("exception", ex.getMessage());
                        addToJSON(jsonData, jsonObject, "error");
                    }
                }
            }
            jsonObject.put("data", jsonData);
            return Response.ok().entity(jsonObject.toString()).build();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            throw new GenericApplicationException(ex.getMessage());
        }
    }

    @GET
    @Path("mostdesirable")

    @Produces({ MediaType.APPLICATION_JSON + ";charset=utf-8" })
    public Response mostdesirable(
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset,
            @QueryParam("type") String documentType,
            @QueryParam("policy") String policy) {
        // "http://localhost:8080/search/inc/home/mostDesirables-rss.jsp"
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("rss",
                ApplicationURL.applicationURL(requestProvider.get())
                        + "/inc/home/mostDesirables-rss.jsp");
        if (limit == null) {
            limit = LIMIT;
        }
        if (offset == null) {
            offset = 0;
        }
        policy = policy == null ? "all" : policy;
        List<String> mostDesirable = this.mostDesirable.getMostDesirable(limit, offset, documentType);
        JSONObject jsonData = new JSONObject();
        for (String pid : mostDesirable) {
            try {
                String uriString = UriBuilder
                        .fromResource(FeederResource.class)
                        .path("mostdesirable").build(pid).toString();
                JSONObject mdis = JSONUtils.pidAndModelDesc(pid, 
                        uriString, this.solrMemo, this.decoratorsAggregate, uriString);
                if(policy.equals("all") || (mdis.containsKey("policy") && mdis.get("policy").equals(policy))) {
                    addToJSON(jsonData, mdis, mdis.getString("model"));
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                JSONObject error = new JSONObject();
                error.put("pid", pid);
                error.put("exception", e.getMessage());
                addToJSON(jsonData, jsonObject, "error");
            }
        }
        jsonObject.put("data", jsonData);

        return Response.ok().entity(jsonObject.toString()).build();
    }

    @GET
    @Path("custom")
    @Produces({ MediaType.APPLICATION_JSON + ";charset=utf-8" })
    public Response custom(
            @QueryParam("policy") String policy,
            @QueryParam("type") String documentType
    ) {
        JSONObject result = new JSONObject();
        JSONObject jsonData = new JSONObject();

        Set<String> config = configuration.getPropertiesSubset("search.home.tab.custom");
        policy = policy == null ? "all" : policy;

        for (String property : config) {
            String[] pids = configuration.getPropertyList(property);
            for (String pid : pids){
                if(property.equals("search.home.tab.custom.uuids")) {
                    if(!pid.isEmpty()) {
                        jsonData = getJSONForPid(pid, policy, jsonData);
                    }
                } else {
                    String model = property.split("\\.")[4];
                    jsonData = getJSONForPid(pid, policy, jsonData, model, true);
                }
            }
        }
        result.put("data", jsonData);

        return Response.ok().entity(result.toString()).build();
    }

    private JSONObject getJSONForPid(String pid, String policy, JSONObject jsonData){
        return getJSONForPid(pid, policy, jsonData, null, false);
    }

    private JSONObject getJSONForPid(String pid, String policy, JSONObject jsonData, String model, boolean hasModel){
        try {
            String uriString = UriBuilder
                    .fromResource(FeederResource.class)
                    .path("custom").build(pid).toString();
            JSONObject mdis = JSONUtils.pidAndModelDesc(pid, uriString, this.solrMemo, this.decoratorsAggregate, uriString);
            if(policy.equals("all") || (mdis.containsKey("policy") && mdis.get("policy").equals(policy))) {
                if(hasModel) {
                    if(model.equals(mdis.getString("model"))) {
                        addToJSON(jsonData, mdis, model);
                    } else {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("pid", "Neshoduje se model a kategorie pid: " + pid);
                        addToJSON(jsonData, jsonObject, "error");
                    }
                } else {
                    addToJSON(jsonData, mdis, mdis.getString("model"));
                }

            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            JSONObject error = new JSONObject();
            error.put("pid", pid);
            error.put("exception", e.getMessage());
            addToJSON(jsonData, error, "error");
        }
        return jsonData;
    }

    private void addToJSON(JSONObject jsonData, JSONObject mdis, String model) {
        if(jsonData.has(model)) {
            JSONArray jsonArray = jsonData.getJSONArray(model);
            jsonArray.add(mdis);
        } else {
            JSONArray jsonArray = new JSONArray();
            jsonArray.add(mdis);
            jsonData.put(model, jsonArray);
        }
    }
}
