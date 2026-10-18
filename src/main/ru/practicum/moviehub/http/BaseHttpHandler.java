package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;

public abstract class BaseHttpHandler implements HttpHandler {
    //Content-Type для каждого ответа на сервере
    protected static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    protected static final String CT_JSON = "application/json; charset=UTF-8";

    Gson gson = new Gson();

    //функция для отправки ответа с телом в формате JSON, когда есть тело
    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        // преобразуем строку в массив байтов
        byte[] bytes = json.getBytes(DEFAULT_CHARSET);

        //добавляем заголовки
        ex.getResponseHeaders().set("Content-Type",CT_JSON);
        //отправляем ответ с кодом 200
        ex.sendResponseHeaders(status,bytes.length);

        //записываем строку-ответ в тело ответа
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    //функция, которая выводит ошибки
    protected void sendError(HttpExchange ex, int status, String message, String details) throws IOException {
        ErrorResponse errorResponse;

        //елси детали есть, добавляем их в ошибки, если нет, то просто сообщение
        if (!details.isBlank()) errorResponse = new ErrorResponse(message,gson.fromJson(details,String[].class));
        else errorResponse = new ErrorResponse(message);

        sendJson(ex, status, gson.toJson(errorResponse));
    }

    //функция для отправки ответа, когда нет тела
    protected void sendNoContent(HttpExchange ex) throws java.io.IOException {
        //добавляем заголовки
        ex.getResponseHeaders().set("Content-Type",CT_JSON);
        //отпраляем ответ с кодом 204
        ex.sendResponseHeaders(204,-1);
    }
}