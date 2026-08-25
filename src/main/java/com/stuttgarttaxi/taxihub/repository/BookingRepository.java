package com.stuttgarttaxi.taxihub.repository;

import com.stuttgarttaxi.taxihub.entity.Booking;
import com.stuttgarttaxi.taxihub.entity.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByBookingCode(String bookingCode);

    @EntityGraph(attributePaths = "assignedEmployee")
    List<Booking> findAllByOrderByBookingDateDescBookingTimeDesc();

    @EntityGraph(attributePaths = "assignedEmployee")
    Optional<Booking> findWithAssignedEmployeeById(Long id);

    List<Booking> findByStatusOrderByBookingDateAscBookingTimeAsc(BookingStatus status);

    Optional<Booking> findFirstByAssignedEmployeeIdAndStatusIn(Long employeeId, Collection<BookingStatus> statuses);

    long countByStatus(BookingStatus status);

    long countByStatusIn(Collection<BookingStatus> statuses);

    long countByBookingDate(LocalDate date);

    long countByAssignedEmployeeIdAndStatusAndCompletedAtBetween(
            Long employeeId, BookingStatus status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT b.assignedEmployee.id FROM Booking b WHERE b.status IN :statuses AND b.assignedEmployee IS NOT NULL")
    List<Long> findAssignedEmployeeIdsByStatusIn(Collection<BookingStatus> statuses);
}
