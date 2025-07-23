package com.microblogging.project.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class TweetRequest {

    @NotBlank(message = "Tweet content cannot be empty")
    @Size(max = 280, message = "Tweet content cannot exceed 280 characters")
    private String content;
}