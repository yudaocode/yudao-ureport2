package cn.iocoder.ureport2.compat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 测试用 HTTP 客户端：仅使用 java.net，三条线（Boot2/3/4）通用，不依赖 TestRestTemplate
 *
 * @author 芋道源码
 */
final class HttpTestClient {

    static final class Response {
        final int status;
        final String contentType;
        final byte[] body;

        Response(int status, String contentType, byte[] body) {
            this.status = status;
            this.contentType = contentType;
            this.body = body;
        }

        String text() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    private HttpTestClient() {
    }

    static Response get(int port, String path) throws IOException {
        HttpURLConnection conn = open(port, path);
        conn.setRequestMethod("GET");
        return read(conn);
    }

    static Response postMultipart(int port, String path, String field, String fileName, byte[] content) throws IOException {
        String boundary = "----yudaoUReportBoundary" + System.nanoTime();
        HttpURLConnection conn = open(port, path);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + field + "\"; filename=\"" + fileName + "\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(content);
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }
        return read(conn);
    }

    private static HttpURLConnection open(int port, String path) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        return conn;
    }

    private static Response read(HttpURLConnection conn) throws IOException {
        int status = conn.getResponseCode();
        InputStream in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        if (in != null) {
            try (InputStream is = in) {
                byte[] b = new byte[8192];
                int n;
                while ((n = is.read(b)) != -1) {
                    buf.write(b, 0, n);
                }
            }
        }
        return new Response(status, conn.getContentType(), buf.toByteArray());
    }

}
