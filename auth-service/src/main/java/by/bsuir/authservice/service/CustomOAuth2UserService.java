package by.bsuir.authservice.service;

import by.bsuir.authservice.DTO.RegisterRequest;
import by.bsuir.authservice.feign.EmployeeClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final EmployeeClient employeeClient;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String email;
        String name;

        if (registrationId.equals("google")) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
        } else if (registrationId.equals("yandex")) {
            email = (String) attributes.get("default_email");
            name = (String) attributes.get("real_name");
        } else {
            throw new RuntimeException("Unsupported provider: " + registrationId);
        }

        saveOrUpdateOAuthUser(email, name);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_worker")),
                attributes,
                registrationId.equals("yandex") ? "default_email" : "email"
        );
    }

    private void saveOrUpdateOAuthUser(String email, String name) {
        try {
            employeeClient.getByLogin(email.toLowerCase());
        } catch (FeignException.NotFound ex) {
            RegisterRequest newUser = new RegisterRequest();
            newUser.setLogin(email.toLowerCase());
            newUser.setFirstName(name != null ? name : "OAuth2User");
            newUser.setSecondName("");
            newUser.setSurname("");
            newUser.setPhone("");
            newUser.setTitle("worker");
            newUser.setPassword("N/A");

            employeeClient.createEmployee(newUser);
        }  catch (FeignException fe) {
        System.out.println("dsadsadsasaddsadsa");
        throw fe;
    }

    }
}
