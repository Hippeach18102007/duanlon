package com.retailmanagement.service;

import com.retailmanagement.dto.response.CartItemResponse;
import com.retailmanagement.entity.CartItem;
import com.retailmanagement.entity.Customer;
import com.retailmanagement.entity.ProductVariant;
import com.retailmanagement.repository.CartItemRepository;
import com.retailmanagement.repository.CustomRes;
import com.retailmanagement.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final CustomRes customerRepository;
    private final ProductVariantRepository productVariantRepository;


    // =================================================
    // ADD TO CART
    // =================================================
    @Transactional
    public void addToCart(Integer customerId, Integer variantId, Integer quantity) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        CartItem cartItem = cartItemRepository
                .findByCustomerIdAndProductVariantId(customerId, variantId)
                .orElseGet(() -> {
                    CartItem ci = new CartItem();
                    ci.setCustomer(customer);
                    ci.setProductVariant(variant);
                    ci.setQuantity(0);
                    ci.setCreatedAt(Instant.now());
                    return ci;
                });

        cartItem.setQuantity(cartItem.getQuantity() + quantity);
        cartItem.setUpdatedAt(Instant.now());

        cartItemRepository.save(cartItem);
    }


    // =================================================
    // GET CART ITEMS
    // =================================================
    public List<CartItemResponse> getCartItems(Integer customerId) {
        return cartItemRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }



    // =================================================
    // UPDATE QUANTITY
    // =================================================
    @Transactional
    public void updateQuantity(Integer cartItemId, Integer quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        item.setQuantity(quantity);
        item.setUpdatedAt(Instant.now());
        cartItemRepository.save(item);
    }

    // =================================================
    // REMOVE ITEM
    // =================================================
    @Transactional
    public void removeItem(Integer cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    // =================================================
    // CART COUNT (BADGE)
    // =================================================
    public long count(Integer customerId) {
        return cartItemRepository.countByCustomerId(customerId);
    }




    // =================================================
    // CLEAR CART (SAU KHI CREATE ORDER)
    // =================================================
    @Transactional
    public void clearCart(Integer customerId) {
        List<CartItem> items = cartItemRepository.findByCustomerId(customerId);
        cartItemRepository.deleteAll(items);
    }


    // =================================================
    // MAPPER
    // =================================================
    private CartItemResponse toResponse(CartItem item) {
        CartItemResponse dto = new CartItemResponse();
        dto.setCartItemId(item.getId());

        var variant = item.getProductVariant();
        var product = variant.getProduct();

        dto.setVariantId(variant.getId());
        dto.setProductName(product.getName());
        dto.setVariantName(variant.getVariantName());
        dto.setPrice(variant.getPrice());
        dto.setQuantity(item.getQuantity());

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            String url = product.getImages().stream()
                    .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                    .map(img -> img.getUrl())
                    .findFirst()
                    .orElse(product.getImages().get(0).getUrl());
            dto.setImageUrl(url);
        }

        return dto;
    }
}
