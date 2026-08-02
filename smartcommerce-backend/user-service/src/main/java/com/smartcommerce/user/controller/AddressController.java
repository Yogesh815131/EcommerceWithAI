package com.smartcommerce.user.controller;

import com.smartcommerce.user.dto.AddressRequest;
import com.smartcommerce.user.entity.Address;
import com.smartcommerce.user.security.CurrentUser;
import com.smartcommerce.user.security.CurrentUserResolver;
import com.smartcommerce.user.service.AddressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public ResponseEntity<List<Address>> list(HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        return ResponseEntity.ok(addressService.listForUser(user.getUserId()));
    }

    @PostMapping
    public ResponseEntity<Address> add(@Valid @RequestBody AddressRequest request, HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        Address saved = addressService.add(user.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<Address> update(@PathVariable Long addressId,
                                           @Valid @RequestBody AddressRequest request,
                                           HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        Address updated = addressService.update(addressId, user.getUserId(), request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(@PathVariable Long addressId, HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        addressService.delete(addressId, user.getUserId());
        return ResponseEntity.noContent().build();
    }
}
