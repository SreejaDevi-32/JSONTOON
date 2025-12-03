package com.llm.toon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseResponse {
private String productName;
private String category;
private Integer quantity;
private String saleDate; // ISO-8601 yyyy-MM-dd
private String location;
private boolean ambiguous; // true if low confidence/ambiguous
private String note; // explanation if ambiguous

}
