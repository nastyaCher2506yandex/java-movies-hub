package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    //создаем хранилище фильмов, которые есть на сервере
    private MoviesStore moviesStore;

    MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        //выводим метод и путь запроса
        String requestMethod = ex.getRequestMethod();
        String requestPath = ex.getRequestURI().getPath();

        //находим подходящий эндпоинт
        Endpoint endpoint = getEndpoint(requestPath,requestMethod,ex.getRequestURI().getRawQuery());
        switch (endpoint) {
            case GET_MOVIES :
                handleGetMovies(ex);
                break;
            case GET_MOVIE_ID :
                handleGetMovieId(ex);
                break;
            case GET_MOVIES_WITH_YEARS:
                handleGetMovieWithYear(ex);
                break;
            case DELETE_MOVIE_ID:
                handleDeleteMovieId(ex);
                break;
            case POST_MOVIE :
                handlePostMovies(ex);
                break;
            default:
                sendError(ex,405,"Такого метода нет","");
        }

    }

    private void handleGetMovies(HttpExchange ex) throws IOException {
        //преобразуем список в json-строку и передаем его в ответ сервера
        //т.к. согласно условию должен возвращаться массив фильмов, нам нужно получить сам список из хранилища
        String json = gson.toJson(moviesStore.getMovies());

        sendJson(ex,200,json);
    }

    private void handleGetMovieId(HttpExchange ex) throws IOException {
        //преобразуем список в json-строку и передаем его в ответ сервера
        //т.к. согласно условию должен возвращаться массив фильмов, нам нужно получить сам список из хранилища

        Optional<Integer> movieIdOpt = getMovieId(ex);
        if (movieIdOpt.isEmpty()) {
            sendError(ex,400, "Некорректный ID","");
            return;
        }

        int idMovie = movieIdOpt.get();
        for (Movie movie : moviesStore.getMovies()) {
            if (idMovie == movie.getId()) {
                sendJson(ex,200, gson.toJson(movie));
                return;
            }
        }

        sendError(ex,404, "Фильм не найден","");
    }

    private void handleGetMovieWithYear(HttpExchange ex) throws IOException {
        Optional<Integer> movieYearOpt = getMovieYear(ex);

        if (movieYearOpt.isEmpty()) {
            sendError(ex,400, "Некорректный параметр запроса — 'year'","");
            return;
        }

        sendJson(ex,200,gson.toJson(moviesStore.getAllMovieYear(movieYearOpt.get())));
    }

    private void handleDeleteMovieId(HttpExchange ex) throws IOException {
        //преобразуем список в json-строку и передаем его в ответ сервера
        //т.к. согласно условию должен возвращаться массив фильмов, нам нужно получить сам список из хранилища

        Optional<Integer> movieIdOpt = getMovieId(ex);
        if (movieIdOpt.isEmpty()) {

            sendError(ex,400, "Некорректный ID", "");
            return;
        }

        int idMovie = movieIdOpt.get();
        for (Movie movie : moviesStore.getMovies()) {
            if (idMovie == movie.getId()) {
                moviesStore.deleteMovieId(movie);
                sendNoContent(ex);
                return;
            }
        }

        sendError(ex,404, "Фильм не найден","");
    }

    private void handlePostMovies(HttpExchange ex) throws IOException {
        String contentTypeHeaderValue = ex.getRequestHeaders().getFirst("Content-Type");

        if (contentTypeHeaderValue == null || !contentTypeHeaderValue.equals(CT_JSON)) {
            sendError(ex,415, "Не был указан формат данных и кодировка","");
            return;
        }

        InputStream inputStream = ex.getRequestBody();
        Movie body;

        //если удалось создать, то искать какая была ошибка не нужно
        try {
            body = gson.fromJson(new String(inputStream.readAllBytes(), DEFAULT_CHARSET), Movie.class);
            moviesStore.addMovies(body.getTitle().trim(), body.getYear());
            sendJson(ex,201,gson.toJson(moviesStore.getMovie(moviesStore.sizeMovies() - 1)));
        } catch (IllegalArgumentException error) {
            sendError(ex,422,"Ошибка валидации",error.getMessage());
        } catch (NullPointerException error) {
            sendError(ex,422,"Ошибка валидации", gson.toJson(new String[]{"отсутствует обязательное поле"}));
        } catch (JsonSyntaxException e) {
            sendError(ex,400, "Некорректный json","");
        }
    }

    private Endpoint getEndpoint(String requestPath, String requestMethod, String rawQuery) {
        String[] parts = requestPath.split("/");

        //эл. 0 - это пустая строка т.к. /movies, поэтому счет начинается с 1
        if (requestMethod.equalsIgnoreCase("GET") && parts[1].equals("movies")) {
            if ((parts.length == 2)) {
                if (rawQuery == null) {
                    return Endpoint.GET_MOVIES;
                } else {
                    return Endpoint.GET_MOVIES_WITH_YEARS;
                }
            } else if (parts.length == 3) {
                return Endpoint.GET_MOVIE_ID;
            }
        } else if (requestMethod.equalsIgnoreCase("POST") && parts[1].equals("movies") && (parts.length == 2)) {
                return Endpoint.POST_MOVIE;
        } else if (requestMethod.equalsIgnoreCase("DELETE") && parts[1].equals("movies") && (parts.length == 3)) {
            return Endpoint.DELETE_MOVIE_ID;
        }

        return Endpoint.UNKNOWN;
    }

    private Optional<Integer> getMovieId(HttpExchange ex) {
        String[] pathParts = ex.getRequestURI().getPath().split("/");
        try {
            return Optional.of(Integer.parseInt(pathParts[2]));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private Optional<Integer> getMovieYear(HttpExchange ex) {
        String[] parts = ex.getRequestURI().getRawQuery().split("&");

        if (parts.length !=  1) {
            return Optional.empty();
        }
        try {
            String[] year = parts[0].split("=");
            if (!year[0].equals("year")) {
                return Optional.empty();
            }
            return Optional.of(Integer.parseInt(year[1]));
        //если значение параметра указано числом или не указано вообще, то отдаем пустой Oprinal
            //это приведет к ошибке, что некорректный year
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException exception) {
            return Optional.empty();
        }
    }
}
