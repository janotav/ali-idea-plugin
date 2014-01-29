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

import com.hp.alm.ali.rest.client.exception.AuthenticationFailureException;
import com.hp.alm.ali.rest.client.exception.HttpStatusBasedException;
import com.hp.alm.ali.rest.client.filter.Filter;
import com.hp.alm.ali.rest.client.filter.IdentityFilter;
import com.hp.alm.ali.rest.client.filter.IssueTicketFilter;
import com.hp.alm.ali.rest.client.filter.ResponseFilter;
import com.hp.alm.ali.utils.XmlUtils;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.*;

import static com.hp.alm.ali.utils.PathUtils.pathJoin;

/**
 * Thin wrapper around commons-http client that provides basic support for communication with the HP ALM REST.
 * <p>
 *
 * No higher level abstractions are currently provided, this library only simplifies following tasks:
 *
 * <ul>
 *     <li>authentication; {@link #login()}, {@link #logout()}, {@link SessionStrategy}</li>
 *     <li>domain/project listing: {@link #listDomains()}, {@link #listCurrentProjects()}</li>
 * </ul>
 *
 * <h3>URL composition</h3>
 *
 * Position based expansion is used in methods that accept template with parameters:

 * <pre>
 *     client.getForString("defects/{0}/attachments/{1}", 1001, "readme.txt")
 * </pre>
 *
 * In the DOMAIN/PROJECT of http://localhost:8080/qcbin expands to:
 *
 * <pre>
 *     http://localost:8080/qcbin/domains/DOMAIN/projects/PROJECT/defects/1001/attachments/readme.txt
 * </pre>
 */
public class AliRestClient implements RestClient {

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

    private LinkedList<ResponseFilter> responseFilters;

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

    static final String COOKIE_SSO_NAME = "LWSSO_COOKIE_KEY";
    static final String COOKIE_SESSION_NAME = "QCSession";

    static final String CLIENT_TYPE = "ALI_IDEA_plugin";

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

        responseFilters = new LinkedList<ResponseFilter>();
        responseFilters.add(new IssueTicketFilter());
    }

    private void validateProjectAndDomain(String domain, String project) {
        if (domain == null && project != null) {
            throw new IllegalArgumentException("When project is specified domain must be specified too.");
        }
    }

    @Override
    public void setTimeout(int timeout) {
        httpClient.getParams().setSoTimeout(timeout);
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
    }

    @Override
    public void setHttpProxy(String proxyHost, int proxyPort) {
        httpClient.getHostConfiguration().setProxy(proxyHost, proxyPort);
    }

    @Override
    public void setHttpProxyCredentials(String username, String password) {
        Credentials cred = new UsernamePasswordCredentials(username, password);
        AuthScope scope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
        httpClient.getState().setProxyCredentials(scope, cred);
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public SessionStrategy getSessionStrategy() {
        return sessionStrategy;
    }

    @Override
    public void setEncoding(String encoding) {
        if(encoding != null) {
            Charset.forName(encoding);
        }
        this.encoding = encoding;
    }

    @Override
    public void setDomain(String domain) {
        validateProjectAndDomain(domain, project);
        this.domain = domain;
    }

    @Override
    public void setProject(String project) {
        validateProjectAndDomain(domain, project);
        this.project = project;
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    public String getProject() {
        return project;
    }

    @Override
    public synchronized void login() {
        // exclude the NTLM authentication scheme (requires NTCredentials we don't supply)
        List<String> authPrefs = new ArrayList<String>(2);
        authPrefs.add(AuthPolicy.DIGEST);
        authPrefs.add(AuthPolicy.BASIC);
        httpClient.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);

        // first try Apollo style login
        String authPoint = pathJoin("/", location, "/authentication-point/alm-authenticate");
        String authXml = createAuthXml();
        PostMethod post = initPostMethod(authPoint, authXml);
        ResultInfo resultInfo = ResultInfo.create(null);
        executeAndWriteResponse(post, resultInfo, Collections.<Integer>emptySet());

        if(resultInfo.getHttpStatus() == HttpStatus.SC_NOT_FOUND) {
            // try Maya style login
            Credentials cred = new UsernamePasswordCredentials(userName, password);
            AuthScope scope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
            httpClient.getParams().setParameter(HttpMethodParams.CREDENTIAL_CHARSET, "UTF-8");
            httpClient.getState().setCredentials(scope, cred);

            authPoint = pathJoin("/", location, "/authentication-point/authenticate");
            GetMethod get = new GetMethod(authPoint);
            resultInfo = ResultInfo.create(null);
            executeAndWriteResponse(get, resultInfo, Collections.<Integer>emptySet());
        }
        HttpStatusBasedException.throwForError(resultInfo);
        if(resultInfo.getHttpStatus() != 200) {
            // during login we only accept 200 status (to avoid redirects and such as seemingly correct login)
            throw new AuthenticationFailureException(resultInfo);
        }

        Cookie[] cookies = httpClient.getState().getCookies();
        Cookie ssoCookie = getSessionCookieByName(cookies, COOKIE_SSO_NAME);
        addTenantCookie(ssoCookie);

        //Since ALM 12.00 it is required explicitly ask for QCSession calling "/rest/site-session"
        //For all the rest of HP ALM / AGM versions it is optional
        String siteSessionPoint = pathJoin("/", location, "/rest/site-session");
        String sessionParamXml = createRestSessionXml();
        post = initPostMethod(siteSessionPoint, sessionParamXml);
        resultInfo = ResultInfo.create(null);
        executeAndWriteResponse(post, resultInfo, Collections.<Integer>emptySet());
        //AGM throws 403
        if (resultInfo.getHttpStatus() != HttpStatus.SC_FORBIDDEN) {
            HttpStatusBasedException.throwForError(resultInfo);
        }

        cookies = httpClient.getState().getCookies();
        Cookie qcCookie = getSessionCookieByName(cookies, COOKIE_SESSION_NAME);
        sessionContext = new SessionContext(location, ssoCookie, qcCookie);
    }

    private PostMethod initPostMethod(String restEndPoint, String xml) {
        PostMethod post = new PostMethod(restEndPoint);
        post.setRequestEntity(createRequestEntity(InputData.create(xml)));
        post.addRequestHeader("Content-type", "application/xml");

        return post;
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

    private String createRestSessionXml() {
        Element sessionParamElem = new Element("session-parameters");
        Element clientTypeElem = new Element("client-type");
        sessionParamElem.addContent(clientTypeElem);
        clientTypeElem.setText(CLIENT_TYPE);
        return new XMLOutputter().outputString(sessionParamElem);
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

    @Override
    public synchronized void logout() {
        if(sessionContext != null) {
            GetMethod get = new GetMethod(pathJoin("/", location, "/authentication-point/logout"));
            ResultInfo resultInfo = ResultInfo.create(null);
            executeAndWriteResponse(get, resultInfo, Collections.<Integer>emptySet());
            HttpStatusBasedException.throwForError(resultInfo);
            sessionContext = null;
        }
    }

    @Override
    public String getForString(String template, Object... params) {
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        ResultInfo result = ResultInfo.create(responseBody);
        get(result, template, params);
        HttpStatusBasedException.throwForError(result);
        return responseBody.toString();
    }

    @Override
    public InputStream getForStream(String template, Object... params) {
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        ResultInfo result = ResultInfo.create(responseBody);
        get(result, template, params);
        HttpStatusBasedException.throwForError(result);
        return new ByteArrayInputStream(responseBody.toByteArray());
    }

    @Override
    public int get(ResultInfo result, String template, Object... params) {
        GetMethod method = createMethod(domain, project, GET_BUILDER, null, template, params);
        executeHttpMethod(method, result);
        return result.getHttpStatus();
    }

    @Override
    public int put(InputData inputData, ResultInfo result, String template, Object... params) {
        PutMethod putMethod = createMethod(domain, project, PUT_BUILDER, createRequestEntity(inputData), template, params);
        setHeaders(putMethod, inputData.getHeaders());
        executeHttpMethod(putMethod, result);
        return result.getHttpStatus();
    }

    @Override
    public int delete(ResultInfo result, String template, Object... params) {
        DeleteMethod deleteMethod = createMethod(domain, project, DELETE_BUILDER, null, template, params);
        executeHttpMethod(deleteMethod, result);
        return result.getHttpStatus();
    }

    @Override
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
        return inputData.getRequestEntity(encoding);
    }

    private void writeResponse(ResultInfo result, HttpMethod method, boolean writeBodyAndHeaders) {
        OutputStream bodyStream = result.getBodyStream();

        StatusLine statusLine = method.getStatusLine();
        if (statusLine != null) {
            result.setReasonPhrase(statusLine.getReasonPhrase());
        }
        try {
            result.setLocation(method.getURI().toString());
        } catch (URIException e) {
            throw new RuntimeException(e);
        }
        if (writeBodyAndHeaders) {
            Map<String, String> headersMap = result.getHeaders();
            Header[] headers = method.getResponseHeaders();
            for (Header header : headers) {
                headersMap.put(header.getName(), header.getValue());
            }
        }
        result.setHttpStatus(method.getStatusCode());
        Filter filter = new IdentityFilter(result);
        for(ResponseFilter responseFilter: responseFilters) {
            filter = responseFilter.applyFilter(filter, method, result);
        }
        if (writeBodyAndHeaders && bodyStream != null && method.getStatusCode() != 204) {
            try {
                InputStream responseBody = method.getResponseBodyAsStream();
                if (responseBody != null) {
                    IOUtils.copy(responseBody, filter.getOutputStream());
                    bodyStream.flush();
                    bodyStream.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String composeLocation(String domain, String project, String template, Object... params) {
        String[] strParams = encodeParams(params);
        String substituted = MessageFormat.format(template, strParams);
        Object encDomain = encodeParams(new Object[]{domain})[0];
        Object encProject = encodeParams(new Object[]{project})[0];
        if (encDomain == null) {
            return pathJoin("/", location, "/rest", substituted);
        } else if (encProject == null) {
            return pathJoin("/", location, "/rest/domains", encDomain.toString(), substituted);
        }
        return pathJoin("/", location, "/rest/domains", encDomain.toString(), "projects", encProject.toString(), substituted);
    }

    private String[] encodeParams(Object params[]) {
        String enc = encoding;
        String result[] = new String[params.length];
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

    @Override
    public List<String> listDomains() {
        return listValues(createMethod(null, null, GET_BUILDER, null, "domains"), "Domain");
    }

    @Override
    public List<String> listCurrentProjects() {
        if (domain == null) {
            throw new IllegalStateException("domain==null");
        }
        return listValues(createMethod(domain, null, GET_BUILDER, null, "projects"), "Project");
    }

    private List<String> listValues(GetMethod method, String entity) {
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        ResultInfo resultInfo = ResultInfo.create(responseBody);
        executeHttpMethod(method, resultInfo);
        return getAttributeValues(new ByteArrayInputStream(responseBody.toByteArray()), entity, "Name");
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
