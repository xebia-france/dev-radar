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
import com.sun.jersey.api.client.ClientHandler;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.config.ClientConfig;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class HudsonFetcherTest {

    private HudsonFetcher fetcher = new HudsonFetcher("http://fluxx.fr.cr:8080/hudson", "dev-radar") {
        @Override
        Client buildJerseyClient(ClientConfig clientConfig) {
            ClientHandler clientHandler = FileClientHandlerBuilder.newFileClientHandler()
                    .withFile("http://fluxx.fr.cr:8080/hudson/user/Nicolas%20Griso/api/json?tree=property%5Baddress%5D", "/hudson/json/hudson-rest-stream-user-ngriso.json")
                    .withFile("http://fluxx.fr.cr:8080/hudson/user/mrenou/api/json?tree=property%5Baddress%5D", "/hudson/json/hudson-rest-stream-user-mrenou.json")
                    .withFile("http://fluxx.fr.cr:8080/hudson/user/nomail/api/json?tree=property%5Baddress%5D", "/hudson/json/hudson-rest-stream-user-nomail.json")
                    .withHeader("Content-Type", "application/javascript; charset=utf-8")
                    .create();
            return new Client(clientHandler, clientConfig);
        }
    };

    @Test(expected = ClientHandlerException.class)
    public void should_throw_a_runtime_exception_if_cannot_get_builds() {
        fetcher.url = getUriFromResourceAsString("/github/json/github-rest-stream-invalid.json");

        fetcher.fetch();
    }

    @Test
    public void should_return_an_empty_set_if_no_builds_exist() {
        fetcher.url = getUriFromResourceAsString("/hudson/json/hudson-rest-stream-0-build.json");

        Set<Event> events = fetcher.fetch();

        assertThat(events.isEmpty(), CoreMatchers.is(true));
    }

    @Test
    public void should_return_success_message_and_level_info_if_build_is_success() {
        fetcher.url = getUriFromResourceAsString("/hudson/json/hudson-rest-stream-1-build-success.json");

        Set<Event> events = fetcher.fetch();

        assertThat(events.size(), CoreMatchers.is(1));
        Event event = events.iterator().next();
        assertThat(event.message, equalTo("Build SUCCESS"));
        assertThat(event.level, equalTo(EventLevel.INFO));
    }

    @Test
    public void should_return_failure_message_and_level_unstable_if_build_is_failure() {
        fetcher.url = getUriFromResourceAsString("/hudson/json/hudson-rest-stream-1-build-unstable.json");

        Set<Event> events = fetcher.fetch();

        assertThat(events.size(), CoreMatchers.is(1));
        Event event = events.iterator().next();
        assertThat(event.message, equalTo("Build UNSTABLE"));
        assertThat(event.level, equalTo(EventLevel.WARNING));
    }

    @Test
    public void should_return_failure_message_and_level_error_if_build_is_failure() {
        fetcher.url = getUriFromResourceAsString("/hudson/json/hudson-rest-stream-1-build-failure.json");

        Set<Event> events = fetcher.fetch();

        assertThat(events.size(), CoreMatchers.is(1));
        Event event = events.iterator().next();
        assertThat(event.message, equalTo("Build FAILURE"));
        assertThat(event.level, equalTo(EventLevel.ERROR));
    }

    @Test
    public void should_return_1_event_set_if_1_build_exists_without_user() {
        fetcher.url = getUriFromResourceAsString("/hudson/json/hudson-rest-stream-1-build-without-user.json");

        Set<Event> events = fetcher.fetch();

        assertThat(events.size(), CoreMatchers.is(1));
        Event event = events.iterator().next();
        assertThat(event.timestamp, equalTo(1295779740666L));
        assertThat(event.author, equalTo("anonymous"));
        assertThat(event.message, equalTo("Build SUCCESS"));
        assertThat(event.gravatarUrl, equalTo("http://www.gravatar.com/avatar/d41d8cd98f00b204e9800998ecf8427e?d=mm"));
    }

    @Test
    public void should_return_1_event_set_if_1_build_exists_with_1_user() {
        fetcher.url = getUriFromResourceAsString("/hudson/json/hudson-rest-stream-1-build-1-user.json");

        Set<Event> events = fetcher.fetch();

        assertThat(events.size(), CoreMatchers.is(1));
        Event event = events.iterator().next();
        assertThat(event.timestamp, equalTo(1295779740666L));
        assertThat(event.author, equalTo("Nicolas Griso"));
        assertThat(event.message, equalTo("Build SUCCESS"));
        assertThat(event.gravatarUrl, equalTo("http://www.gravatar.com/avatar/4a89258a4759e47dab3266e9b9d76065?d=mm"));
    }

    @Test
    public void should_return_1_event_set_if_1_build_exists_with_1_user_without_mail() {
        fetcher.url = getUriFromResourceAsString("/hudson/json/hudson-rest-stream-1-build-1-user-without-mail.json");

        Set<Event> events = fetcher.fetch();

        assertThat(events.size(), CoreMatchers.is(1));
        Event event = events.iterator().next();
        assertThat(event.timestamp, equalTo(1295779740666L));
        assertThat(event.author, equalTo("nomail"));
        assertThat(event.message, equalTo("Build SUCCESS"));
        assertThat(event.gravatarUrl, equalTo("http://www.gravatar.com/avatar/d41d8cd98f00b204e9800998ecf8427e?d=mm"));
    }

    @Test
    public void should_return_2_events_set_if_1_build_exists_with_2_user() {
        fetcher.url = getUriFromResourceAsString("/hudson/json/hudson-rest-stream-1-build-2-users.json");

        List<Event> events  = new ArrayList<Event>(fetcher.fetch());
        Collections.sort(events, new Comparator<Event>() {
            @Override
            public int compare(Event event1, Event event2) {
                return event1.author.compareTo(event2.author);
            }
        });

        assertThat(events.size(), CoreMatchers.is(2));

        Event event1 = events.get(0);
        assertThat(event1.timestamp, equalTo(1295779740666L));
        assertThat(event1.author, equalTo("Nicolas Griso"));
        assertThat(event1.message, equalTo("Build SUCCESS"));
        assertThat(event1.gravatarUrl, equalTo("http://www.gravatar.com/avatar/4a89258a4759e47dab3266e9b9d76065?d=mm"));

        Event event2 = events.get(1);
        assertThat(event2.timestamp, equalTo(1295779740666L));
        assertThat(event2.author, equalTo("mrenou"));
        assertThat(event2.message, equalTo("Build SUCCESS"));
        assertThat(event2.gravatarUrl, equalTo("http://www.gravatar.com/avatar/8c92fcdb7c7abc1a50732a93bc361b5e?d=mm"));
    }

    @Test
    public void should_return_2_events_set_if_2_build_exists() {
        fetcher.url = getUriFromResourceAsString("/hudson/json/hudson-rest-stream-2-builds.json");

        List<Event> events  = new ArrayList<Event>(fetcher.fetch());
        Collections.sort(events, new Comparator<Event>() {
            @Override
            public int compare(Event event1, Event event2) {
                return event1.author.compareTo(event2.author);
            }
        });

        assertThat(events.size(), CoreMatchers.is(2));

        Event event1 = events.get(0);
        assertThat(event1.timestamp, equalTo(1295779740666L));
        assertThat(event1.author, equalTo("Nicolas Griso"));
        assertThat(event1.message, equalTo("Build SUCCESS"));
        assertThat(event1.gravatarUrl, equalTo("http://www.gravatar.com/avatar/4a89258a4759e47dab3266e9b9d76065?d=mm"));

        Event event2 = events.get(1);
        assertThat(event2.timestamp, equalTo(1295779740999L));
        assertThat(event2.author, equalTo("mrenou"));
        assertThat(event2.message, equalTo("Build FAILURE"));
        assertThat(event2.gravatarUrl, equalTo("http://www.gravatar.com/avatar/8c92fcdb7c7abc1a50732a93bc361b5e?d=mm"));
    }

    @Test
    public void should_not_return_event_if_build_is_building() {
        fetcher.url = getUriFromResourceAsString("/hudson/json/hudson-rest-stream-2-builds-1-building.json");

        Set<Event> events  = fetcher.fetch();

        assertThat(events.size(), CoreMatchers.is(1));

        Event event1 = events.iterator().next();
        assertThat(event1.timestamp, equalTo(1295779740666L));
        assertThat(event1.author, equalTo("Nicolas Griso"));
        assertThat(event1.message, equalTo("Build SUCCESS"));
        assertThat(event1.gravatarUrl, equalTo("http://www.gravatar.com/avatar/4a89258a4759e47dab3266e9b9d76065?d=mm"));
    }

    @Test
    public void should_return_event_with_user_who_launch_build_by_gui() {
        fetcher.url = getUriFromResourceAsString("/hudson/json/hudson-rest-stream-1-build-1-gui-user.json");

        Set<Event> events  = fetcher.fetch();

        assertThat(events.size(), CoreMatchers.is(1));

        Event event1 = events.iterator().next();
        assertThat(event1.timestamp, equalTo(1295779740666L));
        assertThat(event1.author, equalTo("Nicolas Griso"));
        assertThat(event1.message, equalTo("Build SUCCESS"));
        assertThat(event1.gravatarUrl, equalTo("http://www.gravatar.com/avatar/4a89258a4759e47dab3266e9b9d76065?d=mm"));
    }

    @Test
    public void should_return_event_with_commiter_and_user_who_launch_build_by_gui() {
        fetcher.url = getUriFromResourceAsString("/hudson/json/hudson-rest-stream-1-build-1-user-1-gui-user.json");

        List<Event> events  = new ArrayList<Event>(fetcher.fetch());
        Collections.sort(events, new Comparator<Event>() {
            @Override
            public int compare(Event event1, Event event2) {
                return event1.author.compareTo(event2.author);
            }
        });

        assertThat(events.size(), CoreMatchers.is(2));

        Event event1 = events.get(0);
        assertThat(event1.timestamp, equalTo(1295779740666L));
        assertThat(event1.author, equalTo("Nicolas Griso"));
        assertThat(event1.message, equalTo("Build SUCCESS"));
        assertThat(event1.gravatarUrl, equalTo("http://www.gravatar.com/avatar/4a89258a4759e47dab3266e9b9d76065?d=mm"));

        Event event2 = events.get(1);
        assertThat(event2.timestamp, equalTo(1295779740666L));
        assertThat(event2.author, equalTo("mrenou"));
        assertThat(event2.message, equalTo("Build SUCCESS"));
        assertThat(event2.gravatarUrl, equalTo("http://www.gravatar.com/avatar/8c92fcdb7c7abc1a50732a93bc361b5e?d=mm"));
    }


    @Test
    public void should_return_n_events() {
        fetcher.url = getUriFromResourceAsString("/hudson/json/hudson-rest-stream-n-builds.json");

        Set<Event> events  = fetcher.fetch();

        assertThat(events.size(), CoreMatchers.is(11));

    }

    private String getUriFromResourceAsString(String path) {
        try {
            return getClass().getResource(path).toURI().toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}


