package com.example.timetracking.security;

import com.example.timetracking.model.Employee;
import com.example.timetracking.model.EmployeeRole;
import com.example.timetracking.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminPinAuthenticationProvider implements AuthenticationProvider {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String pin = authentication.getCredentials() == null ? null : authentication.getCredentials().toString();
        if (pin == null || pin.isBlank()) {
            throw new BadCredentialsException("PIN required");
        }

        List<Employee> admins = employeeRepository.findAllByRoleAndActiveIsTrue(EmployeeRole.SUPER_ADMIN);
        for (Employee admin : admins) {
            if (passwordEncoder.matches(pin, admin.getPinHash())) {
                return new UsernamePasswordAuthenticationToken(
                    admin.getId().toString(),
                    "",
                    List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                );
            }
        }

        throw new BadCredentialsException("Invalid PIN");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
