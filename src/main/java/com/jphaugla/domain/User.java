package com.jphaugla.domain;

import lombok.*;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class User implements Serializable {
        private String username;
        private String password;
        private String timestamp;
}
