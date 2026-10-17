package ru.practicum.moviehub.api;

import java.util.Arrays;

public class ErrorResponse {

    private String error;
    private String[] details;

    public ErrorResponse(String error) {
        this.error = error;
        this.details = new String[]{};
    }

    public ErrorResponse(String error, String[] details) {
        this.error = error;
        this.details = details;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "error='" + error + '\'' +
                ", details=" + Arrays.toString(details) +
                '}';
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String[] getDetails() {
        return details;
    }

    public void setDetails(String[] details) {
        this.details = details;
    }
}
