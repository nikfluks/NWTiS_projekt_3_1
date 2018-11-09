package org.foi.nwtis.nikfluks.slusaci;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.foi.nwtis.nikfluks.konfiguracije.bp.BP_Konfiguracija;

@WebListener
public class SlusacAplikacije implements ServletContextListener {

    private static ServletContext servletContext;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8.displayName()));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8.displayName()));
        } catch (UnsupportedEncodingException ex) {
            System.err.println("Greska kod setOut i setErr: " + ex.getLocalizedMessage());
        }
        servletContext = sce.getServletContext();

        String datoteka = servletContext.getInitParameter("konfiguracija");
        String putanja = servletContext.getRealPath("/WEB-INF") + java.io.File.separator;
        BP_Konfiguracija bpk = new BP_Konfiguracija(putanja + datoteka);
        servletContext.setAttribute("BP_Konfig", bpk);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        servletContext.removeAttribute("BP_Konfig");
    }

    public static ServletContext getServletContext() {
        return servletContext;
    }

}
