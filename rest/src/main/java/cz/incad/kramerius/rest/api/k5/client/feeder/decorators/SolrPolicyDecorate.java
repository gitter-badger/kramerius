package cz.incad.kramerius.rest.api.k5.client.feeder.decorators;

import com.google.inject.Inject;
import cz.incad.kramerius.rest.api.k5.client.SolrMemoization;
import cz.incad.kramerius.rest.api.k5.client.utils.SOLRUtils;
import net.sf.json.JSONObject;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by hradskam on 22.9.14.
 */
public class SolrPolicyDecorate extends AbstractFeederDecorator{
    public static final Logger LOGGER = Logger.getLogger(SolrPolicyDecorate.class.getName());
    public static final String KEY = AbstractFeederDecorator.key("SOLRPOLICY");

    @Inject
    SolrMemoization solrMemo;

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public void decorate(JSONObject jsonObject, Map<String, Object> runtimeContext) {
        try {
            String pid = jsonObject.getString("pid");
            Element doc = this.solrMemo.getRememberedIndexedDoc(pid);
            if(doc == null){
                doc = this.solrMemo.askForIndexDocument(pid);
            }
            String policy = SOLRUtils.value(doc, "dostupnost", String.class);
            if(policy != null) {
                jsonObject.put("policy", policy);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public boolean apply(JSONObject jsonObject, String context) {
        TokenizedPath fctx = super.feederContext(tokenize(context));
        if(fctx.isParsed()) {
            return !fctx.getRestPath().isEmpty() && mostDesirableOrNewestOrCustom(fctx);
        }
        return false;
    }
}
