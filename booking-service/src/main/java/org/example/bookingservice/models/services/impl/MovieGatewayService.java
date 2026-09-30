package org.example.bookingservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.example.bookingservice.clients.MovieClient;
import org.example.bookingservice.exceptions.MovieNotFoundException;
import org.example.bookingservice.exceptions.MovieServiceException;
import org.example.bookingservice.models.dto.responses.MovieResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieGatewayService {

    private final MovieClient movieClient;

    @CircuitBreaker(name = "movieService", fallbackMethod = "fallbackGetMovieById")
    public MovieResponse getMovieById(Long movieId) {
        MovieResponse movie = movieClient.getMovieById(movieId);
        if (movie == null) {
            throw new MovieNotFoundException(movieId);
        }
        return movie;
    }

    public MovieResponse fallbackGetMovieById(Long movieId, Exception e) {
        throw new MovieServiceException("Movie service is unavailable");
    }

}
