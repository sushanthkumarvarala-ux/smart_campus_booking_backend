package com.campus.booking.dto.auth;

import com.campus.booking.enums.Role;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;

    private String name;

    private String email;

    private Role role;

    private boolean enabled;

    private LocalDateTime createdAt;
}
