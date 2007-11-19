package org.jsnap.request;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.exception.JSnapException;
import org.jsnap.exception.comm.CommunicationException;
import org.jsnap.exception.comm.MalformedRequestException;
import org.jsnap.exception.comm.MalformedResponseException;
import org.jsnap.exception.comm.RejectedRequestException;
import org.jsnap.exception.db.ResultSetException;
import org.jsnap.response.Response;

public class SizePackedRequest extends DelimitedPacking {
	private static final long serialVersionUID = -3934497429720634376L;

	private static final byte REQUEST_FLAG = 0x01;
	private static final byte RESPONSE_FLAG = 0x02;
	private static final byte EXCEPTION_FLAG = 0x04;
	private static final byte REJECTED_FLAG = 0x08;
	private static final byte ZIPPED_FLAG = 0x40;

	private static final int BUFFER_SIZE = 1024; // 1KB.

	protected final String host;
	protected final int port;

	public SizePackedRequest() { 	// Default constructor required 
		super();					// by the Listener class.
		this.host = null;
		this.port = 0;
	}

	// Clients should use this constructor to create an instance of SizePackedRequest.
	public SizePackedRequest(String host, int port) throws IOException {
		this.host = host;
		this.port = port;
	}

	private SizePackedRequest(long acceptedOn, Socket s) {
		super(acceptedOn, s);
		this.host = s.getInetAddress().getHostAddress();
		this.port = s.getPort();
	}

	protected Socket open() throws CommunicationException {
		try {
			return new Socket(host, port);
		} catch (IOException e) {
			throw new CommunicationException(e);
		}
	}

	public SizePackedRequest doCreate(long acceptedOn, Socket sock) throws CommunicationException, MalformedRequestException {
		SizePackedRequest request = new SizePackedRequest(acceptedOn, sock);
		byte[] packed = request.read();
		request.unpack(packed);
		return request;
	}

	public void doReject(Socket sock) {
		try {
			OutputStream os = sock.getOutputStream();
			os.write(REJECTED_FLAG);
		} catch (IOException ignore) {
			// Hard to do anything at this point, just ignore.
		}
	}

	public void processResponse(Response resp) throws CommunicationException, ResultSetException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
		baos.write(RESPONSE_FLAG); // Place flag byte at the beginning.
		resp.asBytes(baos);
		byte[] data = baos.toByteArray();
		write(data);
	}

	public void processException(JSnapException ex) {
		ex.log();
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
			baos.write(EXCEPTION_FLAG); // Place flag byte at the beginning.
			ex.asBytes(baos);
			byte[] packed = baos.toByteArray();
			write(packed);
		// Hard to do handle an exception while already handling
		// another one. Exceptions are simply ignored.
		} catch (CommunicationException e) {
			e.log(); // logged and skipped.
		} catch (IOException e) {
			new CommunicationException(e).log(); // logged and skipped.
		}
	}

	public byte[] receive() throws JSnapException {
		remoteException = false;
		byte[] bytes = read();
		try {
			s.close();
		} catch (IOException e) {
			Logger.getLogger(SizePackedRequest.class).log(Level.DEBUG, "Ignored an IOException during socket close", e);
		}
		if ((bytes[0] & RESPONSE_FLAG) != 0) {
			byte[] response = new byte[bytes.length - 1];
			System.arraycopy(bytes, 1, response, 0, bytes.length - 1);
			return response;
		} else if ((bytes[0] & EXCEPTION_FLAG) != 0) {
			try {
				remoteException = true;
				throw JSnapException.create(bytes, 1);
			} catch (IOException e) {
				remoteException = false;
				throw new CommunicationException(e);
			}
		} else if ((bytes[0] & REJECTED_FLAG) != 0) {
			throw new RejectedRequestException();
		} else {
			throw new MalformedResponseException("Flag byte does not indicate a response (" + Byte.toString(bytes[0]) + ")");
		}
	}

	// This variable and function is meaningful on the client side. Client should
	// check whether a JRepException thrown by receive() is raised by the client
	// itself or not. When the exception is not local, it means that the exception
	// had been raised on the server.
	private boolean remoteException;

	public boolean isLocalException() {
		return (remoteException == false);
	}

	protected byte[] read() throws CommunicationException {
		try {
			InputStream is = s.getInputStream();
			DataInputStream dis = new DataInputStream(is);
			int length = dis.readInt();
			byte[] data = new byte[length], packed;
			dis.readFully(data);
			boolean doUnzip = ((data[0] & ZIPPED_FLAG) != 0);
			if (doUnzip) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
				baos.write(data[0]); // Do not change the flag byte.
				GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(data, 1, length - 1));
				byte[] buffer = new byte[BUFFER_SIZE];
				int read;
				while ((read = gzis.read(buffer)) > 0)
					baos.write(buffer, 0, read);
				packed = baos.toByteArray();
			} else {
				packed = data;
			}
			return packed;
		} catch (IOException e) {
			throw new CommunicationException(e);
		}
	}

	protected void write(byte[] packed) throws CommunicationException {
		int dataLength = packed.length - 1; // Exclude flag byte from data length.
		byte backupByte = packed[0];
		boolean doZip = false;
		if (zip > 0 && dataLength >= zip) {	// Data (excluding the flag byte) will
			packed[0] |= ZIPPED_FLAG;		// be zipped when on the wire.
			doZip = true;
		}
		try {
			byte[] data;
			if (doZip) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
				baos.write(packed[0]); // Do not change the flag byte.
				GZIPOutputStream gzos = new GZIPOutputStream(baos);
				gzos.write(packed, 1, dataLength);
				gzos.close();
				data = baos.toByteArray();
			} else {
				data = packed;
			}
			OutputStream os = s.getOutputStream();
			DataOutputStream dos = new DataOutputStream(os);
			dos.writeInt(data.length);
			dos.write(data);
		} catch (IOException e) {
			throw new CommunicationException(e);
		} finally {
			packed[0] = backupByte;
		}
	}

	public byte[] pack() throws MalformedRequestException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
		baos.write(REQUEST_FLAG); // Place flag byte at the beginning.
		byte[] p = super.pack();
		baos.write(p, 0, p.length);
		return baos.toByteArray();
	}

	public void unpack(byte[] packed, int offset, int length) throws MalformedRequestException {
		if ((packed[0] & REQUEST_FLAG) == 0)
			throw new MalformedRequestException("Flag byte does not indicate a request (" + Byte.toString(packed[0]) + ")");		
		super.unpack(packed, offset + 1, length - 1);
	}
}
