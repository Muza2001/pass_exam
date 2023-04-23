package passtestapplication.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import passtestapplication.dto.request.*;
import passtestapplication.dto.response.UserEditPassword;
import passtestapplication.service.AuthenticationService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRequest dto){
        return ResponseEntity.status(201).body(service.register(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest dto){
        return ResponseEntity.status(200).body(service.login(dto));
    }

    @GetMapping("/get_all")
    public ResponseEntity<?> getAll(){
        return ResponseEntity.status(200).body(service.getAll());
    }

    @GetMapping("/find_by_id/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id){
        return ResponseEntity.status(200).body(service.findById(id));
    }

    @PutMapping("/edit")
    public ResponseEntity<?> edit(@RequestBody UserEditRequest request){
        return ResponseEntity.status(200).body(service.edit(request));
    }

    @PutMapping("/edit_password")
    public ResponseEntity<?> editPassword(UserEditPassword userEditPassword){
        return ResponseEntity.status(200).body(service.editPassword(userEditPassword));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id){
        return ResponseEntity.status(200).body(service.delete(id));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshTokenRequest request){
        return ResponseEntity.status(200).body(service.logout(request));
    }
}
