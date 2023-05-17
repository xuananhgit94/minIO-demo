package com.example.demominio.model;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImageFile {
    private String fileName;
    private String url;
}
