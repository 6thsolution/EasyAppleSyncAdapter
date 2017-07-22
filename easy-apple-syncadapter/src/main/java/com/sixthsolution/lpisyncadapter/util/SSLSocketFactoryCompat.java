/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.sixthsolution.lpisyncadapter.util;

import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import lombok.Cleanup;

public class SSLSocketFactoryCompat extends SSLSocketFactory {

    private SSLSocketFactory delegate;

    // Android 5.0+ (API level21) provides reasonable default settings
    // but it still allows SSLv3
    // https://developer.android.com/reference/javax/net/ssl/SSLSocket.html
    static String protocols[] = null, cipherSuites[] = null;

    static {
        try {
            @Cleanup SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
            if (socket != null) {
                /* set reasonable protocol versions */
                // - enable all supported protocols (enables TLSv1.1 and TLSv1.2 on Android <5.0)
                // - remove all SSL versions (especially SSLv3) because they're insecure now
                List<String> protocols = new LinkedList<>();
                for (String protocol : socket.getSupportedProtocols())
                    if (!protocol.toUpperCase(Locale.US).contains("SSL")) {
                        protocols.add(protocol);
                    }
                SSLSocketFactoryCompat.protocols = protocols.toArray(new String[protocols.size()]);

                /* set up reasonable cipher suites */
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    // choose known secure cipher suites
                    List<String> allowedCiphers = Arrays.asList(
                            // TLS 1.2
                            "TLS_RSA_WITH_AES_256_GCM_SHA384",
                            "TLS_RSA_WITH_AES_128_GCM_SHA256",
                            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                            // maximum interoperability
                            "TLS_RSA_WITH_3DES_EDE_CBC_SHA",
                            "TLS_RSA_WITH_AES_128_CBC_SHA",
                            // additionally
                            "TLS_RSA_WITH_AES_256_CBC_SHA",
                            "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
                            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                            "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
                            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
                    List<String> availableCiphers = Arrays.asList(socket.getSupportedCipherSuites());

                    // take all allowed ciphers that are available and put them into preferredCiphers
                    HashSet<String> preferredCiphers = new HashSet<>(allowedCiphers);
                    preferredCiphers.retainAll(availableCiphers);

                    /* For maximum security, preferredCiphers should *replace* enabled ciphers (thus disabling
                     * ciphers which are enabled by default, but have become unsecure), but I guess for
                     * the security level of DAVdroid and maximum compatibility, disabling of insecure
                     * ciphers should be a server-side task */

                    // add preferred ciphers to enabled ciphers
                    HashSet<String> enabledCiphers = preferredCiphers;
                    enabledCiphers.addAll(new HashSet<>(Arrays.asList(socket.getEnabledCipherSuites())));

                    SSLSocketFactoryCompat.cipherSuites =
                            enabledCiphers.toArray(new String[enabledCiphers.size()]);
                }
            }
        } catch (IOException e) {
            Log.e("SSLSocketFactoryCompat", "Couldn't determine default TLS settings");
            e.printStackTrace();
        }
    }

    public SSLSocketFactoryCompat(@NonNull X509TrustManager trustManager) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new X509TrustManager[] {trustManager}, null);
            delegate = sslContext.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new AssertionError(); // The system has no TLS. Just give up.
        }
    }

    private void upgradeTLS(SSLSocket ssl) {
        if (protocols != null) {
            ssl.setEnabledProtocols(protocols);
        }

        if (cipherSuites != null) {
            ssl.setEnabledCipherSuites(cipherSuites);
        }
    }


    @Override
    public String[] getDefaultCipherSuites() {
        return cipherSuites;
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return cipherSuites;
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        Socket ssl = delegate.createSocket(s, host, port, autoClose);
        if (ssl instanceof SSLSocket) {
            upgradeTLS((SSLSocket) ssl);
        }
        return ssl;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket ssl = delegate.createSocket(host, port);
        if (ssl instanceof SSLSocket) {
            upgradeTLS((SSLSocket) ssl);
        }
        return ssl;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        Socket ssl = delegate.createSocket(host, port, localHost, localPort);
        if (ssl instanceof SSLSocket) {
            upgradeTLS((SSLSocket) ssl);
        }
        return ssl;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket ssl = delegate.createSocket(host, port);
        if (ssl instanceof SSLSocket) {
            upgradeTLS((SSLSocket) ssl);
        }
        return ssl;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
            throws IOException {
        Socket ssl = delegate.createSocket(address, port, localAddress, localPort);
        if (ssl instanceof SSLSocket) {
            upgradeTLS((SSLSocket) ssl);
        }
        return ssl;
    }
}
