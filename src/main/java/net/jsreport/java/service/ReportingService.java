package net.jsreport.java.service;

import net.jsreport.java.JsReportException;
import net.jsreport.java.dto.RenderTemplateRequest;
import net.jsreport.java.entity.Report;

import java.util.concurrent.Future;

public interface ReportingService {

    /**
     * The simpliest rendering using template shortid and input data
     *
     * @param templateShortid can be taken from jsreport studio or from filename in jsreport embedded
     * @param data any json serializable object
     * @throws JsReportException When problems contact reporting service occurs.
     * @return Report result promise
     *
     * */
    Report render(String templateShortid, Object data) throws JsReportException;
    Future<Report> renderAsync(String templateShortid, Object data) throws JsReportException;

    /**
     * The simpliest rendering using template shortid and input data
     *
     * @param templateShortid template shortid can be taken from jsreport studio or from filename in jsreport embedded
     * @param jsonData any json String
     * @throws JsReportException When problems contact reporting service occurs.
     * @return Report result promise
     * */
    Report render(String templateShortid, String jsonData) throws JsReportException;
    Future<Report> renderAsync(String templateShortid, String jsonData) throws JsReportException;

    /**
     * Overload for more sophisticated rendering.
     *
     * @param request Description of rendering process {@link RenderTemplateRequest}
     * @throws JsReportException When problems contact reporting service occurs.
     *
     * @return Report result promise
     * */
    Report render(RenderTemplateRequest request) throws JsReportException;
    Future<Report> renderAsync(RenderTemplateRequest request) throws JsReportException;

    /**
     * Request jsreport package version.
     *
     * @return JSReport package version.
     * */
    String getServerVersion();
    Future<String> getServerVersionAsync();

}
