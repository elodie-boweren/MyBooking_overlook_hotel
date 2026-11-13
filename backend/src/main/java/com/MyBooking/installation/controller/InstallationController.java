package com.MyBooking.installation.controller;

import com.MyBooking.installation.domain.Installation;
import com.MyBooking.installation.dto.InstallationResponseDto;
import com.MyBooking.installation.service.InstallationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/installations")
@CrossOrigin(origins = "*")
public class InstallationController {
    
    @Autowired
    private InstallationService installationService;
    
    /**
     * Get all installations
     * Access: CLIENT, ADMIN
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<List<InstallationResponseDto>> getAllInstallations() {
        List<InstallationResponseDto> responseDtos = installationService.getAllInstallationsForApi();
        return ResponseEntity.ok(responseDtos);
    }
    
    /**
     * Get installation by ID
     * Access: CLIENT, ADMIN
     */
    @GetMapping("/{installationId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<InstallationResponseDto> getInstallationById(@PathVariable Long installationId) {
        InstallationResponseDto responseDto = installationService.getInstallationByIdForApi(installationId);
        return ResponseEntity.ok(responseDto);
    }
    
}
