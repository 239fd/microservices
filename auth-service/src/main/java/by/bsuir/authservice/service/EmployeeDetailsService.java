package by.bsuir.authservice.service;

import by.bsuir.authservice.DTO.EmployeeDto;
import by.bsuir.authservice.feign.EmployeeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeDetailsService implements UserDetailsService {

    private final EmployeeClient employeeClient;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        EmployeeDto employee;
        try {
            employee = employeeClient.getByLogin(login).getData();
        } catch (Exception e) {
            throw new UsernameNotFoundException("Not found: " + login);
        }

        return User.withUsername(employee.getLogin())
                .password(employee.getEncodedPassword())
                .roles(employee.getTitle().toUpperCase())
                .build();
    }
}
