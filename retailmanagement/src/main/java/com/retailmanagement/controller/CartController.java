package com.retailmanagement.controller;

import com.retailmanagement.dto.request.AddToCartRequest;
import com.retailmanagement.dto.response.CartItemResponse;
import com.retailmanagement.security.service.CustomUserDetails;
import com.retailmanagement.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // =================================================
    // ADD TO CART
    // =================================================
    @PostMapping("/items")
    public ResponseEntity<Void> addToCart(
            @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        cartService.addToCart(
                user.getCustomerId(),              // userId chỉ dùng để map Customer
                request.getVariantId(),
                request.getQuantity()
        );
        return ResponseEntity.ok().build();
    }

    // =================================================
    // GET CART ITEMS
    // =================================================
    @GetMapping("/items")
    public ResponseEntity<List<CartItemResponse>> getCartItems(
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(
                cartService.getCartItems(user.getCustomerId())
        );
    }

    // =================================================
    // UPDATE QUANTITY
    // =================================================
    @PutMapping("/items/{id}")
    public ResponseEntity<Void> updateQuantity(
            @PathVariable Integer id,
            @RequestParam Integer quantity) {

        cartService.updateQuantity(id, quantity);
        return ResponseEntity.ok().build();
    }

    // =================================================
    // REMOVE ITEM
    // =================================================
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> removeItem(@PathVariable Integer id) {
        cartService.removeItem(id);
        return ResponseEntity.ok().build();
    }

    // =================================================
    // CART COUNT (BADGE)
    // =================================================
    @GetMapping("/count")
    public ResponseEntity<Long> countItems(
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(
                cartService.count(user.getCustomerId())
        );
    }

    // =================================================
    // CLEAR CART
    // =================================================
    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal CustomUserDetails user) {

        cartService.clearCart(user.getCustomerId());
        return ResponseEntity.ok().build();
    }

}

