package com.stuttgarttaxi.taxihub.service;

import com.stuttgarttaxi.taxihub.dto.CustomerForm;
import com.stuttgarttaxi.taxihub.dto.CustomerRow;
import com.stuttgarttaxi.taxihub.entity.AccountType;
import com.stuttgarttaxi.taxihub.entity.Booking;
import com.stuttgarttaxi.taxihub.entity.Customer;
import com.stuttgarttaxi.taxihub.exception.CustomerEmailAlreadyExistsException;
import com.stuttgarttaxi.taxihub.exception.CustomerNotFoundException;
import com.stuttgarttaxi.taxihub.repository.BookingRepository;
import com.stuttgarttaxi.taxihub.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;

    /**
     * The full Customer Database table: every stored Customer, each paired
     * with ride stats computed live from the bookings table (never stored -
     * a customer's true ride count is whatever's actually in Booking, not a
     * counter that could drift out of sync).
     */
    @Transactional(readOnly = true)
    public List<CustomerRow> getAllCustomersWithStats() {
        List<Booking> allBookings = bookingRepository.findAll();
        Map<String, List<Booking>> bookingsByEmail = allBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getEmail().toLowerCase()));

        return customerRepository.findAll().stream()
                .map(customer -> toRow(customer, bookingsByEmail.getOrDefault(
                        customer.getEmail().toLowerCase(), List.of())))
                .sorted(Comparator.comparing(CustomerRow::lastRideAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private CustomerRow toRow(Customer customer, List<Booking> bookings) {
        LocalDateTime lastRideAt = bookings.stream()
                .map(Booking::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new CustomerRow(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getAccountType(),
                bookings.size(),
                lastRideAt
        );
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Müşteri bulunamadı"));
    }

    @Transactional(readOnly = true)
    public CustomerRow getCustomerRowById(Long id) {
        Customer customer = getCustomerById(id);
        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getEmail().equalsIgnoreCase(customer.getEmail()))
                .toList();
        return toRow(customer, bookings);
    }

    /**
     * "Add New Customer": lets a dispatcher pre-register someone (typically
     * a corporate account) before their first ride.
     */
    @Transactional
    public Customer createCustomer(CustomerForm form) {
        String email = form.getEmail().trim().toLowerCase();

        if (customerRepository.existsByEmailIgnoreCase(email)) {
            throw new CustomerEmailAlreadyExistsException("Bu e-posta adresi zaten kayıtlı");
        }

        Customer customer = Customer.builder()
                .firstName(form.getFirstName())
                .lastName(form.getLastName())
                .email(email)
                .phoneNumber(form.getPhoneNumber())
                .accountType(form.getAccountType())
                .build();

        return customerRepository.save(customer);
    }

    /**
     * Name/phone/account type can be corrected after the fact - email can't,
     * since it's what ties this row back to the customer's ride history.
     */
    @Transactional
    public Customer updateCustomer(Long id, CustomerForm form) {
        Customer customer = getCustomerById(id);
        customer.setFirstName(form.getFirstName());
        customer.setLastName(form.getLastName());
        customer.setPhoneNumber(form.getPhoneNumber());
        customer.setAccountType(form.getAccountType());
        return customerRepository.save(customer);
    }

    /**
     * Called from BookingService.createBooking(): the first time a booking
     * comes in under an email with no matching Customer, silently create
     * one (PERSONAL by default) so the Customer Database stays a complete,
     * accurate record of everyone who's actually booked - not something a
     * dispatcher has to remember to do by hand.
     */
    @Transactional
    public void ensureCustomerExists(String email, String firstName, String lastName, String phoneNumber) {
        String normalizedEmail = email.trim().toLowerCase();

        if (customerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }

        Customer customer = Customer.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(normalizedEmail)
                .phoneNumber(phoneNumber)
                .accountType(AccountType.PERSONAL)
                .build();

        customerRepository.save(customer);
    }
}
