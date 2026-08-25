package com.stuttgarttaxi.taxihub.config;

import com.stuttgarttaxi.taxihub.entity.AccountType;
import com.stuttgarttaxi.taxihub.entity.Booking;
import com.stuttgarttaxi.taxihub.entity.Customer;
import com.stuttgarttaxi.taxihub.repository.BookingRepository;
import com.stuttgarttaxi.taxihub.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Customer Database (/customers) is meant to be a real record of who's
 * actually booked - not a separately-maintained list that can drift out of
 * sync. Every booking going forward auto-registers its customer (see
 * BookingService.createBooking -> CustomerService.ensureCustomerExists);
 * this just does the same thing once for whatever bookings already existed
 * before the Customer table did, so the page isn't empty on first load.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class CustomerBackfillSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            return;
        }

        List<Booking> allBookings = bookingRepository.findAll();
        if (allBookings.isEmpty()) {
            return;
        }

        // One customer per distinct email, using their most recent booking
        // for name/phone (in case a guest typed their name slightly
        // differently across bookings).
        Map<String, Booking> latestBookingByEmail = allBookings.stream()
                .collect(Collectors.toMap(
                        b -> b.getEmail().toLowerCase(),
                        b -> b,
                        (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b
                ));

        List<Customer> customers = latestBookingByEmail.values().stream()
                .sorted(Comparator.comparing(Booking::getCreatedAt))
                .map(b -> Customer.builder()
                        .firstName(b.getFirstName())
                        .lastName(b.getLastName())
                        .email(b.getEmail().toLowerCase())
                        .phoneNumber(b.getPhoneNumber())
                        .accountType(AccountType.PERSONAL)
                        .build())
                .toList();

        customerRepository.saveAll(customers);
        log.info("{} müşteri, mevcut rezervasyon verisinden oluşturuldu.", customers.size());
    }
}
