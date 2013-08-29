/*
 * Copyright 2013 Hewlett-Packard Development Company, L.P
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.alm.ali.rest.client;


import static com.hp.alm.ali.utils.StringUtils.joinWithSeparator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.hp.alm.ali.rest.client.exception.AuthenticationFailureException;
import com.hp.alm.ali.rest.client.exception.HttpStatusBasedException;
import com.hp.alm.ali.rest.client.exception.ResourceAccessException;
import com.hp.alm.ali.utils.XmlUtils;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

/**
 * Allows interaction with ALM rest.
 * Facilitates getting and posting resources into ALM rest.
 * <p/><p/><b>Rest URL composition</b><br/>
 * Resulting  rest location is composition example:
 * <ul>
 * <li>{@link #} : http://localost:8080/qcbin</li>
 * <li>{@link #setDomain(String)} : default</li>
 * <li>{@link #setProject(String)} : test_project</li> <p/>
 * </ul>
 * URL:<i>http://localost:8080/qcbin/domains/default/projects/test_project</i>
 * <p/><p/><b>Template parameter</b><br/>
 * <ul>
 * <li>most of  methods for rest interaction have parameter  <i>template<i/></li>
 * <li><i>template<i/> is expanded with position parameters {@link MessageFormat#format(String, Object...)}</li> <p/>
 * <li><i>template<i/> is appended to above composed url</li>
 * </ul>
 *
 * @see MessageFormat#format(String, Object...)
 */
public class AliRestClient {

    public static final LocationBasedBuilder<PostMethod> POST_BUILDER = new LocationBasedBuilder<PostMethod>() {
        @Override
        public PostMethod build(String location) {
            return new PostMethod(location);
        }
    };
    public static final LocationBasedBuilder<PutMethod> PUT_BUILDER = new LocationBasedBuilder<PutMethod>() {
        @Override
        public PutMethod build(String location) {
            return new PutMethod(location);
        }
    };
    public static final LocationBasedBuilder<GetMethod> GET_BUILDER = new LocationBasedBuilder<GetMethod>() {
        @Override
        public GetMethod build(String location) {
            return new GetMethod(location);
        }
    };
    public static final LocationBasedBuilder<DeleteMethod> DELETE_BUILDER = new LocationBasedBuilder<DeleteMethod>() {
        @Override
        public DeleteMethod build(String location) {
            return new DeleteMethod(location);
        }
    };
    public static final Set<Integer> AUTH_FAIL_STATUSES = Collections.unmodifiableSet(new HashSet<Integer>(Arrays.asList(401, 403)));

    /**
     * Creates ALM ALI rest client
     *
     * @param location location of an alm server e.g. http://localost:8080/qcbin
     * @param domain   ALM domain
     * @param project  ALM project
     * @param userName ALM user name
     * @param password ALM user password
     */
    public static AliRestClient create(String location, String domain, String project, String userName, String password, SessionStrategy sessionStrategy) {
        return new AliRestClient(location, domain, project, userName, password, sessionStrategy);
    }


    /**
     * Allows use already already established session.
     * Allows do login fallback after failed subsequent rest operation with given session.
     * When  subsequent rest operation fail with given session then is session dropped. Login creates new session and operation is repeated.
     * Fallback mechanism is driven by {@code loginOnFailure} parameter
     * When new session is created then is accessible via {@link #getSessionContext()}  and can be stored for further reuse.
     *
     * @param sessionContext
     * @see SessionContext
     */
    public static AliRestClient use(SessionContext sessionContext, String domain, String project, String userName, String password) {
        return new AliRestClient(sessionContext, domain, project, userName, password);
    }


    public static enum SessionStrategy {
        /**
         * Each REST operation is wrapped by implicit login and logout.
         */
        AUTO_LOGIN_LOGOUT,
        /**
         * Perform automatic login when no session is established or is expired .
         * Logout must be handled by caller..
         */
        AUTO_LOGIN,
        /**
         * Handling of login and logout must be explicitly handled by caller.
         */
        NONE
    }


    static final String COOKIE_SSO_NAME = "LWSSO_COOKIE_KEY";
    static final String COOKIE_SESSION_NAME = "QCSession";

    public static final int DEFAULT_CLIENT_TIMEOUT = 30000;

    private final String location;
    private final String userName;
    private final String password;

    private DomainProjectSynchronizedHolder domainProject = new DomainProjectSynchronizedHolder();

    private final SessionStrategy sessionStrategy;
    private final HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
    private volatile SessionContext sessionContext = null;
    private volatile String encoding = "UTF-8";


    private AliRestClient(String location, String domain, String project,
                          String userName, String password,
                          SessionStrategy sessionStrategy) {
        if (location == null) throw new IllegalArgumentException("location==null");
        validateProjectAndDomain(domain, project);
        this.location = location;
        this.userName = userName;
        this.password = password;
        domainProject.setDomain(domain);
        domainProject.setProject(project);
        this.sessionStrategy = sessionStrategy;
        setTimeout(DEFAULT_CLIENT_TIMEOUT);
        httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));
    }

    private static void validateProjectAndDomain(String domain, String project) {
        if (domain == null && project != null)
            throw new IllegalArgumentException("When project is specified than domain must specified also.");
    }


    private AliRestClient(SessionContext sessionContext, String domain, String project, String userName, String password) {
        validateProjectAndDomain(domain, project);
        this.sessionContext = sessionContext;
        httpClient.getState().addCookie(sessionContext.getSsoCookie());
        httpClient.getState().addCookie(sessionContext.getQcCookie());
        addTenantCookie(sessionContext.getSsoCookie());
        location = sessionContext.getAlmLocation();
        domainProject.setDomain(domain);
        domainProject.setProject(project);
        this.userName = userName;
        this.password = password;
        this.sessionStrategy = SessionStrategy.AUTO_LOGIN;
        setTimeout(DEFAULT_CLIENT_TIMEOUT);
        httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));
    }

    /**
     * Sets the default socket timeout (SO_TIMEOUT) which is the timeout for waiting for data and  the timeout until a connection is etablished.
     * Both timeouts are set to the same value.
     *
     * @param timeout in milliseconds
     */
    public void setTimeout(int timeout) {
        httpClient.getParams().setSoTimeout(timeout);
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
    }

    /**
     * Sets HTTP connection proxy.
     *
     * @param proxyHost proxy host
     * @param proxyPort proxy port
     */
    public void setHttpProxy(String proxyHost, int proxyPort) {
        httpClient.getHostConfiguration().setProxy(proxyHost, proxyPort);
    }

    /**
     * Sets HTTP proxy credentials
     * @param username proxy username
     * @param password proxy password
     */
    public void setHttpProxyCredentials(String username, String password) {
        Credentials cred = new UsernamePasswordCredentials(username, password);
        AuthScope scope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
        httpClient.getState().setProxyCredentials(scope, cred);
    }

    /**
     * Do login into ALM server rest.
     * Make new session into ALM rest.
     *
     * @throws com.hp.alm.ali.rest.client.exception.AuthenticationFailureException
     *          for 401 status code
     * @throws com.hp.alm.ali.rest.client.exception.HttpClientErrorException
     *          for http statuses 400-499
     * @throws com.hp.alm.ali.rest.client.exception.HttpServerErrorException
     *          for http statuses 500-599
     */
    public void login() {
        handleConcurrency(new Runnable() {
            @Override
            public void run() {
                // first try Apollo style login
                String authPoint = joinWithSeparator("/", location, "/authentication-point/alm-authenticate");
                PostMethod post = new PostMethod(authPoint);
                String xml = createAuthXml();
                post.setRequestEntity(createRequestEntity(InputData.create(xml)));
                post.addRequestHeader("Content-type", "application/xml");

                ResultInfo resultInfo = ResultInfo.create(false, null);
                executeAndWriteResponse(post, resultInfo, Collections.<Integer>emptySet());

                if(resultInfo.getHttpStatus() == HttpStatus.SC_NOT_FOUND) {
                    // try Maya style login
                    Credentials cred = new UsernamePasswordCredentials(userName, password);
                    AuthScope scope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
                    httpClient.getParams().setParameter(HttpMethodParams.CREDENTIAL_CHARSET, "UTF-8");
                    httpClient.getState().setCredentials(scope, cred);

                    // exclude the NTLM authentication scheme (requires NTCredentials we don't supply)
                    List<String> authPrefs = new ArrayList<String>(2);
                    authPrefs.add(AuthPolicy.DIGEST);
                    authPrefs.add(AuthPolicy.BASIC);
                    httpClient.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);

                    authPoint = joinWithSeparator("/", location, "/authentication-point/authenticate");
                    GetMethod get = new GetMethod(authPoint);
                    resultInfo = ResultInfo.create(false, null);
                    executeAndWriteResponse(get, resultInfo, Collections.<Integer>emptySet());
                }
                HttpStatusBasedException.throwForError(resultInfo.getHttpStatus(), resultInfo.getLocation());
                if(resultInfo.getHttpStatus() != 200) {
                    // during login we only accept 200 status (to avoid redirects and such as seemingly correct login)
                    throw new AuthenticationFailureException(resultInfo.getHttpStatus(), location);
                }

                Cookie[] cookies = httpClient.getState().getCookies();
                Cookie ssoCookie = getSessionCookieByName(cookies, COOKIE_SSO_NAME);
                Cookie qcCookie = getSessionCookieByName(cookies, COOKIE_SESSION_NAME);
                addTenantCookie(ssoCookie);
                sessionContext = new SessionContext(location, ssoCookie, qcCookie);
            }
        });
    }

    private String createAuthXml() {
        Element authElem = new Element("alm-authentication");
        Element userElem = new Element("user");
        authElem.addContent(userElem);
        userElem.setText(userName);
        Element passElem = new Element("password");
        passElem.setText(password);
        authElem.addContent(passElem);
        return new XMLOutputter().outputString(authElem);
    }

    private void addTenantCookie(Cookie ssoCookie) {
        if(ssoCookie != null) {
            Cookie tenant_id_cookie = new Cookie(ssoCookie.getDomain(), "TENANT_ID_COOKIE", "0");
            tenant_id_cookie.setDomainAttributeSpecified(true);
            tenant_id_cookie.setPath(ssoCookie.getPath());
            tenant_id_cookie.setPathAttributeSpecified(true);
            httpClient.getState().addCookie(tenant_id_cookie);
        }
    }

    private Cookie getSessionCookieByName(Cookie[] cookies, String name) {
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

    /**
     * Do logout from ALM server.
     *
     * @throws com.hp.alm.ali.rest.client.exception.HttpClientErrorException
     *          for http statuses 400-499
     * @throws com.hp.alm.ali.rest.client.exception.HttpServerErrorException
     *          for http statuses 500-599
     */
    public void logout() {
        handleConcurrency(new Runnable() {
            @Override
            public void run() {
                GetMethod get = new GetMethod(joinWithSeparator("/", location, "/authentication-point/logout"));
                ResultInfo resultInfo = ResultInfo.create(false, null);
                executeAndWriteResponse(get, resultInfo, Collections.<Integer>emptySet());
                HttpStatusBasedException.throwForError(resultInfo.getHttpStatus(), resultInfo.getLocation());
                sessionContext = null;
            }
        });
    }

    private void handleConcurrency(Runnable loginOrLogout) {
        if (sessionStrategy == SessionStrategy.AUTO_LOGIN_LOGOUT) {
            loginOrLogout.run();
        } else {
            synchronized (this) {
                loginOrLogout.run();
            }
        }

    }

    /**
     * Perform GET HTTP request for given location.
     *
     * @param template to be expanded and appended to ALM rest base
     * @param params   position based expansion of template
     * @return response body as string
     * @throws com.hp.alm.ali.rest.client.exception.HttpClientErrorException
     *          for http statuses 400-499
     * @throws com.hp.alm.ali.rest.client.exception.HttpServerErrorException
     *          for http statuses 500-599
     * @see MessageFormat#format(String, Object...)
     */
    public String getForString(String template, Object... params) {
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        ResultInfo result = ResultInfo.create(false, responseBody);
        get(result, template, params);
        HttpStatusBasedException.throwForError(result.getHttpStatus(), result.getLocation());
        return responseBody.toString();
    }

    /**
     * Perform GET HTTP request for given location.
     *
     * @param template to be expanded and appended to ALM rest base
     * @param params   position based expansion of template
     * @return response body as byte stream
     * @throws com.hp.alm.ali.rest.client.exception.HttpClientErrorException
     *          for http statuses 400-499
     * @throws com.hp.alm.ali.rest.client.exception.HttpServerErrorException
     *          for http statuses 500-599
     * @see MessageFormat#format(String, Object...)
     */
    public InputStream getForStream(String template, Object... params) {
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        ResultInfo result = ResultInfo.create(false, responseBody);
        get(result, template, params);
        HttpStatusBasedException.throwForError(result.getHttpStatus(), result.getLocation());
        return new ByteArrayInputStream(responseBody.toByteArray());
    }


    /**
     * Perform GET HTTP request for given location.
     * Does not throw exception for error status codes.
     *
     * @param template to be expanded and appended to ALM rest base
     * @param params   position based expansion of template
     * @param result   container for response related information
     * @return HTTP status code
     * @see MessageFormat#format(String, Object...)
     */
    public int get(ResultInfo result, String template, Object... params) {
        DomainProjectPair domainProjectPair = domainProject.getSnapshot();
        GetMethod method = createMethod(domainProjectPair, GET_BUILDER, null, template, params);
        executeHttpMethod(method, result);
        return result.getHttpStatus();
    }

    /**
     * Perform PUT HTTP request for given location.
     *
     * @param template to be expanded and appended to ALM rest base
     * @param params   position based expansion of template
     * @param result   container for response related information
     * @param inputData input data
     * @return HTTP status code
     */
    public int put(InputData inputData, ResultInfo result, String template, Object... params) {
        PutMethod putMethod = createMethod(domainProject.getSnapshot(), PUT_BUILDER, createRequestEntity(inputData), template, params);
        setHeaders(putMethod, inputData.getHeaders());
        executeHttpMethod(putMethod, result);
        return result.getHttpStatus();
    }

    /**
     * Perform PUT HTTP request for given location.
     *
     * @param template to be expanded and appended to ALM rest base
     * @param params   position based expansion of template
     * @param data     request body
     * @throws com.hp.alm.ali.rest.client.exception.HttpClientErrorException
     *          for http statuses 400-499
     * @throws com.hp.alm.ali.rest.client.exception.HttpServerErrorException
     *          for http statuses 500-599
     */
    public void put(InputData data, String template, Object... params) {
        ResultInfo result = ResultInfo.create(false, null);
        put(data, result, template, params);
        HttpStatusBasedException.throwForError(result.getHttpStatus(), result.getLocation());

    }

    /**
     * Perform DELETE HTTP request for given location.
     *
     * @param template to be expanded and appended to ALM rest base
     * @param params   position based expansion of template
     * @param result   container for response related information
     * @return HTTP status code
     */
    public int delete(ResultInfo result, String template, Object... params) {
        DeleteMethod deleteMethod = createMethod(domainProject.getSnapshot(), DELETE_BUILDER, null, template, params);
        executeHttpMethod(deleteMethod, result);
        return result.getHttpStatus();
    }

    /**
     * Perform DELETE HTTP request for given location.
     *
     * @param template to be expanded and appended to ALM rest base
     * @param params   position based expansion of template
     * @throws com.hp.alm.ali.rest.client.exception.HttpClientErrorException
     *          for http statuses 400-499
     * @throws com.hp.alm.ali.rest.client.exception.HttpServerErrorException
     *          for http statuses 500-599
     */
    public void delete(String template, Object... params) {
        ResultInfo result = ResultInfo.create(false, null);
        delete(result, template, params);
        HttpStatusBasedException.throwForError(result.getHttpStatus(), result.getLocation());
    }


    private static interface LocationBasedBuilder<T extends HttpMethod> {

        T build(String location);

    }


    /**
     * Perform POST HTTP request for given location.
     *
     * @param template to be expanded and appended to ALM rest base
     * @param params   position based expansion of template
     * @param result   container for response related information
     * @param data     request body
     * @return HTTP status code
     */
    public int post(InputData data, ResultInfo result, String template, Object... params) {
        PostMethod postMethod = createMethod(domainProject.getSnapshot(), POST_BUILDER, createRequestEntity(data), template, params);
        setHeaders(postMethod, data.getHeaders());
        executeHttpMethod(postMethod, result);
        return result.getHttpStatus();
    }
    
    public void postAndThrowException(InputData data, ResultInfo result, String template, Object... params) {
        post(data, result, template, params);
        HttpStatusBasedException.throwForError(result.getHttpStatus(), result.getLocation());
    }

    private void setHeaders(HttpMethod method, Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            method.setRequestHeader(entry.getKey(), entry.getValue());
        }
    }


    /**
     * Perform POST HTTP request for given location.
     *
     * @param template to be expanded and appended to ALM rest base
     * @param params   position based expansion of template
     * @param data     request body
     * @throws com.hp.alm.ali.rest.client.exception.HttpClientErrorException
     *          for http statuses 400-499
     * @throws com.hp.alm.ali.rest.client.exception.HttpServerErrorException
     *          for http statuses 500-599
     */
    public void post(InputData data, String template, Object... params) {
        ResultInfo result = ResultInfo.create(false, null);
        post(data, result, template, params);
        HttpStatusBasedException.throwForError(result.getHttpStatus(), result.getLocation());
    }


    private <T extends HttpMethod> T createMethod(DomainProjectPair domainProjectPair, LocationBasedBuilder<T> builder, RequestEntity requestEntity, String template, Object... params) {
        String location = composeLocation(domainProjectPair.domain, domainProjectPair.project, template, params);
        T method = builder.build(location);
        if (requestEntity != null) {
            ((EntityEnclosingMethod) method).setRequestEntity(requestEntity);
        }
        return method;
    }

    private static final class DomainProjectSynchronizedHolder {
        private String domain;
        private String project;


        public synchronized void setDomain(String domain) {
            validateProjectAndDomain(domain, project);
            this.domain = domain;
        }

        public synchronized void setProject(String project) {
            validateProjectAndDomain(domain, project);
            this.project = project;
        }

        public synchronized DomainProjectPair getSnapshot() {
            return new DomainProjectPair(domain, project);
        }


    }


    private static class DomainProjectPair {
        private final String domain;
        private final String project;

        private DomainProjectPair(String domain, String project) {
            this.domain = domain;
            this.project = project;
        }
    }

    private RequestEntity createRequestEntity(InputData inputData) {
        if (inputData.getData() != null) {
            RequestEntity requestEntity;
            try {
                requestEntity = new StringRequestEntity(inputData.getData(), "application/xml", encoding);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return requestEntity;
        } else if (inputData.getDataStream() != null) {
            return new InputStreamRequestEntity(inputData.getDataStream(), inputData.getSize(), "application/xml");
        } else {
            return null;
        }
    }

    private void writeResponse(ResultInfo result, HttpMethod method, boolean writeBodyAndHeaders) {
        OutputStream bodyStream = result.getBodyStream();

        StatusLine statusLine = method.getStatusLine();
        if (statusLine != null) {
            result.setHttpVersion(statusLine.getHttpVersion());
            result.setReasonPhrase(statusLine.getReasonPhrase());
        }
        if (writeBodyAndHeaders && bodyStream != null && method.getStatusCode() != 204) {
            try {
                InputStream responseBody = method.getResponseBodyAsStream();
                if (responseBody != null) {
                    copyStream(method.getResponseBodyAsStream(), bodyStream);
                    bodyStream.flush();
                    bodyStream.close();
                }
            } catch (IOException e) {
                throw new ResourceAccessException(e);
            }
        }
        try {
            result.setLocation(method.getURI().toString());
        } catch (URIException e) {
            throw new RuntimeException(e);
        }
        if (writeBodyAndHeaders) {
            Map<String, String> headersMap = result.getHeadersInternal();
            if (headersMap != null) {
                Header[] headers = method.getResponseHeaders();
                for (Header header : headers) {
                    headersMap.put(header.getName(), header.getValue());
                }
            }
        }
        result.setHttpStatus(method.getStatusCode());
    }

    private String composeLocation(String domain, String project, String template, Object... params) {
        params = encodeParams(params);
        String substituted = MessageFormat.format(template, params);
        Object encDomain = encodeParams(new Object[]{domain})[0];
        Object encProject = encodeParams(new Object[]{project})[0];
        if (encDomain == null) {
            return joinWithSeparator("/", location, "/rest", substituted);
        } else if (encProject == null) {
            return joinWithSeparator("/", location, "/rest/domains", encDomain.toString(), substituted);
        }
        return joinWithSeparator("/", location, "/rest/domains", encDomain.toString(), "projects", encProject.toString(), substituted);
    }

    private Object[] encodeParams(Object params[]) {
        String enc = encoding;
        Object result[] = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                result[i] = null;
                continue;
            }
            String strPar = params[i].toString();
            try {
                if (enc == null) {
                    result[i] = strPar;
                } else {
                    result[i] = URLEncoder.encode(strPar, enc).replace("+", "%20");
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e); //should never happen
            }
        }
        return result;
    }


    /**
     * Encoding to be used for URL fragments encoding.
     * Default is utf8.
     * {@code null} means no encoding
     *
     * @return
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Encoding to be used for URL fragments encoding.
     * Default is utf8.
     * {@code null} means no encoding
     *
     * @param encoding
     * @throws UnsupportedEncodingException
     * @see #getEncoding()
     */
    public void setEncoding(String encoding) {
        if(encoding != null) {
            Charset.forName(encoding);
        }
        this.encoding = encoding;
    }

    private void executeHttpMethod(HttpMethod method, ResultInfo resultInfo) {
        switch (sessionStrategy) {
            case NONE:
                executeAndWriteResponse(method, resultInfo, Collections.<Integer>emptySet());
                return;
            case AUTO_LOGIN_LOGOUT:
                login();
                try {
                    executeAndWriteResponse(method, resultInfo, Collections.<Integer>emptySet());
                } finally {
                    logout();
                }
                return;
            case AUTO_LOGIN:
                SessionContext myContext = null;
                synchronized (this) {
                    if (sessionContext == null) {
                        login();
                    } else {
                        myContext = sessionContext;
                    }
                }
                int statusCode = executeAndWriteResponse(method, resultInfo, AUTH_FAIL_STATUSES);
                if (AUTH_FAIL_STATUSES.contains(statusCode)) {
                    synchronized (this) {
                        if(myContext == sessionContext) {
                            // login (unless someone else just did it)
                            login();
                        }
                    }
                    // and try again
                    executeAndWriteResponse(method, resultInfo, Collections.<Integer>emptySet());
                }
        }
    }


    public static void copyStream(InputStream input, OutputStream output)
            throws IOException {
        byte[] buffer = new byte[8192];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
    }


    private int executeAndWriteResponse(HttpMethod method, ResultInfo resultInfo, Set<Integer> doNotWriteForStatuses) {
        try {
            int status = -1;
            // prevent creation of multiple implicit sessions
            // hopefully the first request to come (and fill in the session) will be short
            boolean hasQcSession;
            synchronized (this) {
                hasQcSession = hasQcSessionCookie();
                if(!hasQcSession) {
                    status = httpClient.executeMethod(method);
                }
            }
            if(hasQcSession) {
                status = httpClient.executeMethod(method);
            }
            writeResponse(resultInfo, method, !doNotWriteForStatuses.contains(status));
            return status;
        } catch (IOException e) {
            throw new ResourceAccessException(e);
        } finally {
            method.releaseConnection();
        }
    }

    private boolean hasQcSessionCookie() {
        for(Cookie cookie: httpClient.getState().getCookies()) {
            if(COOKIE_SESSION_NAME.equals(cookie.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Session context created during {@link #login()}  or supplied with {@link #use(SessionContext, String, String, String, String)}.
     * Allows retrieve session context for future reuse.
     * Session context can be created by explicit {@link #login()} call
     * or when {@link SessionStrategy#AUTO_LOGIN} or {@link SessionStrategy#AUTO_LOGIN_LOGOUT)
     * strategy is used and login is invoked implicitely.
     * <p/>
     * NOTE: when instance of session context was changed (== identity comparison) from supplied by {@link #use(SessionContext, String, String, String, String)}
     * than spupplied session expired and automatic login was performed.
     *
     * @return session context
     */
    public SessionContext getSessionContext() {
        return sessionContext;
    }


    /**
     * Sets current domain.
     *
     * @param domain
     */
    public void setDomain(String domain) {
        domainProject.setDomain(domain);
    }

    /**
     * Sets current project
     *
     * @param project
     */
    public void setProject(String project) {
        domainProject.setProject(project);
    }

    public String getDomain() {
        return domainProject.getSnapshot().domain;
    }

    public String getProject() {
        return domainProject.getSnapshot().project;
    }

    public List<String> listDomains() {
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        ResultInfo resultInfo = ResultInfo.create(false, responseBody);
        GetMethod method = createMethod(new DomainProjectPair(null, null), GET_BUILDER, null, "domains");
        executeHttpMethod(method, resultInfo);
        return getAttributeValues(new ByteArrayInputStream(responseBody.toByteArray()), "Domain", "Name");
    }

    public List<String> listCurrentProjects() {
        return listProjects(domainProject.getSnapshot().domain);
    }


    public List<String> listProjects(String domain) {
        if (domain == null) throw new IllegalArgumentException("domain==null");
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        ResultInfo resultInfo = ResultInfo.create(false, responseBody);

        GetMethod method = createMethod(new DomainProjectPair(domain, null), GET_BUILDER, null, "projects");
        executeHttpMethod(method, resultInfo);
        return getAttributeValues(new ByteArrayInputStream(responseBody.toByteArray()), "Project", "Name");
    }


    private List<String> getAttributeValues(InputStream xml, String elemName, String attrName) {
        try {
            XMLInputFactory factory = XmlUtils.createBasicInputFactory();
            XMLStreamReader parser;
            parser = factory.createXMLStreamReader(xml);
            List<String> result = new LinkedList<String>();
            while (true) {
                int event = parser.next();
                if (event == XMLStreamConstants.END_DOCUMENT) {
                    parser.close();
                    break;
                }
                if (event == XMLStreamConstants.START_ELEMENT && elemName.equals(parser.getLocalName())) {

                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        String localName = parser.getAttributeLocalName(i);
                        if (attrName.equals(localName)) {
                            result.add(parser.getAttributeValue(i));
                            break;
                        }
                    }
                }
            }
            return result;
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                xml.close();
            } catch (IOException e) {
                // ignore
            }

        }

    }
}
