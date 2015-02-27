package cz.incad.kramerius.rest.api.k5.client.pdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.lowagie.text.DocumentException;

import cz.incad.kramerius.FedoraAccess;
import cz.incad.kramerius.ProcessSubtreeException;
import cz.incad.kramerius.SolrAccess;
import cz.incad.kramerius.document.DocumentService;
import cz.incad.kramerius.pdf.FirstPagePDFService;
import cz.incad.kramerius.pdf.GeneratePDFService;
import cz.incad.kramerius.pdf.utils.PDFExlusiveGenerateSupport;
import cz.incad.kramerius.rest.api.exceptions.GenericApplicationException;
import cz.incad.kramerius.rest.api.k5.client.JSONDecoratorsAggregate;
import cz.incad.kramerius.rest.api.k5.client.SolrMemoization;
import cz.incad.kramerius.rest.api.k5.client.pdf.AbstractPDFResource.FirstPage;
import cz.incad.kramerius.service.TextsService;
import cz.incad.kramerius.utils.ApplicationURL;
import cz.incad.kramerius.utils.IOUtils;
import cz.incad.kramerius.utils.conf.KConfiguration;

@Path("/v5.0/asyncpdf")
public class AsyncPDFResource  extends AbstractPDFResource{

    public static Logger LOGGER = Logger.getLogger(PDFResource.class.getName());

    //private boolean acquired;

    public JSONObject outputJSON(File generatedPDF) throws IOException, COSVisitorException, JSONException {
        String uuid = UUID.randomUUID().toString();
        PDFExlusiveGenerateSupport.pushFile(uuid, generatedPDF);
        JSONObject obj = new JSONObject();
        obj.put("handle", uuid);
        return obj;
    }

    @GET
    @Path("handle")
    @Produces({ "application/pdf" })
    public Response handle(@QueryParam("handle") String handle) {
        final File pFile = PDFExlusiveGenerateSupport.popFile(handle);
        if (pFile != null) {
            try {
                final InputStream fis = new FileInputStream(pFile);
                StreamingOutput stream = new StreamingOutput() {
                    public void write(OutputStream output)
                            throws IOException, WebApplicationException {
                        try {
                            IOUtils.copyStreams(fis, output);
                        } catch (Exception e) {
                            throw new WebApplicationException(e);
                        } finally {
                            if (pFile != null)
                                pFile.delete();
                        }
                    }
                };
                SimpleDateFormat sdate = new SimpleDateFormat(
                        "yyyyMMdd_mmhhss");
                return Response
                        .ok()
                        .header("Content-disposition",
                                    "attachment; filename="
                                            + sdate.format(new Date()) + ".pdf")
                        .entity(stream).build();
            } catch (FileNotFoundException e) {
                throw new PDFResourceNotFound("uuid not found");
            }
        } else {
            throw new PDFResourceNotFound("uuid not found");
        }
    }


    @GET
    @Path("parent")
    @Produces({ "application/json"})
    public Response parent(@QueryParam("pid") String pid,
            @QueryParam("number") String number,
            @QueryParam("firstPageType") @DefaultValue("TEXT") String pageType,
            @QueryParam("format") String format) {
        boolean acquired = false;
        try {
            acquired = PDFExlusiveGenerateSupport.PDF_SEMAPHORE.tryAcquire();
            if (acquired) {
                try {
                    String imgServletUrl = ApplicationURL
                            .applicationURL(this.requestProvider.get())
                            + "/img";
                    if ((configuration.getApplicationURL() != null)
                            && (!configuration.getApplicationURL().equals(""))) {
                        imgServletUrl = configuration.getApplicationURL()
                                + "img";
                    }
                    String i18nUrl = ApplicationURL
                            .applicationURL(this.requestProvider.get())
                            + "/i18n";
                    if ((configuration.getApplicationURL() != null)
                            && (!configuration.getApplicationURL().equals(""))) {
                        i18nUrl = configuration.getApplicationURL() + "i18n";
                    }
                    AbstractPDFResource.FirstPage fp = pageType != null ? AbstractPDFResource.FirstPage
                            .valueOf(pageType) : AbstractPDFResource.FirstPage.TEXT;
                    if (number == null || number.trim().equals(""))
                        number  = "" + (Integer.parseInt(number) - 1);
                    AbstractPDFResource.checkNumber(number);

                    File f = null;
                    if (fp == AbstractPDFResource.FirstPage.IMAGES) {
                        f = parent(pid, number, this.imageFirstPage,
                                this.service, solrAccess, documentService,
                                imgServletUrl, i18nUrl, format);
                    } else {
                        f = parent(pid, number, this.textFirstPage,
                                this.service, solrAccess, documentService,
                                imgServletUrl, i18nUrl, format);
                    }

                    //final File fileToDelete = f;
                    JSONObject outputJSON = outputJSON(f);

                    return Response.ok().entity(outputJSON.toString()).build();
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    throw new GenericApplicationException(e.getMessage());
                } catch (COSVisitorException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    throw new GenericApplicationException(e.getMessage());
                } catch (FileNotFoundException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    throw new GenericApplicationException(e.getMessage());
                } catch (DocumentException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    throw new GenericApplicationException(e.getMessage());
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    throw new GenericApplicationException(e.getMessage());
                } catch (ProcessSubtreeException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    throw new GenericApplicationException(e.getMessage());
                } catch (JSONException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    throw new GenericApplicationException(e.getMessage());
                }
            } else {
                throw new PDFResourceNotReadyException("not ready");

            }

        } finally {
            if (acquired)
                PDFExlusiveGenerateSupport.PDF_SEMAPHORE.release();
        }
    }
}
