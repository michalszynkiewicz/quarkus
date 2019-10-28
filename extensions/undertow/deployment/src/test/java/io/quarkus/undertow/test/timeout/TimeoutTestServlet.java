package io.quarkus.undertow.test.timeout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@WebServlet(urlPatterns = "/timeout")
public class TimeoutTestServlet extends HttpServlet {

    public static final String TIMEOUT_SERVLET = "timeout-servlet";
    public static boolean invoked = false;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String data = readRequestData(req);
            System.out.println("Read " + data); // mstodo remove
            invoked = true;
            String header = req.getHeader("Processing-Time");
            if (header != null) {
                long sleepTime = Long.parseLong(header);
                Thread.sleep(sleepTime);
            }
            resp.getWriter().write(TIMEOUT_SERVLET);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String readRequestData(HttpServletRequest req) throws IOException {
        try (InputStreamReader isReader = new InputStreamReader(req.getInputStream());
                BufferedReader reader = new BufferedReader(isReader)) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
