package com.retailmanagement.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ProductSerialRequest {

    @NotEmpty(message = "Danh sách số seri không được rỗng")
    private List<String> serialNumbers;
}