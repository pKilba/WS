package com.fin;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WebSocketServerEndpoint {

    // Хранение активных сессий
    private static final Set<Session> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @OnWebSocketConnect
    public void onConnect(Session session) throws IOException {
        System.out.println("Server: Connected to client");
        sessions.add(session);
        session.getRemote().sendString("Welcome to the server!");
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws IOException {
        System.out.println("Server: Received message - " + message);
        session.getRemote().sendString("Server received your message: " + message);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("Server: Connection closed - " + reason);
        sessions.remove(session);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
        sessions.remove(session);
    }

    public static void main(String[] args) {
        // Создаем сервер
        Server server = new Server();

        // Настройка соединения
        ServerConnector connector = new ServerConnector(server);
        connector.setHost("0.0.0.0");

        connector.setPort(1357);
        connector.setIdleTimeout(1300000); // Устанавливаем тайм-аут в 5 минут (300 000 миллисекунд)
        server.addConnector(connector);

        // Настройка контекста для сервлетов
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Настройка Jetty WebSocket контейнера с увеличенным тайм-аутом
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.setIdleTimeout(Duration.ofDays(1300000)); // Устанавливаем тайм-аут на уровне WebSocket контейнера
            wsContainer.addMapping("/ws", WebSocketServerEndpoint.class);
        });

        // Создаем задачу для периодической отправки сообщений
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            for (Session session : sessions) {
                try {
                    session.getRemote().sendString("place_bet");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 6, TimeUnit.MINUTES); // Отправка сообщения каждые 1 минуту

        try {
            server.start();
            System.out.println("WebSocket server started on ws://localhost:1357/ws");
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scheduler.shutdown(); // Останавливаем планировщик при завершении работы сервера
        }
    }
}
