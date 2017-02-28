/*----------------------------------------------------------------------
 * Copyright 2017 realglobe Inc.
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
 *----------------------------------------------------------------------*/

package jp.realglobe.sugo.module.android.arducopter;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * TCP/UDP の接続指定。
 * Created by fukuchidaisuke on 16/12/01.
 */
final class Address {

    private static final String LOCAL_REMOTE_SEPARATOR = "<>";

    private final int localPort;
    private final String remoteHost;
    private final int remotePort;

    private Address(int localPort, String remoteHost, int remotePort) {
        this.localPort = localPort;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    static Address parse(String address) {
        final int sep = address.indexOf(LOCAL_REMOTE_SEPARATOR);
        if (sep < 0) {
            return parseSingle(address);
        }

        final Address local = parseSingle(address.substring(0, sep));
        final Address remote = parseSingle(address.substring(sep + LOCAL_REMOTE_SEPARATOR.length()));

        return new Address(local.getLocalPort(), remote.getRemoteHost(), remote.getRemotePort());
    }

    private static Address parseSingle(String address) {
        // ローカルポートかリモートの指定

        try {
            final int localPort = Integer.parseInt(address);
            // ローカルポートの指定だった。
            return new Address(localPort, null, 0);
        } catch (NumberFormatException ignored) {
        }

        // リモートの指定のはず

        final URI uri;
        try {
            uri = new URI("my://" + address);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        if (uri.getHost() == null || uri.getHost().isEmpty()) {
            throw new IllegalArgumentException("No remote host: " + address);
        }
        return new Address(0, uri.getHost(), uri.getPort());
    }

    int getLocalPort() {
        return this.localPort;
    }

    String getRemoteHost() {
        return this.remoteHost;
    }

    int getRemotePort() {
        return this.remotePort;
    }

}
