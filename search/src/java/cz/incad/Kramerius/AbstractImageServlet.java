package cz.incad.Kramerius;

import static cz.incad.kramerius.utils.IOUtils.copyStreams;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPathExpressionException;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.sun.net.httpserver.HttpServer;

import cz.incad.Kramerius.backend.guice.GuiceServlet;
import cz.incad.kramerius.FedoraAccess;
import cz.incad.kramerius.FedoraNamespaces;
import cz.incad.kramerius.imaging.utils.ImageUtils;
import cz.incad.kramerius.impl.fedora.FedoraDatabaseUtils;
import cz.incad.kramerius.security.SecurityException;
import cz.incad.kramerius.utils.FedoraUtils;
import cz.incad.kramerius.utils.IOUtils;
import cz.incad.kramerius.utils.RESTHelper;
import cz.incad.kramerius.utils.XMLUtils;
import cz.incad.kramerius.utils.conf.KConfiguration;
import cz.incad.kramerius.utils.imgs.ImageMimeType;
import cz.incad.kramerius.utils.imgs.KrameriusImageSupport;
import cz.incad.kramerius.utils.imgs.KrameriusImageSupport.ScalingMethod;
import cz.incad.utils.SafeSimpleDateFormat;

public abstract class AbstractImageServlet extends GuiceServlet {

    protected static final DateFormat[] XSD_DATE_FORMATS = {
            new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'S'Z'"),
            new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'S"),
            new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
            new SafeSimpleDateFormat("yyyy-MM-dd'Z'"),
            new SafeSimpleDateFormat("yyyy-MM-dd") };

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    public static final java.util.logging.Logger LOGGER = java.util.logging.Logger
            .getLogger(AbstractImageServlet.class.getName());

    public static final String SCALE_PARAMETER = "scale";
    public static final String SCALED_HEIGHT_PARAMETER = "scaledHeight";
    public static final String SCALED_WIDTH_PARAMETER = "scaledWidth";
    public static final String OUTPUT_FORMAT_PARAMETER = "outputFormat";

    @Inject
    protected transient KConfiguration configuration;

    @Inject
    @Named("securedFedoraAccess")
    protected transient FedoraAccess fedoraAccess;

    // @Inject
    // @Named("fedora3")
    // protected Provider<Connection> fedora3Provider;

    public static BufferedImage scale(BufferedImage img, Rectangle pageBounds,
            HttpServletRequest req, ScalingMethod scalingMethod) {
        String spercent = req.getParameter(SCALE_PARAMETER);
        String sheight = req.getParameter(SCALED_HEIGHT_PARAMETER);
        String swidth = req.getParameter(SCALED_WIDTH_PARAMETER);
        // System.out.println("REQUEST PARAMS: sheight:"+sheight+"swidth:"+swidth);
        if (spercent != null) {
            double percent = 1.0;
            {
                try {
                    percent = Double.parseDouble(spercent);
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
            return ImageUtils.scaleByPercent(img, pageBounds, percent,
                    scalingMethod);
        } else if (sheight != null) {
            int height = 200;
            {
                try {
                    height = Integer.parseInt(sheight);
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
            return ImageUtils.scaleByHeight(img, pageBounds, height,
                    scalingMethod);
        } else if (swidth != null) {
            int width = 200;
            {
                try {
                    width = Integer.parseInt(swidth);
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
            return ImageUtils.scaleByWidth(img, pageBounds, width,
                    scalingMethod);
        } else
            return null;
    }

    protected BufferedImage rawThumbnailImage(String uuid, int page)
            throws XPathExpressionException, IOException, SecurityException,
            SQLException {
        return KrameriusImageSupport.readImage(uuid,
                FedoraUtils.IMG_THUMB_STREAM, this.fedoraAccess, page);
    }

    protected BufferedImage rawFullImage(String uuid,
            HttpServletRequest request, int page) throws IOException,
            MalformedURLException, XPathExpressionException {
        return KrameriusImageSupport.readImage(uuid,
                FedoraUtils.IMG_FULL_STREAM, this.fedoraAccess, page);
    }

    protected BufferedImage rawImage(String uuid, String stream,
            HttpServletRequest request, int page) throws IOException,
            MalformedURLException, XPathExpressionException {
        return KrameriusImageSupport.readImage(uuid, stream, this.fedoraAccess,
                page);
    }

    protected void writeImage(HttpServletRequest req, HttpServletResponse resp,
            BufferedImage image, OutputFormats format) throws IOException {
        if ((format.equals(OutputFormats.JPEG))
                || (format.equals(OutputFormats.PNG))) {
            resp.setContentType(format.getMimeType());
            OutputStream os = resp.getOutputStream();
            KrameriusImageSupport.writeImageToStream(image,
                    format.getJavaFormat(), os);
        } else
            throw new IllegalArgumentException("unsupported mimetype '"
                    + format + "'");
    }

    protected void setDateHaders(String pid, String streamName,
            HttpServletResponse resp) throws IOException {
        Date lastModifiedDate = lastModified(pid, streamName);
        Calendar instance = Calendar.getInstance();
        instance.roll(Calendar.YEAR, 1);
        resp.setDateHeader("Last Modified", lastModifiedDate.getTime());
        resp.setDateHeader("Last Fetched", System.currentTimeMillis());
        resp.setDateHeader("Expires", instance.getTime().getTime());
    }

    private Date lastModified(String pid, String stream) throws IOException {
        Date date = null;
        Document streamProfile = fedoraAccess.getStreamProfile(pid, stream);

        Element elm = XMLUtils.findElement(streamProfile.getDocumentElement(),
                "dsCreateDate",
                FedoraNamespaces.FEDORA_MANAGEMENT_NAMESPACE_URI);
        if (elm != null) {
            String textContent = elm.getTextContent();
            for (DateFormat df : XSD_DATE_FORMATS) {
                try {
                    date = df.parse(textContent);
                    break;
                } catch (ParseException e) {
                    //
                }
            }
        }
        if (date == null) {
            date = new Date();
        }
        return date;
    }

    protected void setResponseCode(String pid, String streamName,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        long dateHeader = request.getDateHeader("If-Modified-Since");
        if (dateHeader != -1) {
            Date reqDate = new Date(dateHeader);
            Date lastModified = lastModified(pid, streamName);
            if (lastModified.after(reqDate)) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            }
        }
    }

    public FedoraAccess getFedoraAccess() {
        return fedoraAccess;
    }

    public void setFedoraAccess(FedoraAccess fedoraAccess) {
        this.fedoraAccess = fedoraAccess;
    }

    public abstract ScalingMethod getScalingMethod();

    public abstract boolean turnOnIterateScaling();

    public void copyFromImageServer(String urlString, HttpServletResponse resp)
            throws MalformedURLException, IOException {
        InputStream inputStream = null;
        try {
            URLConnection con = RESTHelper.openConnection(urlString, "", "");
            inputStream = con.getInputStream();
            String contentType = con.getContentType();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copyStreams(inputStream, bos);
            copyStreams(new ByteArrayInputStream(bos.toByteArray()),
                    resp.getOutputStream());
            resp.setContentType(contentType);
        } finally {
            IOUtils.tryClose(inputStream);
        }
    }

    protected String getURLForStream(String uuid, String urlFromRelsExt)
            throws IOException, XPathExpressionException, SQLException {
        StringTemplate template = new StringTemplate(urlFromRelsExt);
        // template.setAttribute("internalstream",
        // getPathForInternalStream(uuid));
        return template.toString();
    }

    // protected String getPathForInternalStream(String uuid) throws
    // SQLException, IOException {
    // return FedoraDatabaseUtils.getRelativeDataStreamPathAsString(uuid,
    // this.fedora3Provider);
    // }

    // public String getThumbnailIIPUrl(String uuid) throws SQLException,
    // UnsupportedEncodingException, IOException, XPathExpressionException {
    // String dataStreamPath = getPathForFullImageStream(uuid);
    // StringTemplate fUrl = stGroup().getInstanceOf("fullthumb");
    // setStringTemplateModel(uuid, dataStreamPath, fUrl, fedoraAccess);
    // fUrl.setAttribute("height",
    // "hei="+KConfiguration.getInstance().getConfiguration().getInt("scaledHeight",
    // FedoraUtils.THUMBNAIL_HEIGHT));
    // return fUrl.toString();
    // }

    public static StringTemplateGroup stGroup() {
        InputStream is = AbstractImageServlet.class
                .getResourceAsStream("imaging/iipforward.stg");
        StringTemplateGroup grp = new StringTemplateGroup(
                new InputStreamReader(is), DefaultTemplateLexer.class);
        return grp;
    }

    public static void setStringTemplateModel(String uuid,
            String dataStreamPath, StringTemplate template,
            FedoraAccess fedoraAccess) throws UnsupportedEncodingException,
            IOException {

        List<String> folderList = new ArrayList<String>();
        File currentFile = new File(dataStreamPath);
        while (!currentFile.getName().equals("data")) {
            folderList
                    .add(0, URLEncoder.encode(currentFile.getName(), "UTF-8"));
            currentFile = currentFile.getParentFile();
        }

        template.setAttribute("dataPath", KConfiguration.getInstance()
                .getFedoraDataFolderInIIPServer());
        template.setAttribute("folderList", folderList);
        template.setAttribute("iipServer", KConfiguration.getInstance()
                .getUrlOfIIPServer());
        String smimeType = fedoraAccess.getMimeTypeForStream("uuid:" + uuid,
                "IMG_FULL");

        ImageMimeType mimeType = ImageMimeType.loadFromMimeType(smimeType);
        // mimetype a koncovka ! Doplnovat a nedoplnovat
        if (mimeType != null) {
            String extension = mimeType.getDefaultFileExtension();
            if (!dataStreamPath.endsWith("." + extension)) {
                template.setAttribute("extension", "." + extension);
            } else {
                template.setAttribute("extension", "");
            }
        } else {
            template.setAttribute("extension", "");
        }
    }

    public enum OutputFormats {
        JPEG("image/jpeg", "jpg"), PNG("image/png", "png"),

        VNDDJVU("image/vnd.djvu", null), XDJVU("image/x.djvu", null), DJVU(
                "image/djvu", null),

        RAW(null, null);

        String mimeType;
        String javaFormat;

        private OutputFormats(String mimeType, String javaFormat) {
            this.mimeType = mimeType;
            this.javaFormat = javaFormat;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getJavaFormat() {
            return javaFormat;
        }
    }

    public static void main(String[] args) {
        String testUrl = "ahoj nevim co dal $neni$";
        StringTemplate template = new StringTemplate(testUrl);
        template.setAttribute("baseFolder", "some val");
        template.setAttribute("neni", "Je");
        System.out.println(template.toString());
    }
}
