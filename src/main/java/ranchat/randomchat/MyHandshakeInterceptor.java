package ranchat.randomchat;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class MyHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        HttpSession httpSession = ((ServletServerHttpRequest) request).getServletRequest().getSession();

        if (httpSession != null) {
            // HttpSession에서 sessionId를 가져와서 WebSocket 세션의 attribute에 저장
            String sessionId = httpSession.getId();
            attributes.put("sessionId", sessionId);

            // 다른 필요한 정보도 필요하다면 attribute에 저장할 수 있음
            // attributes.put("customKey", "customValue");
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Handshake 이후에 추가 작업이 필요하다면 여기에 구현
    }
}
