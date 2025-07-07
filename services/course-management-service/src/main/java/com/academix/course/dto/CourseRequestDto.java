package com.academix.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRequestDto {

    @NotBlank(message = "Course title cannot be empty")
    @Size(min = 3, max = 255, message = "Course title must be between 3 and 255 characters")
    private String title;

    @NotBlank(message = "Course description cannot be empty")
    @Size(min = 10, max = 1000, message = "Course description must be between 10 and 1000 characters")
    private String description;
}