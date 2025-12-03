package com.llm.toon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParseRequest {
@NotBlank(message = "data is required")
private String data; // free-form text
}
