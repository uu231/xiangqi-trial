package com.dyouwang.xiangqi.server;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration // 这是一个 Spring 配置类
@EnableWebSocket // 启用 WebSocket 支持
public class WebSocketConfig implements WebSocketConfigurer {

    // 我们将在这里注入我们的处理器
    private final GameWebSocketHandler gameWebSocketHandler;

    public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler) {
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 1. 注册我们的处理器 gameWebSocketHandler
        // 2. 将它映射到路径 "/game"
        // 3. setAllowedOrigins("*") 允许来自任何域的连接 (用于开发, 生产环境需要更严格)
        registry.addHandler(gameWebSocketHandler, "/game")
                .setAllowedOrigins("*");
    }
}