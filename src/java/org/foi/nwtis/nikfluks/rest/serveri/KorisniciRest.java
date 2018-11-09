package org.foi.nwtis.nikfluks.rest.serveri;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.foi.nwtis.nikfluks.helperi.KorisnikListaj;
import org.foi.nwtis.nikfluks.konfiguracije.bp.BP_Konfiguracija;
import org.foi.nwtis.nikfluks.slusaci.SlusacAplikacije;

/**
 * REST Web Service
 *
 * @author Nikola
 */
@Path("korisnici")
public class KorisniciRest {

    @Context
    private UriInfo context;
    private String komandaZaSlanje;
    private int portPosluzitelja;
    private String adresaPosluzitelja;

    public KorisniciRest() {
    }

    private boolean dohvatiPodatkeIzKonfiguracije() {
        try {
            BP_Konfiguracija bpk = (BP_Konfiguracija) SlusacAplikacije.getServletContext().getAttribute("BP_Konfig");
            portPosluzitelja = Integer.parseInt(bpk.getPortServera_());
            adresaPosluzitelja = bpk.getAdresaServera_();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String dajPopisSvihKorisnika(@QueryParam("korisnickoIme") String korisnickoIme, @QueryParam("lozinka") String lozinka) {
        komandaZaSlanje = "KORISNIK " + korisnickoIme + "; LOZINKA " + lozinka + "; LISTAJ;";
        String popisKorisnika = posaljiKomandu();
        String odgovor = "";
        if (popisKorisnika != null) {
            if (popisKorisnika.contains("ERR")) {
                odgovor = "{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod dohvaćanja korisnika!\"}";
            } else {
                //rezem van OK 10;
                popisKorisnika = popisKorisnika.substring(7, popisKorisnika.length());
                odgovor = "{\"odgovor\":" + popisKorisnika + ", \"status\": \"OK\"}";
            }
        } else {
            odgovor = "{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod dohvaćanja korisnika!\"}";
        }
        return odgovor;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{korisnickoIme}")
    public String preuzmiJednogKorisnika(@PathParam("korisnickoIme") String korisnickoIme, @QueryParam("korIme") String korIme,
            @QueryParam("lozinka") String lozinka) {
        String sviKorisnici = dajPopisSvihKorisnika(korIme, lozinka);
        String odgovor = "";
        boolean postoji = false;
        JsonObject jsonSadrzaj = new JsonParser().parse(sviKorisnici).getAsJsonObject();
        if (jsonSadrzaj.get("poruka") != null) {
            odgovor = "{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod dohvaćanja korisnika!\"}";
        } else {
            List<KorisnikListaj> korisniciJson = new Gson().fromJson(jsonSadrzaj.getAsJsonArray("odgovor"), new TypeToken<List<KorisnikListaj>>() {
            }.getType());
            for (KorisnikListaj k : korisniciJson) {
                if (k.getKi().equals(korisnickoIme)) {
                    String korisnik = new Gson().toJson(k);
                    odgovor = "{\"odgovor\": [" + korisnik + "], \"status\": \"OK\"}";
                    postoji = true;
                    break;
                }
            }
            if (!postoji) {
                odgovor = "{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Korisnik ne postoji!\"}";
            }
        }
        return odgovor;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{korisnickoIme}")
    public String autenticirajKorisnika(@PathParam("korisnickoIme") String korisnickoIme, @QueryParam("lozinka") String lozinka) {
        komandaZaSlanje = "KORISNIK " + korisnickoIme + "; LOZINKA " + lozinka + ";";
        String autenticiraniKorisnik = posaljiKomandu();
        String odgovor = "";
        if (autenticiraniKorisnik != null) {
            if (autenticiraniKorisnik.contains("ERR")) {
                odgovor = "{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod dohvaćanja korisnika!\"}";
            } else {
                String korisnik = preuzmiJednogKorisnika(korisnickoIme, korisnickoIme, lozinka);
                JsonObject jsonSadrzaj = new JsonParser().parse(korisnik).getAsJsonObject();
                KorisnikListaj k = new Gson().fromJson(jsonSadrzaj.getAsJsonArray("odgovor").get(0), KorisnikListaj.class);

                odgovor = "{\"odgovor\": " + new Gson().toJson(k) + ", \"status\": \"OK\"}";
            }
        } else {
            odgovor = "{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod dohvaćanja korisnika!\"}";
        }
        System.out.println("odg: " + odgovor);
        return odgovor;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{korisnickoIme}")
    public String dodajJednogKorisnika(@PathParam("korisnickoIme") String korisnickoIme, @QueryParam("lozinka") String lozinka,
            String korisnik) {
        JsonObject jsonSadrzaj = new JsonParser().parse(korisnik).getAsJsonObject();
        KorisnikListaj k = new Gson().fromJson(jsonSadrzaj, KorisnikListaj.class);

        komandaZaSlanje = "KORISNIK " + korisnickoIme + "; LOZINKA " + lozinka
                + "; DODAJ " + k.getPrezime() + " " + k.getIme() + ";";
        String dodaniKorisnik = posaljiKomandu();
        String odgovor = "";
        if (dodaniKorisnik != null) {
            if (dodaniKorisnik.contains("ERR")) {
                odgovor = "{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod dodavanja korisnika!\"}";
            } else {
                odgovor = "{\"odgovor\": [], \"status\": \"OK\"}";
            }
        } else {
            odgovor = "{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod dodavanja korisnika!\"}";
        }
        System.out.println("odg: " + odgovor);
        return odgovor;
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{korisnickoIme}")
    public String azurirajJednogKorisnika(@PathParam("korisnickoIme") String korisnickoIme, @QueryParam("lozinka") String lozinka,
            String korisnik) {
        JsonObject jsonSadrzaj = new JsonParser().parse(korisnik).getAsJsonObject();
        KorisnikListaj k = new Gson().fromJson(jsonSadrzaj, KorisnikListaj.class);

        komandaZaSlanje = "KORISNIK " + korisnickoIme + "; LOZINKA " + lozinka
                + "; AZURIRAJ " + k.getPrezime() + " " + k.getIme() + ";";
        String dodaniKorisnik = posaljiKomandu();
        String odgovor = "";
        if (dodaniKorisnik != null) {
            if (dodaniKorisnik.contains("ERR")) {
                odgovor = "{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod ažuriranja korisnika!\"}";
            } else {
                odgovor = "{\"odgovor\": [], \"status\": \"OK\"}";
            }
        } else {
            odgovor = "{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod ažuriranja korisnika!\"}";
        }
        System.out.println("odg: " + odgovor);
        return odgovor;
    }

    private String posaljiKomandu() {
        System.out.println("komandaZaSlanje: " + komandaZaSlanje);
        try {
            if (dohvatiPodatkeIzKonfiguracije()) {
                Socket socket = new Socket(adresaPosluzitelja, portPosluzitelja);
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();
                os.write(komandaZaSlanje.getBytes());
                os.flush();
                socket.shutdownOutput();

                StringBuilder odgovor = new StringBuilder();
                int znak;
                BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8.displayName()));
                while ((znak = in.read()) != -1) {
                    odgovor.append((char) znak);
                }
                System.out.println("Odgovor: " + odgovor);
                in.close();
                is.close();
                return odgovor.toString();
            } else {
                System.out.println("Greška kod dohvaćanja podataka iz konf dat!");
            }
        } catch (IOException ex) {
            System.err.println("Greska kod slanja komande kroz socket: " + ex.getLocalizedMessage());
        }
        return null;
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public String azuriraj(String podaci) {
        return "{\"odgovor\": [], "
                + "\"status\": \"ERR\", \"poruka\": \"Nije dozvoljeno\"}";
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public String brisi() {
        return "{\"odgovor\": [], "
                + "\"status\": \"ERR\", \"poruka\": \"Nije dozvoljeno\"}";
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String postJson(String podaci) {
        return "{\"odgovor\": [], "
                + "\"status\": \"ERR\", \"poruka\": \"Nije dozvoljeno\"}";
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{korisnickoIme}")
    public String brisi(@PathParam("korisnickoIme") String korisnickoIme) {
        return "{\"odgovor\": [], "
                + "\"status\": \"ERR\", \"poruka\": \"Nije korišteno\"}";
    }
}
