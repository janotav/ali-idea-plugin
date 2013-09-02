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

import java.io.InputStream;
import java.util.List;

public interface RestClient {

    enum SessionStrategy {
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

    /**
     * Get session strategy
     * @return session strategy
     */
    SessionStrategy getSessionStrategy();

    /**
     * Encoding to be used for URL fragments encoding.
     * Default is utf8.
     * {@code null} means no encoding
     *
     * @param encoding charset encoding
     * @see #getEncoding()
     */
    void setEncoding(String encoding);

    /**
     * Encoding to be used for URL fragments encoding.
     * {@code null} means no encoding
     * @return encoding
     */
    String getEncoding();

    /**
     * Sets the default socket timeout (SO_TIMEOUT) which is the timeout for waiting for data and  the timeout until a connection is etablished.
     * Both timeouts are set to the same value.
     *
     * @param timeout in milliseconds
     */
    void setTimeout(int timeout);

    /**
     * Sets HTTP connection proxy.
     *
     * @param proxyHost proxy host
     * @param proxyPort proxy port
     */
    void setHttpProxy(String proxyHost, int proxyPort);

    /**
     * Sets HTTP proxy credentials
     * @param username proxy username
     * @param password proxy password
     */
    void setHttpProxyCredentials(String username, String password);

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
    void login();

    /**
     * Do logout from ALM server.
     *
     * @throws com.hp.alm.ali.rest.client.exception.HttpClientErrorException
     *          for http statuses 400-499
     * @throws com.hp.alm.ali.rest.client.exception.HttpServerErrorException
     *          for http statuses 500-599
     */
    void logout();

    /**
     * Sets current domain.
     * @param domain domain
     */
    void setDomain(String domain);

    /**
     * Get current domain.
     * @return domain
     */
    String getDomain();

    /**
     * Sets current project.
     * @param project project
     */
    void setProject(String project);

    /**
     * Get current project.
     * @return project
     */
    String getProject();

    List<String> listDomains();

    List<String> listCurrentProjects();

    /**
     * Perform GET HTTP request for given location.
     * Does not throw exception for error status codes.
     *
     * @param result   container for response related information
     * @param template to be expanded and appended to ALM rest base
     * @param params   position based expansion of template
     * @return HTTP status code
     */
    int get(ResultInfo result, String template, Object ... params);

    /**
     * Perform PUT HTTP request for given location.
     *
     * @param inputData input data
     * @param result   container for response related information
     * @param template to be expanded and appended to ALM rest base
     * @param params   position based expansion of template
     * @return HTTP status code
     */
    int put(InputData inputData, ResultInfo result, String template, Object ... params);

    /**
     * Perform POST HTTP request for given location.
     *
     * @param input request body
     * @param result   container for response related information
     * @param template to be expanded and appended to ALM rest base
     * @param params   position based expansion of template
     * @return HTTP status code
     */
    int post(InputData input, ResultInfo result, String template, Object ... params);

    /**
     * Perform DELETE HTTP request for given location.
     *
     * @param result   container for response related information
     * @param template to be expanded and appended to ALM rest base
     * @param params   position based expansion of template
     * @return HTTP status code
     */
    int delete(ResultInfo result, String template, Object ... params);

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
     */
    String getForString(String template, Object ... params);

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
     */
    InputStream getForStream(String template, Object ... params);

}
