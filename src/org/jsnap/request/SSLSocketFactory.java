package org.jsnap.request;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.io.SocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class SSLSocketFactory implements SocketFactory {
	private static final SSLSocketFactory TRUSTING = new SSLSocketFactory(true);
	private static final SSLSocketFactory VALIDATING = new SSLSocketFactory(false);

	public static SSLSocketFactory getTrusting() {
		return TRUSTING;
	}

	public static SSLSocketFactory getValidating() {
		return VALIDATING;
	}

	private javax.net.SocketFactory sf;

	private SSLSocketFactory(boolean trustAll) {
		sf = null;
		if (trustAll) {
			// Create a trust manager that does not validate certificate chains.
			TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}
		
					public void checkClientTrusted(X509Certificate[] certs, String authType) {
					}
		
					public void checkServerTrusted(X509Certificate[] certs, String authType) {
					}
				}
			};
			try {
				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, trustAllCerts, null);
				sf = sc.getSocketFactory();
			} catch (NoSuchAlgorithmException e) {
				Logger.getLogger(SSLSocketFactory.class).log(Level.WARN, "Unable to instantiate SSLSocketFactory", e);
			} catch (KeyManagementException e) {
				Logger.getLogger(SSLSocketFactory.class).log(Level.WARN, "Unable to instantiate SSLSocketFactory", e);
			}
		} else {
			try {
				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, null, null);
				sf = sc.getSocketFactory();
			} catch (NoSuchAlgorithmException e) {
				Logger.getLogger(SSLSocketFactory.class).log(Level.WARN, "Unable to instantiate SSLSocketFactory", e);
			} catch (KeyManagementException e) {
				Logger.getLogger(SSLSocketFactory.class).log(Level.WARN, "Unable to instantiate SSLSocketFactory", e);
			}
		}
	}

	public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpParams params) throws IOException {
		if (sf == null)
			throw new IOException("SSLSocketFactory is not properly initialized");
        Socket s = sf.createSocket(host, port, localAddress, localPort);
        if (params != null) {
        	int timeout = HttpConnectionParams.getConnectionTimeout(params);
        	if (timeout != 0)
        		s.setSoTimeout(timeout);
        }
        return s;
	}

	public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
		return createSocket(host, port, localAddress, localPort, null);
	}

	public Socket createSocket(String host, int port) throws IOException {
		return createSocket(host, port, null, 0);
	}
}
