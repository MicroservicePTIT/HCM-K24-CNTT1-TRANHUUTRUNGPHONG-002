package org.example.bookingservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.bookingservice.models.constants.BookingStatus;
import org.example.bookingservice.models.dto.requests.CreateBookingDetailRequest;
import org.example.bookingservice.models.dto.requests.CreateBookingRequest;
import org.example.bookingservice.models.dto.responses.BookingDetailResponse;
import org.example.bookingservice.models.dto.responses.BookingResponse;
import org.example.bookingservice.models.dto.responses.MovieResponse;
import org.example.bookingservice.models.entities.Booking;
import org.example.bookingservice.models.entities.BookingDetail;
import org.example.bookingservice.models.repositories.BookingDetailRepository;
import org.example.bookingservice.models.repositories.BookingRepository;
import org.example.bookingservice.models.services.BookingService;
import org.example.bookingservice.services.BookingEventProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

        private final BookingRepository bookingRepository;
        private final BookingDetailRepository bookingDetailRepository;
        private final MovieGatewayService movieGatewayService;
        private final BookingEventProducer bookingEventProducer;

        @Override
        @Transactional
        public BookingResponse createBooking(CreateBookingRequest request) {
                List<BookingDetail> bookingDetails = new ArrayList<>();
                List<BookingDetailResponse> bookingDetailResponses = new ArrayList<>();
                double totalAmount = 0.0;

                for (CreateBookingDetailRequest item : request.items()) {
                        MovieResponse movie = movieGatewayService.getMovieById(item.movieId());
                        double unitPrice = movie.ticketPrice();
                        double subtotal = unitPrice * item.quantity();
                        totalAmount += subtotal;

                        BookingDetail bookingDetail = BookingDetail.builder()
                                .movieId(item.movieId())
                                .quantity(item.quantity())
                                .unitPrice(unitPrice)
                                .build();

                        bookingDetails.add(bookingDetail);

                        BookingDetailResponse detailResponse = new BookingDetailResponse(
                                null,
                                item.movieId(),
                                movie.title(),
                                item.quantity(),
                                unitPrice,
                                subtotal
                        );
                        bookingDetailResponses.add(detailResponse);
                }

                Booking booking = Booking.builder()
                        .customerName(request.customerName())
                        .customerEmail(request.customerEmail())
                        .total(totalAmount)
                        .status(BookingStatus.PENDING)
                        .build();

                Booking savedBooking = bookingRepository.save(booking);

                for (BookingDetail detail : bookingDetails) {
                        detail.setBooking(savedBooking);
                        bookingDetailRepository.save(detail);
                }

                bookingEventProducer.sendBookingCreatedEvent(request.customerEmail());

                List<BookingDetailResponse> finalResponses = new ArrayList<>();
                for (int i = 0; i < bookingDetailResponses.size(); i++) {
                        BookingDetailResponse resp = bookingDetailResponses.get(i);
                        finalResponses.add(new BookingDetailResponse(
                                bookingDetails.get(i).getId(),
                                resp.movieId(),
                                resp.movieTitle(),
                                resp.quantity(),
                                resp.unitPrice(),
                                resp.subtotal()
                        ));
                }

                return new BookingResponse(
                        savedBooking.getId(),
                        savedBooking.getCustomerName(),
                        savedBooking.getCustomerEmail(),
                        savedBooking.getTotal(),
                        savedBooking.getStatus(),
                        finalResponses
                );
        }
}
