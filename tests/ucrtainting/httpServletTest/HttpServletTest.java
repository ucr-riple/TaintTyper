package httpServletTest;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpServletTest {
    public void doFilter(ServletRequest request, ServletResponse response ) throws IOException, ServletException {
        response.getOutputStream().println( "   This is the body of a response for " +  ((HttpServletRequest)request).getRequestURI() );
    }
}
