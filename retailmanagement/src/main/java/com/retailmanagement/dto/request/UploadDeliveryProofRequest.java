package com.retailmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadDeliveryProofRequest {

    @NotBlank
    private String proofUrl;
}

