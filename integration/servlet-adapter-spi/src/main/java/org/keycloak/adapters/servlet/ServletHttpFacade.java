package org.keycloak.adapters.servlet;

import org.bouncycastle.ocsp.Req;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.util.KeycloakUriBuilder;
import org.keycloak.util.MultivaluedHashMap;
import org.keycloak.util.ServerCookie;
import org.keycloak.util.UriUtils;

import javax.security.cert.X509Certificate;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletHttpFacade implements HttpFacade {
    protected final RequestFacade requestFacade = new RequestFacade();
    protected final ResponseFacade responseFacade = new ResponseFacade();
    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected MultivaluedHashMap<String, String> queryParameters;

    public ServletHttpFacade(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    protected class RequestFacade implements Request {
        @Override
        public String getMethod() {
            return request.getMethod();
        }

        @Override
        public String getURI() {
            StringBuffer buf = request.getRequestURL();
            if (request.getQueryString() != null) {
                buf.append('?').append(request.getQueryString());
            }
            return buf.toString();
        }

        @Override
        public boolean isSecure() {
            return request.isSecure();
        }

        @Override
        public String getFirstParam(String param) {
            return request.getParameter(param);
        }

        @Override
        public String getQueryParamValue(String param) {
            if (queryParameters == null) {
                queryParameters = UriUtils.decodeQueryString(request.getQueryString());
            }
            return queryParameters.getFirst(param);
        }

        public MultivaluedHashMap<String, String> getQueryParameters() {
            if (queryParameters == null) {
                queryParameters = UriUtils.decodeQueryString(request.getQueryString());
            }
            return queryParameters;
        }

        @Override
        public Cookie getCookie(String cookieName) {
            if (request.getCookies() == null) return null;
            javax.servlet.http.Cookie cookie = null;
            for (javax.servlet.http.Cookie c : request.getCookies()) {
                if (c.getName().equals(cookieName)) {
                    cookie = c;
                    break;
                }
            }
            if (cookie == null) return null;
            return new Cookie(cookie.getName(), cookie.getValue(), cookie.getVersion(), cookie.getDomain(), cookie.getPath());
        }

        @Override
        public String getHeader(String name) {
            return request.getHeader(name);
        }

        @Override
        public List<String> getHeaders(String name) {
            Enumeration<String> values = request.getHeaders(name);
            List<String> list = new LinkedList<>();
            while (values.hasMoreElements()) list.add(values.nextElement());
            return list;
        }

        @Override
        public InputStream getInputStream() {
            try {
                return request.getInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String getRemoteAddr() {
            return request.getRemoteAddr();
        }
    }
    public boolean isEnded() {
        return responseFacade.isEnded();
    }

    protected class ResponseFacade implements Response {
        protected boolean ended;

        @Override
        public void setStatus(int status) {
            response.setStatus(status);
        }

        @Override
        public void addHeader(String name, String value) {
            response.addHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value) {
            response.setHeader(name, value);
        }

        @Override
        public void resetCookie(String name, String path) {
            setCookie(name, "", path, null, 0, false, false);
        }

        @Override
        public void setCookie(String name, String value, String path, String domain, int maxAge, boolean secure, boolean httpOnly) {
            StringBuffer cookieBuf = new StringBuffer();
            ServerCookie.appendCookieValue(cookieBuf, 1, name, value, path, domain, null, maxAge, secure, httpOnly);
            String cookie = cookieBuf.toString();
            response.addHeader("Set-Cookie", cookie);
        }

        @Override
        public OutputStream getOutputStream() {
            try {
                return response.getOutputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void sendError(int code, String message) {
            try {
                response.sendError(code, message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void end() {
            ended = true;
        }

        public boolean isEnded() {
            return ended;
        }
    }


    @Override
    public Request getRequest() {
        return requestFacade;
    }

    @Override
    public Response getResponse() {
        return responseFacade;
    }

    @Override
    public X509Certificate[] getCertificateChain() {
        throw new IllegalStateException("Not supported yet");
    }
}
