package com.campus.booking.dto.auth;

import com.campus.booking.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    @Builder.Default
    private String type = "Bearer";

    private Long id;

    private String name;

    private String email;

    private Role role;
}
