package net.jsreport.java.service

import com.google.gson.Gson
import net.jsreport.java.JsReportException
import net.jsreport.java.dto.RenderTemplateRequest
import net.jsreport.java.entity.Report
import net.jsreport.java.entity.Template
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.ProtocolVersion
import org.apache.http.message.BasicHeader
import org.apache.http.message.BasicStatusLine
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import spock.lang.Specification
import spock.lang.Unroll

class ReportingServiceImplSpec extends Specification {

    private static final Gson GSON = new Gson()
    private static final PDFTextStripper PDF_TEXT_STRIPPER = new PDFTextStripper()
    private static final BasicHeader CONTENT_TYPE_HEADER = new BasicHeader(HttpRemoteServiceImpl.HEADER_CONTENT_TYPE, "")

    @Unroll
    def testRender() {
        setup:

        HttpRemoteService httpRemoteServiceMock = Mock(HttpRemoteService)
        ReportingService reportingService = new ReportingServiceImpl(httpRemoteServiceMock)

        when:

        RenderTemplateRequest renderTemplateRequest = new RenderTemplateRequest(template: new Template(name: "${name}"))
        Report report
        try {
            report = reportingService.render(renderTemplateRequest)
        } catch(JsReportException e) {
            if (name.startsWith("code_")) return
            if (name.contains(e.class.simpleName)) return
            if (e.cause != null && name.contains(e.cause.class.simpleName)) return

            assert false : "Invalid exception"
        }

        then:

        httpRemoteServiceMock.post("/api/report", _) >> { def args ->
            HttpResponse httpResponseMock = Mock(HttpResponse)

            RenderTemplateRequest req = GSON.fromJson(
                    new StringReader(args[1] as String),
                    RenderTemplateRequest.class
            )

            if (req.template.name.startsWith("code_")) {
                httpResponseMock.getStatusLine() >> { return new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), Integer.parseInt(req.template.name.substring(5)), "Test") }

            } else if (req.template.name.startsWith("ex_")) {
                String exType = req.template.name.substring(3)
                switch (exType) {
                    case "UnsupportedEncodingException" : throw new UnsupportedEncodingException()
                    case "URISyntaxException"           : throw new URISyntaxException("test", "test")
                    case "JsReportException"            : throw new JsReportException()
                    default                             : throw new JsReportException()
                }

            } else if (req.template.name == "${name}") {
                HttpEntity httpEntityMock = Mock(HttpEntity)
                httpResponseMock.getStatusLine() >> { return new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "Test") }
                httpEntityMock.getContent() >> { return ReportingServiceImplSpec.getResourceAsStream("${name}.pdf") }
                httpResponseMock.getEntity() >> { return httpEntityMock }
            }

            return httpResponseMock
        }

        httpRemoteServiceMock.findHeader(_, HttpRemoteServiceImpl.HEADER_CONTENT_TYPE) >> { return CONTENT_TYPE_HEADER }
        httpRemoteServiceMock.findAndParseHeader(_, HttpRemoteServiceImpl.HEADER_FILE_EXTENSION) >> { return "" }

        assert report != null
        assert report.content != null
        assert report.fileExtension == ""
        assert report.contentType == CONTENT_TYPE_HEADER
        assert report.getPermanentLink() == null

        def load = PDDocument.load(report.content)
        assert text == PDF_TEXT_STRIPPER.getText(load)
        load.close()

        where:

        name                                    | text
        "ok"                                    | "Simple test of template call!\n"
        "ex_UnsupportedEncodingException"       | null
        "ex_URISyntaxException"                 | null
        "ex_JsReportException"                  | null
        "code_500"                              | null
    }
}
