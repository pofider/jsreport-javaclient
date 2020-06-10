package net.jsreport.java;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

// TODO - may use factory pattern ???
public class ReportingServiceImpl extends HttpService implements ReportingService {

    private Gson gson = new Gson();

    public ReportingServiceImpl(String baseServerUrl) {
        super(baseServerUrl);
    }

    public Future<Report> renderAsync(final String templateShortid, final Object data) throws JsReportException {
        FutureTask<Report> reportFutureTask =
                new FutureTask<Report>(new Callable<Report>() {
                    public Report call() throws Exception {
                    try {
                        return render(templateShortid, data);
                    } catch (JsReportException e) {
                        e.printStackTrace();
                    }

                    return null;
                    }
                });
//        reportFutureTask.run();
        return reportFutureTask;
    }

    public Report render(String templateShortid, Object data) throws JsReportException {
        Template template = new Template();
        template.setShortid(templateShortid);

        RenderRequest renderRequest = new RenderRequest();
        renderRequest.setTemplate(template);
        renderRequest.setData(data);

        return render(renderRequest);
    }

    public Future<Report> renderAsync(String templateShortid, String jsonData) throws JsReportException {
        return null;
    }

    public Report render(String templateShortid, String jsonData) throws JsReportException {
        String requestString = String.format("{ \"template\" : { \"shortid\" : \"%s\"}, \"data\" : %s }", templateShortid, jsonData);

        try {
            return renderString(requestString);
        } catch (IOException e) {
            throw new JsReportException(e);
        } catch (URISyntaxException e) {
            throw new JsReportException(e);
        }
    }

    public Future<Report> renderAsync(RenderRequest request) {
        return null;
    }

    public Report render(RenderRequest request) throws JsReportException {
        try {
            return renderString(gson.toJson(request));
        } catch (IOException e) {
            throw new JsReportException(e);
        } catch (URISyntaxException e) {
            throw new JsReportException(e);
        }
    }

    public String getServerVersion() {
        return null;
    }

    public Future<String> getServerVersionAsync() {
        return null;
    }


    // --- private

    private Report renderString(String request) throws IOException, JsReportException, URISyntaxException {
        HttpResponse response = post("/api/report", request);

        if (response.getStatusLine().getStatusCode() >= 300) {
            throw new JsReportException(String.format("Invalid status code (%d) !!!", response.getStatusLine().getStatusCode()));
        }

        Report result = new Report();

        try {
            result.setContent(response.getEntity().getContent());
        } catch (IOException e) {
            throw new JsReportException(e);
        }

        result.setContentType(findHeader(response, HEADER_CONTENT_TYPE));
        result.setFileExtension(findAndParseHeader(response, HEADER_FILE_EXTENSION));

        // TODO - how it works ?
        result.setPermanentLink(null);

        return result;
    }
}