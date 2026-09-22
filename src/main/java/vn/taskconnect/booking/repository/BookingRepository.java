package vn.taskconnect.booking.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.booking.entity.Booking;

/**
 * Truy xuat du lieu bang booking_bookings. Chi module Booking duoc inject truc tiep repository
 * nay - module khac phai goi qua BookingFacade.
 */
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /** Booking gan voi 1 application - UNIQUE trong V31, toi da 1 ket qua. */
    Optional<Booking> findByApplicationId(UUID applicationId);
}
