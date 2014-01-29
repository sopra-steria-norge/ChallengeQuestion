package no.steria.tad.cq.view;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
 
public class IEFilter implements Filter {
 
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse res = (HttpServletResponse) response;
        res.addHeader("X-UA-Compatible", "IE=Edge");
        chain.doFilter(request, response);
    }
 
    @Override
    public void destroy() {
    }
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
}