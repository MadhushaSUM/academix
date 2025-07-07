package com.academix.course.dto;

import com.academix.course.model.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponseDto {
    private Long id;
    private String title;
    private String description;
    private Long instructorId;
    // will add instructor details (username, firstName, lastName) here using inter-service communication
    private String instructorUsername;
    private String instructorFirstName;
    private String instructorLastName;
    private Set<String> instructorRoles;
    private CourseStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}