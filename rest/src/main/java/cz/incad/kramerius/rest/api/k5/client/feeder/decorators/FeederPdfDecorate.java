package cz.incad.kramerius.rest.api.k5.client.feeder.decorators;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import cz.incad.kramerius.FedoraAccess;
import cz.incad.kramerius.rest.api.k5.client.utils.PIDSupport;
import cz.incad.kramerius.utils.ApplicationURL;
import cz.incad.kramerius.utils.FedoraUtils;
import cz.incad.kramerius.utils.imgs.ImageMimeType;
import net.sf.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by hradskam on 23.9.14.
 */
public class FeederPdfDecorate extends AbstractFeederDecorator {

    public static Logger LOGGER = Logger.getLogger(FeederPdfDecorate.class.getName());
    public static String KEY = AbstractFeederDecorator.key("FEEDERPDF");

    @Inject
    @Named("securedFedoraAccess")
    FedoraAccess fedoraAccess;

    @Inject
    Provider<HttpServletRequest> requestProvider;

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public void decorate(JSONObject jsonObject, Map<String, Object> runtimeContext) {
        try {
            String pid = jsonObject.getString("pid");
            if(!PIDSupport.isComposedPID(pid) && fedoraAccess.isImageFULLAvailable(pid)) {
                String mimeType = fedoraAccess.getMimeTypeForStream(pid, FedoraUtils.IMG_FULL_STREAM);
                ImageMimeType type = ImageMimeType.loadFromMimeType(mimeType);
                if(type != null && ImageMimeType.PDF.equals(type)) {
                    jsonObject.put("pdf", options(pid));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    private JSONObject options(String pid) {
        JSONObject options = new JSONObject();
        String url = ApplicationURL.applicationURL(this.requestProvider.get())
                .toString()
                + "/img?pid="
                + pid
                + "&stream="
                + FedoraUtils.IMG_FULL_STREAM + "&action=GETRAW";
        options.put("url", url);
        return options;
    }
}
