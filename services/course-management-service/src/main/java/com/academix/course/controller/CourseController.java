package com.academix.course.controller;

import com.academix.common.models.dto.MessageResponseDto;
import com.academix.course.dto.CourseRequestDto;
import com.academix.course.dto.CourseResponseDto;
import com.academix.course.exception.DuplicateCourseTitleException;
import com.academix.course.exception.ResourceNotFoundException;
import com.academix.course.exception.UnauthorizedActionException;
import com.academix.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseController {

    private final CourseService courseService;

    /**
     * Creates a new course.
     * Requires authentication and specific roles (INSTRUCTOR or ADMIN).
     *
     * @param courseRequestDto The DTO containing course details.
     * @return ResponseEntity with the created CourseResponseDto.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<CourseResponseDto> createCourse(@Valid @RequestBody CourseRequestDto courseRequestDto) {
        log.info("Received request to create course: {}", courseRequestDto.getTitle());
        try {
            CourseResponseDto createdCourse = courseService.createCourse(courseRequestDto);
            return new ResponseEntity<>(createdCourse, HttpStatus.CREATED);
        } catch (DuplicateCourseTitleException e) {
            log.warn("Failed to create course due to duplicate title: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict
        } catch (UnauthorizedActionException e) {
            log.error("Unauthorized attempt to create course: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden
        } catch (ResourceNotFoundException e) {
            log.error("Error creating course: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // 400 Bad Request if instructor not found
        } catch (Exception e) {
            log.error("An unexpected error occurred while creating course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves a course by its ID.
     * Accessible by any authenticated user.
     *
     * @param id The ID of the course to retrieve.
     * @return ResponseEntity with the CourseResponseDto.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CourseResponseDto> getCourseById(@PathVariable Long id) {
        log.info("Received request to get course by ID: {}", id);
        try {
            CourseResponseDto course = courseService.getCourseById(id);
            return ResponseEntity.ok(course);
        } catch (ResourceNotFoundException e) {
            log.warn("Course with ID {} not found.", id);
            return ResponseEntity.notFound().build(); // 404 Not Found
        } catch (Exception e) {
            log.error("An unexpected error occurred while retrieving course by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves all courses with pagination.
     * Accessible by any authenticated user.
     *
     * @param pageable Pagination information (e.g., ?page=0&size=10&sort=title,asc).
     * @return ResponseEntity with a Page of CourseResponseDto.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<CourseResponseDto>> getAllCourses(Pageable pageable) {
        log.info("Received request to get all courses (page: {}, size: {})", pageable.getPageNumber(), pageable.getPageSize());
        try {
            Page<CourseResponseDto> coursesPage = courseService.getAllCourses(pageable);
            return ResponseEntity.ok(coursesPage);
        } catch (Exception e) {
            log.error("An unexpected error occurred while retrieving all courses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Updates an existing course.
     * Requires the authenticated user to be the course's instructor or an ADMIN.
     *
     * @param id The ID of the course to update.
     * @param courseRequestDto The DTO containing updated course details.
     * @return ResponseEntity with the updated CourseResponseDto.
     */
    @PutMapping("/{id}") // Use PUT for full replacement, PATCH for partial. Here, PUT implies full update.
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<CourseResponseDto> updateCourse(@PathVariable Long id,
                                                          @Valid @RequestBody CourseRequestDto courseRequestDto) {
        log.info("Received request to update course ID: {}", id);
        try {
            CourseResponseDto updatedCourse = courseService.updateCourse(id, courseRequestDto);
            return ResponseEntity.ok(updatedCourse); // 200 OK
        } catch (ResourceNotFoundException e) {
            log.warn("Course with ID {} not found for update.", id);
            return ResponseEntity.notFound().build(); // 404 Not Found
        } catch (UnauthorizedActionException e) {
            log.error("Unauthorized attempt to update course ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden
        } catch (DuplicateCourseTitleException e) {
            log.warn("Failed to update course ID {} due to duplicate title: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 Conflict
        } catch (Exception e) {
            log.error("An unexpected error occurred while updating course ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Deletes (soft-deletes) a course by setting its status to DELETED.
     * Requires the authenticated user to be the course's instructor or an ADMIN.
     *
     * @param id The ID of the course to delete.
     * @return ResponseEntity with a success message.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<MessageResponseDto> deleteCourse(@PathVariable Long id) {
        log.info("Received request to delete course ID: {}", id);
        try {
            courseService.deleteCourse(id);
            return new ResponseEntity<>(new MessageResponseDto("Course soft-deleted successfully."), HttpStatus.OK); // 200 OK
        } catch (ResourceNotFoundException e) {
            log.warn("Course with ID {} not found for deletion.", id);
            return ResponseEntity.notFound().build(); // 404 Not Found
        } catch (UnauthorizedActionException e) {
            log.error("Unauthorized attempt to delete course ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden
        } catch (Exception e) {
            log.error("An unexpected error occurred while deleting course ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}