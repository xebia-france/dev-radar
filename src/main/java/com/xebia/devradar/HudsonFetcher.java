/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.xebia.devradar;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.json.impl.provider.entity.JSONObjectProvider;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import javax.ws.rs.core.MediaType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *  Fetch a list of commit from github and transform each of them into <code>com.xebia.devradar.Event</code>
 */
public class HudsonFetcher {

    private static final String COM_SUN_JERSEY_API_JSON_POJOMAPPING_FEATURE = "com.sun.jersey.api.json.POJOMappingFeature";

    /**
     * Fetch a set of events from a hudson job specified by an url.
     * @param url
     * @return A set of <code>com.xebia.devradar.Event</code>
     */
    public Set<Event> fetch(String url) {
        ClientConfig clientConfig = new DefaultClientConfig();

        clientConfig.getFeatures().put(COM_SUN_JERSEY_API_JSON_POJOMAPPING_FEATURE, true);
        clientConfig.getClasses();
        Client client = buildJerseyClient(clientConfig);

        ClientFilter clientFilter = new ClientFilter() {
            @Override
            public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
                ClientResponse clientResponse = getNext().handle(cr);
                clientResponse.getHeaders().remove("Content-Type");
                clientResponse.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
                return clientResponse;
            }
        };
        client.addFilter(clientFilter);

        HudsonBuildsDTO hudsonBuildsDTO = getHudsonBuilds(client, url);
        Set<Event> events = new HashSet<Event>();

        for (HudsonBuildDTO hudsonBuildDTO : hudsonBuildsDTO.builds) {
            events.addAll(transformBuildToEvents(client, hudsonBuildDTO));
        }
        return events;
    }

    private Event transformBuildToEvent(HudsonBuildDTO hudsonBuildDTO) {
        return transformBuildToEvent(hudsonBuildDTO, null, null);
    }

    private Event transformBuildToEvent(HudsonBuildDTO hudsonBuildDTO, Client client, HudsonUserDTO hudsonUserDTO) {
        if (hudsonUserDTO == null) {
            return new Event(hudsonBuildDTO.timestamp, null, "Build " + hudsonBuildDTO.result, null);
        }
        return new Event(hudsonBuildDTO.timestamp, hudsonUserDTO.fullName, "Build " + hudsonBuildDTO.result, getUserMail(client, hudsonUserDTO.absoluteUrl));
    }

    private Set<Event> transformBuildToEvents(Client client, HudsonBuildDTO hudsonBuildDTO) {
        Set<Event> events = new HashSet<Event>();
        if (hudsonBuildDTO.culprits.size() == 0) {
            events.add(transformBuildToEvent(hudsonBuildDTO));
        }
        for (HudsonUserDTO hudsonUserDTO : hudsonBuildDTO.culprits) {
            events.add(transformBuildToEvent(hudsonBuildDTO, client, hudsonUserDTO));
        }
        return events;
    }

    private String getUserMail(Client client, String profilUrl) {
        HudsonUserDetailDTO hudsonUserDetailDTO = client.resource(profilUrl + "/api/json").queryParam("tree", "property[address]").get(HudsonUserDetailDTO.class);
        return hudsonUserDetailDTO.property.get(0).address;
    }

    private HudsonBuildsDTO getHudsonBuilds(Client client, String url) {
        return client.resource(url).get(HudsonBuildsDTO.class);
    }

    Client buildJerseyClient(ClientConfig clientConfig) {
        return Client.create(clientConfig);
    }

    /*
     * Hudson API
     */

    private static class HudsonBuildsDTO {
        public List<HudsonBuildDTO> builds;
    }

    private static class HudsonBuildDTO {
        public String result;
        public Long timestamp;
        public List<HudsonUserDTO> culprits;

    }

    private static class HudsonUserDTO {
        public String fullName;
        public String absoluteUrl;
    }

    private static class HudsonUserDetailDTO {
        public List<HudsonUserPropertyDTO> property;
    }

    private static class HudsonUserPropertyDTO {
        public String address;
    }
}