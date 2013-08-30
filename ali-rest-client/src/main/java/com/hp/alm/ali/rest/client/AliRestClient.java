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

import static com.hp.alm.ali.utils.PathUtils.pathJoin;

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
import com.hp.alm.ali.utils.XmlUtils;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
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

    private static final LocationBasedBuilder<PostMethod> POST_BUILDER = new LocationBasedBuilder<PostMethod>() {
        @Override
        public PostMethod build(String location) {
            return new PostMethod(location);
        }
    };
    private static final LocationBasedBuilder<PutMethod> PUT_BUILDER = new LocationBasedBuilder<PutMethod>() {
        @Override
        public PutMethod build(String location) {
            return new PutMethod(location);
        }
    };
    private static final LocationBasedBuilder<GetMethod> GET_BUILDER = new LocationBasedBuilder<GetMethod>() {
        @Override
        public GetMethod build(String location) {
            return new GetMethod(location);
        }
    };
    private static final LocationBasedBuilder<DeleteMethod> DELETE_BUILDER = new LocationBasedBuilder<DeleteMethod>() {
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

    public static enum SessionStrategy {
        /**
         * Perform automatic login when no session is established or is expired.
         * Logout must be called explicitly.
         */
        AUTO_LOGIN,

        /**
         * Both login and logout must be called explicitly.
         */
        NONE
    }

    static final String COOKIE_SSO_NAME = "LWSSO_COOKIE_KEY";
    static final String COOKIE_SESSION_NAME = "QCSession";

    public static final int DEFAULT_CLIENT_TIMEOUT = 30000;

    private final String location;
    private final String userName;
    private final String password;
    private volatile String domain;
    private volatile String project;

    private final SessionStrategy sessionStrategy;
    private final HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
    private volatile SessionContext sessionContext = null;
    private volatile String encoding = "UTF-8";

    private AliRestClient(String location, String domain, String project,
                          String userName, String password,
                          SessionStrategy sessionStrategy) {
        if (location == null) {
            throw new IllegalArgumentException("location==null");
        }
        validateProjectAndDomain(domain, project);
        this.location = location;
        this.userName = userName;
        this.password = password;
        this.domain = domain;
        this.project = project;
        this.sessionStrategy = sessionStrategy;
        setTimeout(DEFAULT_CLIENT_TIMEOUT);
        httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));
    }

    private void validateProjectAndDomain(String domain, String project) {
        if (domain == null && project != null) {
            throw new IllegalArgumentException("When project is specified domain must be specified too.");
        }
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
     * Encoding to be used for URL fragments encoding.
     * {@code null} means no encoding
     * @return encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Encoding to be used for URL fragments encoding.
     * Default is utf8.
     * {@code null} means no encoding
     *
     * @param encoding charset encoding
     * @see #getEncoding()
     */
    public void setEncoding(String encoding) {
        if(encoding != null) {
            Charset.forName(encoding);
        }
        this.encoding = encoding;
    }

    /**
     * Sets current domain.
     * @param domain domain
     */
    public void setDomain(String domain) {
        validateProjectAndDomain(domain, project);
        this.domain = domain;
    }

    /**
     * Sets current project.
     * @param project project
     */
    public void setProject(String project) {
        validateProjectAndDomain(domain, project);
        this.project = project;
    }

    /**
     * Get current domain.
     * @return domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Get current project.
     * @return project
     */
    public String getProject() {
        return project;
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
    public synchronized void login() {
        // exclude the NTLM authentication scheme (requires NTCredentials we don't supply)
        List<String> authPrefs = new ArrayList<String>(2);
        authPrefs.add(AuthPolicy.DIGEST);
        authPrefs.add(AuthPolicy.BASIC);
        httpClient.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);

        // first try Apollo style login
        String authPoint = pathJoin("/", location, "/authentication-point/alm-authenticate");
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

            authPoint = pathJoin("/", location, "/authentication-point/authenticate");
            GetMethod get = new GetMethod(authPoint);
            resultInfo = ResultInfo.create(false, null);
            executeAndWriteResponse(get, resultInfo, Collections.<Integer>emptySet());
        }
        HttpStatusBasedException.throwForError(resultInfo);
        if(resultInfo.getHttpStatus() != 200) {
            // during login we only accept 200 status (to avoid redirects and such as seemingly correct login)
            throw new AuthenticationFailureException(resultInfo.getHttpStatus(), resultInfo.getReasonPhrase(), resultInfo.getLocation());
        }

        Cookie[] cookies = httpClient.getState().getCookies();
        Cookie ssoCookie = getSessionCookieByName(cookies, COOKIE_SSO_NAME);
        Cookie qcCookie = getSessionCookieByName(cookies, COOKIE_SESSION_NAME);
        addTenantCookie(ssoCookie);
        sessionContext = new SessionContext(location, ssoCookie, qcCookie);
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
    public synchronized void logout() {
        if(sessionContext != null) {
            GetMethod get = new GetMethod(pathJoin("/", location, "/authentication-point/logout"));
            ResultInfo resultInfo = ResultInfo.create(false, null);
            executeAndWriteResponse(get, resultInfo, Collections.<Integer>emptySet());
            HttpStatusBasedException.throwForError(resultInfo);
            sessionContext = null;
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
        HttpStatusBasedException.throwForError(result);
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
        HttpStatusBasedException.throwForError(result);
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
        GetMethod method = createMethod(domain, project, GET_BUILDER, null, template, params);
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
        PutMethod putMethod = createMethod(domain, project, PUT_BUILDER, createRequestEntity(inputData), template, params);
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
        HttpStatusBasedException.throwForError(result);
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
        DeleteMethod deleteMethod = createMethod(domain, project, DELETE_BUILDER, null, template, params);
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
        HttpStatusBasedException.throwForError(result);
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
        PostMethod postMethod = createMethod(domain, project, POST_BUILDER, createRequestEntity(data), template, params);
        setHeaders(postMethod, data.getHeaders());
        executeHttpMethod(postMethod, result);
        return result.getHttpStatus();
    }
    
    private void setHeaders(HttpMethod method, Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            method.setRequestHeader(entry.getKey(), entry.getValue());
        }
    }

    private <T extends HttpMethod> T createMethod(String domain, String project, LocationBasedBuilder<T> builder, RequestEntity requestEntity, String template, Object... params) {
        String location = composeLocation(domain, project, template, params);
        T method = builder.build(location);
        if (requestEntity != null) {
            ((EntityEnclosingMethod) method).setRequestEntity(requestEntity);
        }
        return method;
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
            result.setReasonPhrase(statusLine.getReasonPhrase());
        }
        if (writeBodyAndHeaders && bodyStream != null && method.getStatusCode() != 204) {
            try {
                InputStream responseBody = method.getResponseBodyAsStream();
                if (responseBody != null) {
                    IOUtils.copy(method.getResponseBodyAsStream(), bodyStream);
                    bodyStream.flush();
                    bodyStream.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
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
            return pathJoin("/", location, "/rest", substituted);
        } else if (encProject == null) {
            return pathJoin("/", location, "/rest/domains", encDomain.toString(), substituted);
        }
        return pathJoin("/", location, "/rest/domains", encDomain.toString(), "projects", encProject.toString(), substituted);
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

    private boolean tryLogin(ResultInfo resultInfo, HttpMethod method) {
        try {
            login();
            return true;
        } catch (HttpStatusBasedException e) {
            resultInfo.setHttpStatus(e.getHttpStatus());
            resultInfo.setReasonPhrase(e.getReasonPhrase());
            try {
                resultInfo.setLocation(e.getLocation() + " [on-behalf-of: " + method.getURI().toString() + "]");
            } catch (URIException e2) {
                resultInfo.setLocation(e.getLocation() + " [on-behalf-of: " + method.getPath() + "]");
            }
            return false;
        }
    }

    private void executeHttpMethod(HttpMethod method, ResultInfo resultInfo) {
        switch (sessionStrategy) {
            case NONE:
                executeAndWriteResponse(method, resultInfo, Collections.<Integer>emptySet());
                return;
            case AUTO_LOGIN:
                SessionContext myContext = null;
                synchronized (this) {
                    if (sessionContext == null) {
                        if(!tryLogin(resultInfo, method)) {
                            return;
                        }
                    } else {
                        myContext = sessionContext;
                    }
                }
                int statusCode = executeAndWriteResponse(method, resultInfo, AUTH_FAIL_STATUSES);
                if (AUTH_FAIL_STATUSES.contains(statusCode)) {
                    synchronized (this) {
                        if(myContext == sessionContext) {
                            // login (unless someone else just did it)
                            if(!tryLogin(resultInfo, method)) {
                                return;
                            }
                        }
                    }
                    // and try again
                    executeAndWriteResponse(method, resultInfo, Collections.<Integer>emptySet());
                }
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
            throw new RuntimeException(e);
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

    public List<String> listDomains() {
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        ResultInfo resultInfo = ResultInfo.create(false, responseBody);
        GetMethod method = createMethod(null, null, GET_BUILDER, null, "domains");
        executeHttpMethod(method, resultInfo);
        return getAttributeValues(new ByteArrayInputStream(responseBody.toByteArray()), "Domain", "Name");
    }

    public List<String> listCurrentProjects() {
        return listProjects(domain);
    }

    public List<String> listProjects(String domain) {
        if (domain == null) throw new IllegalArgumentException("domain==null");
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        ResultInfo resultInfo = ResultInfo.create(false, responseBody);

        GetMethod method = createMethod(domain, null, GET_BUILDER, null, "projects");
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

    private static interface LocationBasedBuilder<T extends HttpMethod> {

        T build(String location);

    }
}
