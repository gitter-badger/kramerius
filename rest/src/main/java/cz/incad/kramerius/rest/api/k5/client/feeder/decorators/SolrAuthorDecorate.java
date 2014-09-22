package cz.incad.kramerius.rest.api.k5.client.feeder.decorators;

import com.google.inject.Inject;
import cz.incad.kramerius.SolrAccess;
import cz.incad.kramerius.rest.api.k5.client.SolrMemoization;
import cz.incad.kramerius.rest.api.k5.client.utils.SOLRUtils;
import net.sf.json.JSONObject;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by hradskam on 19.9.14.
 */
public class SolrAuthorDecorate extends AbstractFeederDecorator{

    public static Logger LOGGER = Logger.getLogger(SolrAuthorDecorate.class.getName());

    @Inject
    SolrAccess solrAccess;

    @Inject
    SolrMemoization memo;

    public static final String KEY = AbstractFeederDecorator.key("SOLRAUTHOR");

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public void decorate(JSONObject jsonObject, Map<String, Object> runtimeContext) {
        try {
            String pid = jsonObject.getString("pid");
            Element doc = this.memo.getRememberedIndexedDoc(pid);
            if(doc == null){
                doc = this.memo.askForIndexDocument(pid);
            }
            if (doc != null) {
                List<String> aut = SOLRUtils.arrayForAuthors(doc, "dc.creator", String.class);
                if(aut != null && !aut.isEmpty()) {
                    jsonObject.put("autor", aut);
                }
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
