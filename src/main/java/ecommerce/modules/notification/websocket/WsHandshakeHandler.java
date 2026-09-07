package ecommerce.modules.notification.websocket;

import ecommerce.common.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket handshake handler that maps the JWT-authenticated user to a WebSocket Principal.
 * The resolved principal name is the user's UUID string, used by SimpMessagingTemplate to
 * route messages to the correct user queue.
 */
@Slf4j
public class WsHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof UserPrincipal principal) {
                String name = principal.getId().toString();
                log.debug("[WS] Handshake accepted userId={}", name);
                return () -> name;
            }
        }
        log.warn("[WS] Handshake rejected — no valid JWT authentication");
        return null;
    }
}
