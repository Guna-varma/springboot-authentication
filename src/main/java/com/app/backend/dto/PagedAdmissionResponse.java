package com.app.backend.dto;

import com.app.backend.entity.AdmissionForm;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedAdmissionResponse {

    @JsonProperty("content")
    private List<AdmissionForm> admissions;

    @JsonProperty("currentPage")
    private int currentPage;

    @JsonProperty("totalPages")
    private int totalPages;

    @JsonProperty("totalElements")
    private long totalElements;

    @JsonProperty("pageSize")
    private int pageSize;

    @JsonProperty("hasNext")
    private boolean hasNext;

    @JsonProperty("hasPrevious")
    private boolean hasPrevious;

    @JsonProperty("isFirst")
    private boolean isFirst;

    @JsonProperty("isLast")
    private boolean isLast;

    @JsonProperty("numberOfElements")
    private int numberOfElements;

    @JsonProperty("empty")
    private boolean empty;
}
