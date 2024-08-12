package com.fin;

import okhttp3.*;
import okio.ByteString;

import java.util.concurrent.TimeUnit;
import java.util.Timer;
import java.util.TimerTask;

public class WebSocketClient {

    private final OkHttpClient client;
    private WebSocket webSocket;
    private Timer timer;

    public WebSocketClient(String url) {
        client = new OkHttpClient.Builder()
                .readTimeout(3, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder().url(url).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("Client: Connected to server");
                webSocket.send("Hello, Server!");

                // Начинаем отправку пинг-сообщений каждые 2 минуты
                startPingMessages();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                System.out.println("Client: Received message - " + text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                System.out.println("Client: Received ByteString message");
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
                System.out.println("Client: Closing connection");
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                t.printStackTrace();
            }
        });

        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
        client.dispatcher().executorService().shutdown();
    }

    private void startPingMessages() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (webSocket != null) {
                    webSocket.send("ping"); // Отправляем пинг-сообщение
                    System.out.println("Client: Sent ping to server");
                }
            }
        }, 0, 120000); // Каждые 2 минуты (120 000 миллисекунд)
    }

    public void sendMessage(String message) {
        webSocket.send(message);
    }

    public void closeConnection() {
        if (timer != null) {
            timer.cancel();
        }
        if (webSocket != null) {
            webSocket.close(1000, "Client closing connection");
        }
    }

    public static void main(String[] args) {
        WebSocketClient client = new WebSocketClient("ws://localhost:1357/ws");

        // Отправка тестового сообщения серверу
        client.sendMessage("Test message from client!");

        // Ожидание ввода пользователя для завершения
        System.out.println("Press Enter to close the connection...");
        try {
            System.in.read(); // Ожидание нажатия Enter
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Закрытие соединения после нажатия Enter
        client.closeConnection();
    }
}
