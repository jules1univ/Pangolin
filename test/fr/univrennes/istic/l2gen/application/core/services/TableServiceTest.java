package fr.univrennes.istic.l2gen.application.core.services;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.junit.After;
import org.junit.Before;
import fr.univrennes.istic.l2gen.application.core.config.Config;
import fr.univrennes.istic.l2gen.application.core.services.table.TableService;
import fr.univrennes.istic.l2gen.application.core.table.DataTable;

public class TableServiceTest {

    private final List<File> tempPaths = new ArrayList<>();
    private HttpServer server;
    private File httpTargetDir;

    @Before
    public void resetState() throws Exception {
        clearLoadedTables();
        TableService.getRecentTables().clear();
        Config.clear("settings.startup.recent_tables");
        startServer();
    }

    @After
    public void cleanup() throws Exception {
        if (server != null) {
            server.stop(0);
        }
        clearLoadedTables();
        TableService.getRecentTables().clear();
        Config.clear("settings.startup.recent_tables");
        for (File path : tempPaths) {
            deleteRecursively(path);
        }
        if (httpTargetDir != null) {
            deleteRecursively(httpTargetDir);
        }
    }

    private void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/redirect", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) {
                try {
                    exchange.getResponseHeaders().add("Location",
                            "http://localhost:" + server.getAddress().getPort() + "/data.csv");
                    exchange.sendResponseHeaders(302, -1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    exchange.close();
                }
            }
        });
        server.createContext("/data.csv", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) {
                try {
                    byte[] body = "id,val\n1,10.0\n2,20.0\n".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=remote.csv");
                    exchange.getResponseHeaders().add("Content-Type", "text/csv");
                    exchange.sendResponseHeaders(200, body.length);
                    try (java.io.OutputStream output = exchange.getResponseBody()) {
                        output.write(body);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    exchange.close();
                }
            }
        });
        server.start();
    }

    @SuppressWarnings("unchecked")
    private void clearLoadedTables() throws Exception {
        java.lang.reflect.Field field = TableService.class.getDeclaredField("loaded");
        field.setAccessible(true);
        ((java.util.Map<File, DataTable>) field.get(null)).clear();
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}