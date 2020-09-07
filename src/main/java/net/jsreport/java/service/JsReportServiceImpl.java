package net.jsreport.java.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.jsreport.java.JsReportException;
import net.jsreport.java.dto.CreateTemplateRequest;
import net.jsreport.java.dto.RenderTemplateRequest;
import net.jsreport.java.entity.Report;
import net.jsreport.java.entity.Template;
import okhttp3.*;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

public class JsReportServiceImpl implements JsReportService {

    public static final String HEADER_FILE_EXTENSION = "File-Extension";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_AUTHORIZATION = "Authorization";

    private JsReportRetrofitService jsreportRetrofitService;

    public JsReportServiceImpl(String baseServerUrl, String username, String password) {
        Gson gson = new GsonBuilder().setLenient().create();

        OkHttpClient okHttpClient =
                new OkHttpClient()
                        .newBuilder()
                        .addInterceptor(new Interceptor() {
                            @Override
                            public okhttp3.Response intercept(Chain chain) throws IOException {
                                Request originalRequest = chain.request();

                                Request.Builder builder =
                                        originalRequest
                                                .newBuilder()
                                                .header(
                                                        HEADER_AUTHORIZATION,
                                                        Credentials.basic(username, password)
                                                );

                                Request newRequest = builder.build();
                                return chain.proceed(newRequest);
                            }
                        })
                        .build();

        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(baseServerUrl)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .build();

        jsreportRetrofitService = retrofit.create(JsReportRetrofitService.class);
    }

    public JsReportServiceImpl(String baseServerUrl) {
        Gson gson = new GsonBuilder().setLenient().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseServerUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        jsreportRetrofitService = retrofit.create(JsReportRetrofitService.class);
    }

    @Override
    public Report render(RenderTemplateRequest renderTemplateRequest) throws JsReportException {
        Call<ResponseBody> callRender = jsreportRetrofitService.render(renderTemplateRequest);

        try {
            Response<ResponseBody> syncResponse = callRender.execute();
            throwJsReportOnError(syncResponse);

            Report report = new Report();

            if (syncResponse.body() != null) {
                report.setContent(syncResponse.body().byteStream());
                report.setContentType(syncResponse.headers().get(HEADER_CONTENT_TYPE));
                report.setFileExtension(syncResponse.headers().get(JsReportServiceImpl.HEADER_FILE_EXTENSION));

            } else {
                throw new JsReportException("Invalid body response from server! Returns empty body!");

            }

            return report;
        } catch (IOException e) {
            throw new JsReportException(e);

        }
    }

    @Override
    public Template putTemplate(CreateTemplateRequest createTemplateRequest) throws JsReportException {
        return JsReportServiceImpl.<Template>processSyncCall(jsreportRetrofitService.putTemplate(createTemplateRequest)).body();
    }

    @Override
    public void removeTemplate(String id) throws JsReportException {
        processSyncCall(jsreportRetrofitService.removeTemplate(id));
    }

    private static <R> Response<R> processSyncCall(Call<?> call) throws JsReportException {
        try {
            Response<?> response = call.execute();
            throwJsReportOnError(response);

            return (Response<R>) response;
        } catch (IOException e) {
            throw new JsReportException(e);
        }
    }

    private static void throwJsReportOnError(Response<?> response) throws JsReportException {
        if (! response.isSuccessful()) {
            throw new JsReportException(String.format("Invalid status code (%d) !!!", response.code()));
        }
    }
}