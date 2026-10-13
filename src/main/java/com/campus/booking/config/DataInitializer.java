package com.campus.booking.config;

import com.campus.booking.entity.User;
import com.campus.booking.enums.Role;
import com.campus.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final com.campus.booking.repository.ResourceRepository resourceRepository;
    private final com.campus.booking.repository.BookingRepository bookingRepository;
    private final com.campus.booking.repository.NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedResources();
        seedBookings();
    }

    private void seedUsers() {
        // Required Seed Admin User
        if (!userRepository.existsByEmail("admin@campus.edu")) {
            User admin = User.builder()
                    .name("Campus Administrator")
                    .email("admin@campus.edu")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(admin);
            log.info("Default ADMIN user seeded: admin@campus.edu / Admin@123");
        }

        // Demo Faculty User
        if (!userRepository.existsByEmail("faculty@campus.edu")) {
            User faculty = User.builder()
                    .name("Dr. Alan Turing")
                    .email("faculty@campus.edu")
                    .password(passwordEncoder.encode("Faculty@123"))
                    .role(Role.FACULTY)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(faculty);
            log.info("Demo FACULTY user seeded: faculty@campus.edu / Faculty@123");
        }

        // Demo Student User
        if (!userRepository.existsByEmail("student@campus.edu")) {
            User student = User.builder()
                    .name("Alex Johnson")
                    .email("student@campus.edu")
                    .password(passwordEncoder.encode("Student@123"))
                    .role(Role.STUDENT)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(student);
            log.info("Demo STUDENT user seeded: student@campus.edu / Student@123");
        }
    }

    private void seedResources() {
        if (resourceRepository.count() == 0) {
            var r1 = com.campus.booking.entity.Resource.builder()
                    .name("Turing Auditorium (Seminar Hall A)")
                    .type(com.campus.booking.enums.ResourceType.SEMINAR_HALL)
                    .location("Building A, Floor 1")
                    .capacity(250)
                    .description("Main campus auditorium with surround sound, stage lighting, and dual 4K laser projectors.")
                    .active(true)
                    .openingTime(java.time.LocalTime.of(8, 0))
                    .closingTime(java.time.LocalTime.of(22, 0))
                    .createdAt(LocalDateTime.now())
                    .build();

            var r2 = com.campus.booking.entity.Resource.builder()
                    .name("Advanced Robotics & AI Lab")
                    .type(com.campus.booking.enums.ResourceType.LAB)
                    .location("Science Complex, Room 302")
                    .capacity(35)
                    .description("High-spec workstations with NVIDIA GPUs, robotics testing arena, and 3D printers.")
                    .active(true)
                    .openingTime(java.time.LocalTime.of(8, 30))
                    .closingTime(java.time.LocalTime.of(20, 0))
                    .createdAt(LocalDateTime.now())
                    .build();

            var r3 = com.campus.booking.entity.Resource.builder()
                    .name("Smart Lecture Hall 101")
                    .type(com.campus.booking.enums.ResourceType.CLASSROOM)
                    .location("Academic Block 2, Floor 1")
                    .capacity(80)
                    .description("Tiered seating lecture theatre equipped with digital whiteboard and lecture capture system.")
                    .active(true)
                    .openingTime(java.time.LocalTime.of(8, 0))
                    .closingTime(java.time.LocalTime.of(18, 0))
                    .createdAt(LocalDateTime.now())
                    .build();

            var r4 = com.campus.booking.entity.Resource.builder()
                    .name("Indoor Badminton Court 1")
                    .type(com.campus.booking.enums.ResourceType.SPORTS_FACILITY)
                    .location("Sports Arena, Court B")
                    .capacity(6)
                    .description("Synthetic wooden flooring court with professional LED lighting and spectator gallery.")
                    .active(true)
                    .openingTime(java.time.LocalTime.of(6, 0))
                    .closingTime(java.time.LocalTime.of(22, 0))
                    .createdAt(LocalDateTime.now())
                    .build();

            var r5 = com.campus.booking.entity.Resource.builder()
                    .name("Portable 4K Laser Projector")
                    .type(com.campus.booking.enums.ResourceType.PROJECTOR)
                    .location("Media Center Equipment Desk")
                    .capacity(1)
                    .description("Epson 5000 Lumens wireless projector with HDMI/AirPlay support and travel carry case.")
                    .active(true)
                    .openingTime(java.time.LocalTime.of(8, 0))
                    .closingTime(java.time.LocalTime.of(18, 0))
                    .createdAt(LocalDateTime.now())
                    .build();

            var r6 = com.campus.booking.entity.Resource.builder()
                    .name("Virtual Reality Research Headset")
                    .type(com.campus.booking.enums.ResourceType.EQUIPMENT)
                    .location("Media Lab, Cabinet 4")
                    .capacity(1)
                    .description("Meta Quest Pro headset with high precision controllers and charging dock.")
                    .active(true)
                    .openingTime(java.time.LocalTime.of(9, 0))
                    .closingTime(java.time.LocalTime.of(17, 0))
                    .createdAt(LocalDateTime.now())
                    .build();

            resourceRepository.saveAll(java.util.List.of(r1, r2, r3, r4, r5, r6));
            log.info("Default campus resources successfully seeded (6 resources).");
        }
    }

    private void seedBookings() {
        if (bookingRepository.count() == 0) {
            var studentOpt = userRepository.findByEmail("student@campus.edu");
            var facultyOpt = userRepository.findByEmail("faculty@campus.edu");
            var resources = resourceRepository.findAll();

            if (studentOpt.isPresent() && facultyOpt.isPresent() && !resources.isEmpty()) {
                var student = studentOpt.get();
                var faculty = facultyOpt.get();
                var hall = resources.get(0);
                var classroom = resources.size() > 2 ? resources.get(2) : resources.get(0);

                var tomorrow = java.time.LocalDate.now().plusDays(1);

                var b1 = com.campus.booking.entity.Booking.builder()
                        .user(student)
                        .resource(classroom)
                        .startTime(tomorrow.atTime(10, 0))
                        .endTime(tomorrow.atTime(12, 0))
                        .purpose("Student AI Research Study Group & Demo")
                        .status(com.campus.booking.enums.BookingStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                var b2 = com.campus.booking.entity.Booking.builder()
                        .user(faculty)
                        .resource(hall)
                        .startTime(tomorrow.atTime(14, 0))
                        .endTime(tomorrow.atTime(16, 0))
                        .purpose("Guest Keynote: Advancements in Autonomous Multi-Agent Systems")
                        .status(com.campus.booking.enums.BookingStatus.APPROVED)
                        .adminRemark("Approved for campus lecture by Administration")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                bookingRepository.saveAll(java.util.List.of(b1, b2));

                // Seed initial notifications
                var n1 = com.campus.booking.entity.Notification.builder()
                        .user(student)
                        .message("Welcome to Smart Campus Booking! Your reservation request for " + classroom.getName() + " is pending review.")
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build();

                var n2 = com.campus.booking.entity.Notification.builder()
                        .user(faculty)
                        .message("Your keynote reservation for " + hall.getName() + " on " + tomorrow + " has been APPROVED.")
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build();

                notificationRepository.saveAll(java.util.List.of(n1, n2));
                log.info("Demo bookings and notifications successfully seeded.");
            }
        }
    }
}
