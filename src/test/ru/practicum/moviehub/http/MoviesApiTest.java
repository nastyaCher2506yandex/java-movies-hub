package ru.practicum.moviehub.http;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MoviesStore;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final String CT_JSON = "application/json; charset=UTF-8";

    private static final int MAX_CHAR_TITLE = 100;                      //название не может содержать больше 100 символов
    private static final int MIN_YEAR = 1888;                           //год не может быть меньше 1888
    private static final int MAX_YEAR = LocalDate.now().getYear() + 1;  //год не может быть больше, чем текущего года + 1

    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore moviesStore;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        moviesStore = new MoviesStore();
        server = new MoviesServer(moviesStore);
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        gson = new Gson();

        server.start();
    }

    @BeforeEach
    void beforeEach() {
        //очищаем весь список для следующих тестов
        moviesStore.clearAllMovies();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    //если список фильмов пустой
    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        //строим запрос GET
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        //получаем ответ от сервера
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        //проверяем, чтобы был возвращен массив
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    //если список фильмов не пустой
    @Test
    void getMovies_whenNonEmpty_returnArray() throws Exception {
        //добавим несколько фильмов
        moviesStore.addMovies("Movie1",1980);
        moviesStore.addMovies("Movie2",1980);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        //проверяем, что не получили пустой массив
        assertNotEquals("[]", body, "Ожидается JSON-массив");

        //проверяем, чтобы был возвращен массив (согласно условию, возвращаем массив фильмов из хранилища
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    //POST /movies добавляет фильм при корректных данных
    @Test
    void postMovies_whenCorrectData_returnArray() throws Exception {
        String jsonBody = "{\"title\":\"Movie\",\"year\":1890}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, DEFAULT_CHARSET))
                .headers("Content-Type",CT_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        //проверяем, что это объект
        assertTrue(body.startsWith("{") && body.endsWith("}"),
                "Ожидается JSON-объект");
    }

    //POST /movies возвращает ошибку при пустом title
    @Test
    void postMovies_whenWrongData_returnErrorEmptyTitle() throws Exception {
        String jsonBody = "{\"title\":\"\",\"year\":1890}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, DEFAULT_CHARSET))
                .headers("Content-Type",CT_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse body = gson.fromJson(resp.body().trim(),ErrorResponse.class);

        String[] details = body.getDetails();

        //проверяем, что это объект
        assertEquals(1,details.length,"Должен быть пустым только название (1 ошибка)");
        assertEquals("название не может быть пустым",details[0],"Название фильма должно быть пустым");
    }

    //POST /movies возвращает ошибку при слишком длинном title (> 100 символов)
    @Test
    void postMovies_whenWrongData_returnErrorTooLongTitle() throws Exception {
        String jsonBody = "{\"title\":\"Когда город засыпает: тени прошлого выходят на пустые улицы" +
                ", чтобы напомнить о старых долгах, которые никто не хотел вспоминать\"" +
                ",\"year\":1890}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, DEFAULT_CHARSET))
                .headers("Content-Type",CT_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse body = gson.fromJson(resp.body().trim(),ErrorResponse.class);

        String[] details = body.getDetails();

        //проверяем, что это объект
        assertEquals(1,details.length,"Должно быть только длинное название (1 ошибка)");
        assertEquals("размер не может быть больше " + MAX_CHAR_TITLE + " символов",details[0],
                "Название фильма должно больше 100 символов");
    }

    //POST /movies возвращает ошибку при неверном year (меньше 1888 или больше текущего года + 1);
    //когда дата больше
    @Test
    void postMovies_whenWrongData_returnErrorMoreYear() throws Exception {
        String jsonBody = "{\"title\":\"Когда город засыпает\"" +
                ",\"year\":2700}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, DEFAULT_CHARSET))
                .headers("Content-Type",CT_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse body = gson.fromJson(resp.body().trim(),ErrorResponse.class);

        String[] details = body.getDetails();

        //проверяем, что это объект
        assertEquals(1,details.length,"Должно быть только время вне диапозова(1 ошибка)");
        assertEquals("год должен быть между " + MIN_YEAR + " и " + MAX_YEAR, details[0],
                "Название фильма должно больше 100 символов");
    }

    //когда дата меньше
    @Test
    void postMovies_whenWrongData_returnErrorLessYear() throws Exception {
        String jsonBody = "{\"title\":\"Когда город засыпает\"" +
                ",\"year\":1800}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, DEFAULT_CHARSET))
                .headers("Content-Type",CT_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse body = gson.fromJson(resp.body().trim(),ErrorResponse.class);

        String[] details = body.getDetails();

        //проверяем, что это объект
        assertEquals(1,details.length,"Должно быть только время вне диапозова(1 ошибка)");
        assertEquals("год должен быть между " + MIN_YEAR + " и " + MAX_YEAR, details[0],
                "Название фильма должно больше 100 символов");
    }

    //когда ошибок больше одной
    @Test
    void postMovies_whenWrongData_returnErrorMoreError() throws Exception {
        String jsonBody = "{\"title\":\"Когда город засыпает: тени прошлого выходят на пустые улицы" +
                ", чтобы напомнить о старых долгах, которые никто не хотел вспоминать\"" +
                ",\"year\":2700}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, DEFAULT_CHARSET))
                .headers("Content-Type",CT_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse body = gson.fromJson(resp.body().trim(),ErrorResponse.class);

        String[] details = body.getDetails();

        //проверяем, что это объект
        assertEquals(2,details.length,"Должен быть только длинное название и время за пределами диапозона(2 ошибки)");
        assertEquals("размер не может быть больше " + MAX_CHAR_TITLE + " символов",details[0],
                "Название фильма должно больше 100 символов");
        assertEquals("год должен быть между " + MIN_YEAR + " и " + MAX_YEAR, details[1],
                "Название фильма должно больше 100 символов");
    }

    //POST /movies возвращает ошибку при неверном year (меньше 1888 или больше текущего года + 1);
    @Test
    void postMovies_whenWrongData_returnErrorNotContentType() throws Exception {
        String jsonBody = "{\"title\":\"Movie\",\"year\":1890}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, DEFAULT_CHARSET))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse body = gson.fromJson(resp.body().trim(),ErrorResponse.class);

        String[] details = body.getDetails();

        //проверяем, что это объект
        assertEquals(0,details.length,"деталей быть не должно");
        assertEquals("Не был указан формат данных и кодировка", body.getError());
    }

    //POST /movies возвращает ошибку при некорректном JSON;
    //когда неправильные переменные
    @Test
    void postMovies_whenWrongData_returnErrorNotJSONWrong() throws Exception {
        String jsonBody = "{\"titlefdfs\":\"Movie\",\"yeadfdfr\":1890}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, DEFAULT_CHARSET))
                .headers("Content-Type",CT_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse body = gson.fromJson(resp.body().trim(),ErrorResponse.class);

        String[] details = body.getDetails();

        //проверяем, что это объект
        assertEquals(1,details.length,"Деталь одна, что неправильный json");
        assertEquals("некорректный json", details[0]);
    }

    //когда одна переменная не указана
    @Test
    void postMovies_whenWrongData_returnErrorNotJSONWrongWithoutVar() throws Exception {
        String jsonBody = "{\"year\":1890}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, DEFAULT_CHARSET))
                .headers("Content-Type",CT_JSON)
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse body = gson.fromJson(resp.body().trim(),ErrorResponse.class);

        String[] details = body.getDetails();

        //проверяем, что это объект
        assertEquals(1,details.length,"Деталь одна, что неправильный json");
        assertEquals("некорректный json", details[0]);
    }

    //GET /movies/{id} возвращает фильм по существующему id
    @Test
    void getMovies_whenIdCorrect_returnObject() throws Exception {
        //добавим несколько фильмов
        moviesStore.addMovies("Movie1",1980);
        moviesStore.addMovies("Movie2",1980);
        moviesStore.addMovies("Movie3",1984);
        moviesStore.addMovies("Movie4",2000);

        //строим запрос GET
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        //получаем ответ от сервера
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        //проверяем, чтобы был возвращен объект
        assertTrue(body.startsWith("{") && body.endsWith("}"),
                "Ожидается JSON-объект");

        Movie movie = gson.fromJson(body,Movie.class);
        assertEquals("Movie2",movie.getTitle(),"Неправильное название");
        assertEquals(1,movie.getId(),"Неправильный ID");
    }

    //GET /movies/{id} возвращает ошибку, если фильм не найден
    @Test
    void getMovies_whenIdWrong_returnErrorFilmNotFind() throws Exception {
        //добавим несколько фильмов
        moviesStore.addMovies("Movie1",1980);
        moviesStore.addMovies("Movie2",1980);
        moviesStore.addMovies("Movie3",1984);
        moviesStore.addMovies("Movie4",2000);

        //строим запрос GET
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/10"))
                .GET()
                .build();

        //получаем ответ от сервера
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(404, resp.statusCode(), "GET /movies должен вернуть 404");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse body = gson.fromJson(resp.body().trim(),ErrorResponse.class);

        assertEquals("Фильм не найден",body.getError(),"Должна быть ошибка фильм не найдет");
    }

    //GET /movies/{id} возвращает ошибку, если id не число
    @Test
    void getMovies_whenIdWrong_returnErrorIdNotNum() throws Exception {
        //добавим несколько фильмов
        moviesStore.addMovies("Movie1",1980);
        moviesStore.addMovies("Movie2",1980);
        moviesStore.addMovies("Movie3",1984);
        moviesStore.addMovies("Movie4",2000);

        //строим запрос GET
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/десять"))
                .GET()
                .build();

        //получаем ответ от сервера
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(400, resp.statusCode(), "GET /movies должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse body = gson.fromJson(resp.body().trim(),ErrorResponse.class);

        assertEquals("Некорректный ID",body.getError(),"Должна быть ошибка некорректный ID");
    }

    //DELETE /movies/{id} удаляет фильм по существующему id
    @Test
    void deleteMovies_whenIdCorrect_returnObject() throws Exception {
        //добавим несколько фильмов
        moviesStore.addMovies("Movie1",1980);
        moviesStore.addMovies("Movie2",1980);
        moviesStore.addMovies("Movie3",1984);
        moviesStore.addMovies("Movie4",2000);

        //строим запрос GET
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        //получаем ответ от сервера
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(204, resp.statusCode(), "DELETE /movies должен вернуть 204");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        //проверяем, что ничего не вернулось
        assertTrue(body.isBlank(),
                "Ожидается, что тела ответа не будет");

        assertEquals(3,moviesStore.sizeMovies(),"Элемент должен был удалиться из списка фильмов");
    }

    //DELETE /movies/{id}  возвращает ошибку, если фильм не найден
    @Test
    void deleteMovies_whenIdWrong_returnErrorFilmNotFind() throws Exception {
        //добавим несколько фильмов
        moviesStore.addMovies("Movie1",1980);
        moviesStore.addMovies("Movie2",1980);
        moviesStore.addMovies("Movie3",1984);
        moviesStore.addMovies("Movie4",2000);

        //строим запрос GET
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/10"))
                .DELETE()
                .build();

        //получаем ответ от сервера
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(404, resp.statusCode(), "DELETE /movies должен вернуть 404");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse body = gson.fromJson(resp.body().trim(),ErrorResponse.class);

        assertEquals(4,moviesStore.sizeMovies(),"Элемент должен был удалиться из списка фильмов");
        assertEquals("Фильм не найден",body.getError(),"Должна быть ошибка фильм не найдет");
    }

    //DELETE /movies/{id}  возвращает ошибку, если id не число
    @Test
    void deleteMovies_whenIdWrong_returnErrorIdNotNum() throws Exception {
        //добавим несколько фильмов
        moviesStore.addMovies("Movie1",1980);
        moviesStore.addMovies("Movie2",1980);
        moviesStore.addMovies("Movie3",1984);
        moviesStore.addMovies("Movie4",2000);

        //строим запрос GET
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/десять"))
                .DELETE()
                .build();

        //получаем ответ от сервера
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(400, resp.statusCode(), "DELETE /movies должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        ErrorResponse body = gson.fromJson(resp.body().trim(),ErrorResponse.class);

        assertEquals(4,moviesStore.sizeMovies(),"Элемент должен был удалиться из списка фильмов");
        assertEquals("Некорректный ID",body.getError(),"Должна быть ошибка некорректный ID");
    }

    //GET /movies?year=YYYY возвращает фильмы указанного года;
    @Test
    void getMovies_ForYear_ReturnArray() throws IOException, InterruptedException {
        moviesStore.addMovies("Movie1",1980);
        moviesStore.addMovies("Movie2",1980);
        moviesStore.addMovies("Movie3",1984);
        moviesStore.addMovies("Movie4",2000);

        //строим запрос GET
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1980"))
                .GET()
                .build();

        //получаем ответ от сервера
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        List<Movie> movieList = gson.fromJson(body,new ListOfMoviesTypeToken().getType());

        for (Movie movie : movieList) {
            assertTrue(movie.getYear() == 1980,"Год должен ровняться году в параметрах запроса");
        }
    }

    //GET /movies?year=YYYY возвращает фильмы указанного года;
    @Test
    void getMovies_ForYear_ReturnEmptyArray() throws IOException, InterruptedException {
        moviesStore.addMovies("Movie1",1980);
        moviesStore.addMovies("Movie2",1980);
        moviesStore.addMovies("Movie3",1984);
        moviesStore.addMovies("Movie4",2000);

        //строим запрос GET
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2001"))
                .GET()
                .build();

        //получаем ответ от сервера
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        List<Movie> movieList = gson.fromJson(body,new ListOfMoviesTypeToken().getType());

        assertTrue(movieList.isEmpty(),"Должен вернуть пустой список");
    }

    //GET /movies?year=YYYY возвращает ошибку, если параметр year не число.
    @Test
    void getMovies_ForYearNonNum_ReturnEmptyArray() throws IOException, InterruptedException {
        moviesStore.addMovies("Movie1",1980);
        moviesStore.addMovies("Movie2",1980);
        moviesStore.addMovies("Movie3",1984);
        moviesStore.addMovies("Movie4",2000);

        //строим запрос GET
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=jbbjj"))
                .GET()
                .build();

        //получаем ответ от сервера
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(400, resp.statusCode(), "GET /movies должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        ErrorResponse error= gson.fromJson(body,ErrorResponse.class);

        assertEquals("Некорректный параметр запроса — 'year'", error.getError(),"Должна вылезти ошибка некореектный year");
    }
}