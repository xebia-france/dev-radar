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

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Date;

/*
 * Use public field instead of private+getter&setter for immutable fields
 */
public class Event {

    private static final String GRAVATAR_URL = "http://www.gravatar.com/avatar/";
    private static final String DEFAULT_IMAGE = "?d=mm";

    public final long timestamp;
    public final String author;
    public final String message;
    public final String gravatarUrl;
    public final EventLevel level;

    public Event(long timestamp, String author, String message, String email, EventLevel level) {
        this.timestamp = timestamp;
        this.author = author;
        this.message = message;
        this.gravatarUrl = getGravatarUrl(email);
        this.level = level;
    }

    public Event(long timestamp, String author, String message, String email) {
        this(timestamp, author, message, email, EventLevel.UNDIFINED);
    }

    private String getGravatarUrl(String email) {
        if (email == null) {
            email = "";
        }
        StringBuilder builder = new StringBuilder(GRAVATAR_URL);

        builder.append(DigestUtils.md5Hex(email));
        builder.append(DEFAULT_IMAGE);
        return builder.toString();
    }
}
