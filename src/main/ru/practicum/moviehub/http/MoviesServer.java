package ru.practicum.moviehub.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.model.MoviesStore;

public class MoviesServer {
    private final HttpServer server;

    public MoviesServer(MoviesStore moviesStore) {
        try {
            // создание сервера
            server = HttpServer.create(new InetSocketAddress(8080),0);

            // связывание путу и обработчика запросов
            server.createContext("/movies", new MoviesHandler(moviesStore));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        // запустите сервер
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        // остановите сервер
        server.stop(0);
        System.out.println("Сервер остановлен");
    }
}