/*
 * Copyright (c) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.client.googleapis.json;

import com.google.api.client.googleapis.GoogleTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.Json;
import com.google.api.client.util.ArrayMap;
import com.google.api.client.util.Key;

import org.codehaus.jackson.JsonParser;

import java.io.IOException;
import java.util.Map;

/**
 * Manages a JSON-formatted document from the experimental Google Discovery API
 * version 0.1.
 *
 * @since 1.0
 * @author Yaniv Inbar
 */
public final class DiscoveryDocument {

  /**
   * Defines all versions of an API.
   *
   * @since 1.1
   */
  public static final class APIDefinition extends ArrayMap<
      String, ServiceDefinition> {
  }

  /** Defines a specific version of an API. */
  public static final class ServiceDefinition {
    /**
     * Base URL for service endpoint.
     *
     * @since 1.1
     */
    @Key
    public String baseUrl;

    /** Map from the resource name to the resource definition. */
    @Key
    public Map<String, ServiceResource> resources;

    /**
     * Returns {@link ServiceMethod} definition for given method name. Method
     * identifier is of format "resourceName.methodName".
     */
    public ServiceMethod getResourceMethod(String methodIdentifier) {
      int dot = methodIdentifier.indexOf('.');
      String resourceName = methodIdentifier.substring(0, dot);
      String methodName = methodIdentifier.substring(dot + 1);
      ServiceResource resource = this.resources.get(resourceName);
      return resource == null ? null : resource.methods.get(methodName);
    }

    /**
     * Returns url for requested method. Method identifier is of format
     * "resourceName.methodName".
     */
    String getResourceUrl(String methodIdentifier) {
      return baseUrl + getResourceMethod(methodIdentifier).pathUrl;
    }
  }

  /** Defines a resource in a service definition. */
  public static final class ServiceResource {

    /** Map from method name to method definition. */
    @Key
    public Map<String, ServiceMethod> methods;
  }

  /** Defines a method of a service resource. */
  public static final class ServiceMethod {

    /**
     * Path URL relative to base URL.
     *
     * @since 1.1
     */
    @Key
    public String pathUrl;

    /** HTTP method name. */
    @Key
    public String httpMethod;

    /** Map from parameter name to parameter definition. */
    @Key
    public Map<String, ServiceParameter> parameters;

    /**
     * Method type.
     *
     * @since 1.1
     */
    @Key
    public final String methodType = "rest";
  }

  /** Defines a parameter to a service method. */
  public static final class ServiceParameter {

    /** Whether the parameter is required. */
    @Key
    public boolean required;
  }

  /**
   * First API service definition parsed from discovery document.
   *
   * @deprecated (scheduled to be removed in version 1.2) Use for example {@code
   *             get("buzz").get("v1")}
   */
  @Deprecated
  public ServiceDefinition serviceDefinition;

  /** API name. */
  private String apiName;

  /**
   * Definition of all versions defined in this Google API.
   *
   * @since 1.1
   */
  public final APIDefinition apiDefinition = new APIDefinition();

  /**
   * Google transport required by {@link #buildRequest}.
   *
   * @deprecated (scheduled to be removed in version 1.2) Use
   *             {@link GoogleApi#transport}
   */
  @Deprecated
  public HttpTransport transport;

  private DiscoveryDocument() {
  }

  /**
   * Executes a request for the JSON-formatted discovery document based on the
   * <code>0.1</code> version of the API by the given name.
   * <p>
   * Most API's do not have a <code>0.1</code> version, so this will usually
   * fail to return any service documents.
   *
   * @param apiName API name
   * @return discovery document
   * @throws IOException I/O exception executing request
   */
  public static DiscoveryDocument load(String apiName) throws IOException {
    GenericUrl discoveryUrl =
        new GenericUrl("http://www.googleapis.com/discovery/0.1/describe");
    discoveryUrl.put("api", apiName);
    HttpTransport transport = GoogleTransport.create();
    HttpRequest request = transport.buildGetRequest();
    request.url = discoveryUrl;
    JsonParser parser = JsonCParser.parserForResponse(request.execute());
    Json.skipToKey(parser, apiName);
    DiscoveryDocument result = new DiscoveryDocument();
    result.apiName = apiName;
    APIDefinition apiDefinition = result.apiDefinition;
    Json.parseAndClose(parser, apiDefinition, null);
    if (apiDefinition.size() != 0) {
      result.serviceDefinition = apiDefinition.getValue(0);
    }
    return result;
  }

  /**
   * Creates an HTTP request based on the given method name and parameters.
   *
   * @param fullyQualifiedMethodName name of method as defined in Discovery
   *        document of format "resourceName.methodName"
   * @param parameters user defined key / value data mapping
   * @return HTTP request
   * @throws IOException I/O exception reading
   * @deprecated (scheduled to be removed in version 1.2) Use
   *             {@link GoogleApi#buildRequest(String, Object)}
   */
  @Deprecated
  public HttpRequest buildRequest(
      String fullyQualifiedMethodName, Object parameters) throws IOException {
    GoogleApi googleAPI = new GoogleApi();
    googleAPI.transport = this.transport;
    googleAPI.name = this.apiName;
    googleAPI.version = this.apiDefinition.getKey(0);
    googleAPI.serviceDefinition = this.apiDefinition.getValue(0);
    return googleAPI.buildRequest(fullyQualifiedMethodName, parameters);
  }
}