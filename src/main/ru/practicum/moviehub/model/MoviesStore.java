package ru.practicum.moviehub.model;

import com.google.gson.Gson;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


//класс отвечает за хранение, добавление, поиск и удаление фильмов
public class MoviesStore {

    private static final int MAX_CHAR_TITLE = 100;                      //название не может содержать больше 100 символов
    private static final int MIN_YEAR = 1888;                           //год не может быть меньше 1888
    private static final int MAX_YEAR = LocalDate.now().getYear() + 1;  //год не может быть больше, чем текущего года + 1

    //хранилище фильмов
    private List<Movie> movies;
    private Gson gson;

    public MoviesStore() {
        movies = new ArrayList<>();
        gson = new Gson();
    }

    //добавляем фильм
    public boolean addMovies(String title, int year) {
        //получаем id последнего элемента и добавляем +1
        int id = 0;
        if (!movies.isEmpty()) id = getMovie(sizeMovies() - 1).getId() + 1;

        List<String> error = new ArrayList<>();

        if (title.length() > MAX_CHAR_TITLE) {
            error.add("размер не может быть больше " + MAX_CHAR_TITLE + " символов");
        }
        if (title.isBlank()) {
            error.add("название не может быть пустым");
        }
        if (year < MIN_YEAR || year > MAX_YEAR) {
            error.add("год должен быть между " + MIN_YEAR + " и " + MAX_YEAR);
        }

        if (!error.isEmpty()) throw new IllegalArgumentException(gson.toJson(error));

        return movies.add(new Movie(id,title,year));
    }

    //получание элемента по индексу
    public Movie getMovie(int index) {
        return movies.get(index);
    }

    //выводим количество фильмов в хранилище
    public int sizeMovies() {
        return movies.size();
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public void clearAllMovies() {
        movies.clear();
    }

    public void deleteMovieId(Movie movie) {
        movies.remove(movie);
    }

    public List<Movie> getAllMovieYear(int year) {
        return movies.stream()
                .filter(movie -> movie.getYear() == year)
                .map(movie -> movie)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "MoviesStore{" +
                "movies=" + movies +
                '}';
    }
}
