package com.academix.course.service;

import com.academix.common.models.dto.InternalUserDto;
import com.academix.course.clients.UserManagementServiceClient;
import com.academix.course.dto.CourseRequestDto;
import com.academix.course.dto.CourseResponseDto;
import com.academix.course.exception.ResourceNotFoundException;
import com.academix.course.exception.DuplicateCourseTitleException;
import com.academix.course.exception.UnauthorizedActionException;
import com.academix.course.model.Course;
import com.academix.course.model.CourseStatus;
import com.academix.course.repository.CourseRepository;
import com.academix.course.security.CourseServiceUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserManagementServiceClient userManagementServiceClient;

    /**
     * Creates a new course.
     * Only users with ROLE_INSTRUCTOR or ROLE_ADMIN can create courses.
     * The instructorId is derived from the authenticated user's JWT.
     *
     * @param courseRequestDto The DTO containing course details.
     * @return CourseResponseDto of the created course.
     * @throws DuplicateCourseTitleException if a course with the same title already exists.
     * @throws UnauthorizedActionException if the authenticated user is not an instructor or admin.
     * @throws ResourceNotFoundException if the instructor ID from the token does not exist in user service.
     */
    @Transactional
    public CourseResponseDto createCourse(CourseRequestDto courseRequestDto) {
        // 1. Get authenticated user's ID and roles from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CourseServiceUserDetails)) {
            throw new UnauthorizedActionException("Authentication required to create a course.");
        }
        CourseServiceUserDetails userDetails = (CourseServiceUserDetails) authentication.getPrincipal();
        Long instructorId = userDetails.getId();
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());

        log.info("Attempting to create course by user ID: {} with roles: {}", instructorId, roles);

        // 2. Validate user's role (already handled by @PreAuthorize in controller, but good for internal check)
        if (!roles.contains("INSTRUCTOR") && !roles.contains("ADMIN")) {
            throw new UnauthorizedActionException("Only instructors or administrators can create courses.");
        }

        // 3. Check if course title already exists
        if (courseRepository.existsByTitle(courseRequestDto.getTitle())) {
            throw new DuplicateCourseTitleException("Course with title '" + courseRequestDto.getTitle() + "' already exists.");
        }

        // 4. Validate instructor existence via User Management Service (optional but good practice)
        Optional<InternalUserDto> instructorDetails = userManagementServiceClient.getUserById(instructorId);
        if (instructorDetails.isEmpty()) {
            throw new ResourceNotFoundException("Instructor with ID " + instructorId + " not found in user management service.");
        }

        // 5. Build and save the course
        Course newCourse = Course.builder()
                .title(courseRequestDto.getTitle())
                .description(courseRequestDto.getDescription())
                .instructorId(instructorId)
                .status(CourseStatus.DRAFT) // New courses start as DRAFT
                .build();

        newCourse = courseRepository.save(newCourse);
        log.info("Course created successfully with ID: {}", newCourse.getId());

        // 6. Convert to Response DTO and enrich with instructor details
        return mapToCourseResponseDto(newCourse, instructorDetails.get());
    }

    /**
     * Retrieves a course by its ID, enriching it with instructor details from User Management Service.
     *
     * @param courseId The ID of the course to retrieve.
     * @return CourseResponseDto of the found course.
     * @throws ResourceNotFoundException if the course is not found.
     */
    @Transactional(readOnly = true)
    public CourseResponseDto getCourseById(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + courseId));

        return enrichCourseWithInstructorDetails(course);
    }

    /**
     * Retrieves all courses with pagination, enriching each with instructor details.
     *
     * @param pageable Pagination information.
     * @return Page of CourseResponseDto.
     */
    @Transactional(readOnly = true)
    public Page<CourseResponseDto> getAllCourses(Pageable pageable) {
        Page<Course> coursesPage = courseRepository.findAll(pageable);

        // Collect all unique instructor IDs to batch fetch details
        Set<Long> instructorIds = coursesPage.stream()
                .map(Course::getInstructorId)
                .collect(Collectors.toSet());

        // This would be a place to call a batch-get user endpoint if available.
        // For simplicity, we'll fetch individually, but in a real-world scenario,
        // you'd optimize this (e.g., using a Map<Long, InternalUserDto> from a batch call).
        List<CourseResponseDto> responseDtos = coursesPage.stream()
                .map(this::enrichCourseWithInstructorDetails) // This calls user service for each course
                .collect(Collectors.toList());

        return new PageImpl<>(responseDtos, pageable, coursesPage.getTotalElements());
    }

    /**
     * Updates an existing course.
     * Requires the authenticated user to be the course's instructor or an ADMIN.
     *
     * @param courseId The ID of the course to update.
     * @param courseRequestDto The DTO containing updated course details.
     * @return CourseResponseDto of the updated course.
     * @throws ResourceNotFoundException if the course is not found.
     * @throws UnauthorizedActionException if the user is not authorized to update the course.
     * @throws DuplicateCourseTitleException if the new title already exists for another course.
     */
    @Transactional
    public CourseResponseDto updateCourse(Long courseId, CourseRequestDto courseRequestDto) {
        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + courseId));

        // Get authenticated user's ID and roles for authorization
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CourseServiceUserDetails userDetails = (CourseServiceUserDetails) authentication.getPrincipal();
        Long authenticatedUserId = userDetails.getId();
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());

        // Authorization check: Must be ADMIN or the course's instructor
        if (!roles.contains("ADMIN") && !existingCourse.getInstructorId().equals(authenticatedUserId)) {
            throw new UnauthorizedActionException("You are not authorized to update this course.");
        }

        // Check for duplicate title if title is being changed
        if (!existingCourse.getTitle().equalsIgnoreCase(courseRequestDto.getTitle())) {
            if (courseRepository.existsByTitle(courseRequestDto.getTitle())) {
                throw new DuplicateCourseTitleException("Course with title '" + courseRequestDto.getTitle() + "' already exists.");
            }
            existingCourse.setTitle(courseRequestDto.getTitle());
        }

        // Update description
        existingCourse.setDescription(courseRequestDto.getDescription());
        // Note: instructorId and status are not updated via this general update endpoint.
        // Instructor changes and status changes should be separate admin/specific actions.

        Course updatedCourse = courseRepository.save(existingCourse);
        log.info("Course ID {} updated successfully by user ID {}.", courseId, authenticatedUserId);

        return enrichCourseWithInstructorDetails(updatedCourse);
    }

    /**
     * Deletes (soft-deletes) a course by setting its status to DELETED.
     * Requires the authenticated user to be the course's instructor or an ADMIN.
     *
     * @param courseId The ID of the course to delete.
     * @throws ResourceNotFoundException if the course is not found.
     * @throws UnauthorizedActionException if the user is not authorized to delete the course.
     */
    @Transactional
    public void deleteCourse(Long courseId) {
        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + courseId));

        // Get authenticated user's ID and roles for authorization
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CourseServiceUserDetails userDetails = (CourseServiceUserDetails) authentication.getPrincipal();
        Long authenticatedUserId = userDetails.getId();
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());

        // Authorization check: Must be ADMIN or the course's instructor
        if (!roles.contains("ADMIN") && !existingCourse.getInstructorId().equals(authenticatedUserId)) {
            throw new UnauthorizedActionException("You are not authorized to delete this course.");
        }

        // Perform soft delete
        existingCourse.setStatus(CourseStatus.DELETED);
        courseRepository.save(existingCourse);
        log.info("Course ID {} soft-deleted successfully by user ID {}.", courseId, authenticatedUserId);
    }

    /**
     * Helper method to enrich a single Course entity with instructor details.
     */
    private CourseResponseDto enrichCourseWithInstructorDetails(Course course) {
        Optional<InternalUserDto> instructorDetails = userManagementServiceClient.getUserById(course.getInstructorId());

        return instructorDetails.map(userDto -> mapToCourseResponseDto(course, userDto))
                .orElseGet(() -> {
                    log.warn("Instructor details not found for ID: {} for course ID: {}", course.getInstructorId(), course.getId());
                    // Return DTO with default/null instructor details if not found
                    return mapToCourseResponseDto(course, null);
                });
    }

    /**
     * Maps Course entity to CourseResponseDto, optionally enriching with InternalUserDto.
     */
    private CourseResponseDto mapToCourseResponseDto(Course course, InternalUserDto instructorUserDto) {
        CourseResponseDto.CourseResponseDtoBuilder builder = CourseResponseDto.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .instructorId(course.getInstructorId())
                .status(course.getStatus())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt());

        if (instructorUserDto != null) {
            builder.instructorUsername(instructorUserDto.getUsername())
                    .instructorFirstName(instructorUserDto.getFirstName())
                    .instructorLastName(instructorUserDto.getLastName())
                    .instructorRoles(instructorUserDto.getRoles());
        }
        return builder.build();
    }
}