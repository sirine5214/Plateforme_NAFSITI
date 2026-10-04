package tn.esprit.pi.nefsiti.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import tn.esprit.pi.nefsiti.temps_reel.TempsReelHandler;

import java.util.Arrays;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final TempsReelHandler handler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws")
                .setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toArray(String[]::new));
    }
}
