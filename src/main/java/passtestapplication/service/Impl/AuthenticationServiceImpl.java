package passtestapplication.service.Impl;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import passtestapplication.dto.request.AuthenticationRequest;
import passtestapplication.dto.request.RefreshTokenRequest;
import passtestapplication.dto.request.UserEditRequest;
import passtestapplication.dto.request.UserRequest;
import passtestapplication.dto.response.AuthenticationResponse;
import passtestapplication.dto.response.Response;
import passtestapplication.dto.response.UserEditPassword;
import passtestapplication.dto.response.UserResponse;
import passtestapplication.model.User;
import passtestapplication.repository.UserRepository;
import passtestapplication.security.JwtProvider;
import passtestapplication.service.AuthenticationService;
import passtestapplication.service.RefreshTokenService;

import java.time.Instant;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService, UserDetailsService {

    private final UserRepository userRepository;

    private final PasswordEncoder encoder;

    private final RefreshTokenService refreshTokenService;

    private final JwtProvider jwtProvider;

    private final AuthenticationManager authenticationManager;

    @Override
    public ResponseEntity<?> register(UserRequest userDto) {
        User user = new User();
        Response response;
        if (userRepository.existsByUsername(userDto.getUsername())){
            response = Response.builder()
                    .message("Username already exists")
                    .status_code(400)
                    .success(false)
                    .build();
        }
        else {
            user.setUsername(userDto.getUsername());
            user.setExpiration(Instant.now());
            user.setFull_name(userDto.getFull_name());
            user.setPassword(encoder.encode(userDto.getPassword()));
            user.setIsEnabled(true);
            User save = userRepository.save(user);
            UserResponse userResponse = UserResponse.builder()
                    .id(save.getId())
                    .fullName(save.getFull_name())
                    .username(save.getUsername())
                    .build();
            response = Response.builder()
                    .success(true)
                    .message("User successfully created")
                    .status_code(201)
                    .data(userResponse)
                    .build();
        }
        return ResponseEntity.status(201).body(response);
    }

    @Override
    public ResponseEntity<?> login(AuthenticationRequest dto) {
        Authentication authenticate =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authenticate);
        String generateToken = jwtProvider.generateToken((org.springframework.security.core.userdetails.User) authenticate.getPrincipal());
        // TODO: 2/25/22 check if user login details match, if not handle it.

        AuthenticationResponse response = AuthenticationResponse.builder()
                .authenticationToken(generateToken)
                .refreshToken(refreshTokenService.generateRefreshToken().getRefreshToken())
                .username(dto.getUsername())
                .expirationData(Instant.now().plusMillis(18_000_000L))
                .build();
        return ResponseEntity.status(201).body(response);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username).orElseThrow(()
                -> new IllegalArgumentException("Email not found"));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getIsEnabled(),
                true,
                true,
                true,
                grantedAuthority("USER")
        );
    }

    private Collection<? extends GrantedAuthority> grantedAuthority(String user) {
        return Collections.singletonList(new SimpleGrantedAuthority(user));
    }

    public UserResponse getUserResponse(User user){
        UserResponse userResponse;
        if (user != null){
            userResponse = UserResponse.builder()
                    .username(user.getUsername())
                    .fullName(user.getFull_name())
                    .id(user.getId())
                    .build();
        }
        else {
            userResponse = null;
        }
        return userResponse;
    }

    @Override
    public ResponseEntity<?> findById(Long id) {
        Response response;
        Optional<User> byId = userRepository.findById(id);
        if (byId.isPresent()){
            User user = byId.get();
            UserResponse userResponse = getUserResponse(user);
            response = Response.builder()
                    .data(userResponse)
                    .status_code(200)
                    .success(true)
                    .message("User find")
                    .build();
        }
        else {
            response = Response.builder()
                    .message("User id not found")
                    .success(false)
                    .status_code(400)
                    .build();
        }
        return ResponseEntity.status(response.getStatus_code()).body(response);
    }

    @Override
    public ResponseEntity<?> edit(UserEditRequest request) {
        User user = getCurrentUser();
        Response response;
        if (user != null){
            UserResponse userResponse;
            if (!user.getUsername().equals(request.getUsername())){
                if (userRepository.existsByUsername(request.getUsername())){
                    response = Response.builder()
                            .status_code(400)
                            .success(false)
                            .message("Username already exists")
                            .build();
                    return ResponseEntity.status(400).body(response);
                }
                else {
                    user.setUsername(request.getUsername());
                }
            }

            if (!request.getFullName().equals(user.getFull_name()))
                user.setFull_name(request.getFullName());
            User save = userRepository.save(user);
            userResponse = getUserResponse(save);
            response = Response.builder()
                    .message("User successfully edited")
                    .success(true)
                    .status_code(200)
                    .data(userResponse)
                    .build();
        }
        else {
            response = Response.builder()
                    .success(false)
                    .message("User id not found")
                    .status_code(404)
                    .build();
        }
        return ResponseEntity.status(response.getStatus_code()).body(response);
    }

    public User getCurrentUser() {
        Jwt principal = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(principal.getSubject()).orElseThrow(()
                -> new UsernameNotFoundException("username Not found"));
    }

    @Override
    public ResponseEntity<?> editPassword(UserEditPassword userEditPassword) {
        User user = getCurrentUser();
        Response response;
        if (user != null) {
            UserResponse userResponse;
            if (user.getPassword().equals(encoder.encode(userEditPassword.getConfirm_password()))) {
                if (userEditPassword.getNew_password().equals(userEditPassword.getRetry_password())) {
                    user.setPassword(encoder.encode(userEditPassword.getNew_password()));
                    userRepository.save(user);
                    userResponse = getUserResponse(user);
                    response = Response.builder()
                            .success(true)
                            .message("User password successfully edited")
                            .status_code(200)
                            .data(userResponse)
                            .build();
                } else {
                    response = Response.builder()
                            .message("Retry password not equal")
                            .status_code(400)
                            .success(false)
                            .build();
                }
            } else {
                response = Response.builder()
                        .success(false)
                        .message("Confirm password wrong")
                        .status_code(400)
                        .build();
            }

        }
        else {
            response = Response.builder()
                    .status_code(400)
                    .message("User id not found")
                    .success(false)
                    .build();
        }

        return ResponseEntity.status(response.getStatus_code()).body(response);
    }

    @Override
    public ResponseEntity<?> delete(Long id) {
        Optional<User> byId = userRepository.findById(id);
        Response response;
        if (byId.isPresent()){
            User user = byId.get();
            userRepository.delete(user);
            response = Response.builder()
                    .message("User successfully deleted")
                    .status_code(200)
                    .success(true)
                    .build();
        }
        else {
            response = Response.builder()
                    .success(false)
                    .status_code(400)
                    .message("User id not found")
                    .build();
        }
        return ResponseEntity.status(response.getStatus_code()).body(response);
    }

    @Override
    public ResponseEntity<?> logout(RefreshTokenRequest request) {
        refreshTokenService.refreshTokenDelete(request);
        return ResponseEntity.status(200).body("Successfully logged out");
    }

    @Override
    public ResponseEntity<?> getAll() {
        List<UserResponse> userResponses = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            if (user != null) {
                userResponses.add(getUserResponse(user));
            }
        }
        Response response = Response.builder()
                .message("Users ")
                .status_code(200)
                .data(userResponses)
                .success(true)
                .build();
        return ResponseEntity.status(response.getStatus_code()).body(response);
    }
}
