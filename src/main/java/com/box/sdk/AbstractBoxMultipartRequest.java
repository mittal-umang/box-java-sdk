package com.box.sdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Base class for multipart uploads</p>
 * <p>This class partially implements the HTTP multipart standard in order to upload files to Box. The body of this
 *  * request type cannot be set directly. Instead, it can be modified by adding multipart fields and setting file
 *  * contents. The body of multipart requests will not be logged since they are likely to contain binary data.</p>
 */
abstract class AbstractBoxMultipartRequest extends BoxAPIRequest {
    protected static final String BOUNDARY = "da39a3ee5e6b4b0d3255bfef95601890afd80709";
    private static final BoxLogger LOGGER = BoxLogger.defaultLogger();
    private static final int BUFFER_SIZE = 8192;
    private final StringBuilder loggedRequest = new StringBuilder();
    private final Map<String, String> fields = new HashMap<>();
    private InputStream inputStream;
    private String filename;
    private long fileSize;
    private OutputStream outputStream;
    private UploadFileCallback callback;
    private boolean firstBoundary = true;

    AbstractBoxMultipartRequest(BoxAPIConnection api, URL url) {
        super(api, url, "POST");
        this.addHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
    }

    /**
     * Sets the file contents of this request.
     *
     * @param inputStream a stream containing the file contents.
     * @param filename    the name of the file.
     */
    public void setFile(InputStream inputStream, String filename) {
        this.inputStream = inputStream;
        this.filename = filename;
    }

    /**
     * Sets the file contents of this request.
     *
     * @param inputStream a stream containing the file contents.
     * @param filename    the name of the file.
     * @param fileSize    the size of the file.
     */
    public void setFile(InputStream inputStream, String filename, long fileSize) {
        this.setFile(inputStream, filename);
        this.fileSize = fileSize;
    }

    /**
     * Adds or updates a multipart field in this request.
     *
     * @param key   the field's key.
     * @param value the field's value.
     */
    public void putField(String key, String value) {
        this.fields.put(key, value);
    }

    /**
     * Adds or updates a multipart field in this request.
     *
     * @param key   the field's key.
     * @param value the field's value.
     */
    public void putField(String key, Date value) {
        this.fields.put(key, BoxDateFormat.format(value));
    }

    /**
     * Sets the callback which allows file content to be written on output stream.
     *
     * @param callback the callback which allows file content to be written on output stream.
     * @param filename the size of the file.
     */
    public void setUploadFileCallback(UploadFileCallback callback, String filename) {
        this.callback = callback;
        this.filename = filename;
    }

    /**
     * Sets the SHA1 hash of the file contents of this request.
     * If set, it will ensure that the file is not corrupted in transit.
     *
     * @param sha1 a string containing the SHA1 hash of the file contents.
     */
    public void setContentSHA1(String sha1) {
        this.addHeader("Content-MD5", sha1);
    }

    /**
     * This method is unsupported in BoxMultipartRequest. Instead, the body should be modified via the {@code putField}
     * and {@code setFile} methods.
     *
     * @param stream N/A
     * @throws UnsupportedOperationException this method is unsupported.
     */
    @Override
    public void setBody(InputStream stream) {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is unsupported in BoxMultipartRequest. Instead, the body should be modified via the {@code putField}
     * and {@code setFile} methods.
     *
     * @param body N/A
     * @throws UnsupportedOperationException this method is unsupported.
     */
    @Override
    public void setBody(String body) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void writeBody(HttpURLConnection connection, ProgressListener listener) {
        try {
            connection.setChunkedStreamingMode(0);
            connection.setDoOutput(true);
            this.outputStream = connection.getOutputStream();

            for (Map.Entry<String, String> entry : this.fields.entrySet()) {
                this.writePartHeader(new String[][]{{"name", entry.getKey()}});
                this.writeOutput(entry.getValue());
            }

            this.writePartHeader(new String[][]{{"name", getPartName()}, {"filename", this.filename}},
                getPartContentType(this.filename));

            OutputStream fileContentsOutputStream = this.outputStream;
            if (listener != null) {
                fileContentsOutputStream = new ProgressOutputStream(this.outputStream, listener, this.fileSize);
            }
            if (this.inputStream != null) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int n = this.inputStream.read(buffer);
                while (n != -1) {
                    fileContentsOutputStream.write(buffer, 0, n);
                    n = this.inputStream.read(buffer);
                }
            } else {
                this.callback.writeToStream(this.outputStream);
            }

            if (LOGGER.isDebugEnabled()) {
                this.loggedRequest.append("<File Contents Omitted>");
            }

            this.writeBoundary();
            this.writeOutput("--");
        } catch (IOException e) {
            throw new BoxAPIException("Couldn't connect to the Box API due to a network error.", e);
        }
    }

    protected abstract String getPartName();

    protected abstract String getPartContentType(String filename);

    @Override
    protected void resetBody() throws IOException {
        this.firstBoundary = true;
        this.inputStream.reset();
        this.loggedRequest.setLength(0);
    }

    @Override
    protected String bodyToString() {
        return this.loggedRequest.toString();
    }

    private void writeBoundary() throws IOException {
        if (!this.firstBoundary) {
            this.writeOutput("\r\n");
        }

        this.firstBoundary = false;
        this.writeOutput("--");
        this.writeOutput(BOUNDARY);
    }

    private void writePartHeader(String[][] formData) throws IOException {
        this.writePartHeader(formData, null);
    }

    private void writePartHeader(String[][] formData, String contentType) throws IOException {
        this.writeBoundary();
        this.writeOutput("\r\n");
        this.writeOutput("Content-Disposition: form-data");
        for (String[] part : formData) {
            this.writeOutput("; ");
            this.writeOutput(part[0]);
            this.writeOutput("=\"");
            this.writeOutput(URLEncoder.encode(part[1], "UTF-8"));
            this.writeOutput("\"");
        }

        if (contentType != null) {
            this.writeOutput("\r\nContent-Type: ");
            this.writeOutput(contentType);
        }

        this.writeOutput("\r\n\r\n");
    }

    private void writeOutput(String s) throws IOException {
        this.outputStream.write(s.getBytes(StandardCharsets.UTF_8));
        if (LOGGER.isDebugEnabled()) {
            this.loggedRequest.append(s);
        }
    }
}
