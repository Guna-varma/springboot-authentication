package com.app.backend.dto;

import com.app.backend.entity.DocumentEntity;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentMetadataDTO {
    private Long id;
    private String filename;
    private String contentType;
    private Long size;
    private Long uploadedByUserId;
    private LocalDateTime uploadedAt;
    private DocumentEntity.DocumentType documentType;
    private String sha256Checksum;
}






//package com.app.backend.dto;
//
//import lombok.*;
//
//import java.time.LocalDateTime;
//
//@Data
//@Getter
//@Setter
//@AllArgsConstructor
//@NoArgsConstructor
//public class ImageMetadataDTO {
//    private Long id;
//    private String filename;
//    private String contentType;
//    private Long size;
//    private Long uploadedByUserId;
//    private LocalDateTime uploadedAt;
//
//}
