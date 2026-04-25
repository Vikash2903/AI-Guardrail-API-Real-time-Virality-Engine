package com.guardrail.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.awt.Desktop;
import java.net.URI;
import java.io.IOException;

@Component
public class OpenBrowserConfig {

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowserAfterStartup() {
        String url = "http://localhost:8080/";
        String os = System.getProperty("os.name").toLowerCase();
        Runtime rt = Runtime.getRuntime();

        try {
            if (os.contains("win")) {
                rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (os.contains("mac")) {
                rt.exec("open " + url);
            } else if (os.contains("nix") || os.contains("nux")) {
                rt.exec("xdg-open " + url);
            }
        } catch (IOException e) {
            System.err.println("Could not open browser automatically: " + e.getMessage());
        }
    }
}
