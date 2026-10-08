package za.ac.mycput.security;

import za.ac.mycput.domain.enums.UserType;

/**
 * The logged-in user, available in controllers via {@code @AuthenticationPrincipal AuthUser me}.
 * Built from the database on every request, so a disabled account is locked out immediately.
 */
public record AuthUser(Integer userId, String email, UserType role) {
}
