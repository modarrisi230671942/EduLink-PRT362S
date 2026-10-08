package za.ac.mycput.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.ac.mycput.dto.AdminDtos.PublicStatsResponse;
import za.ac.mycput.service.IAdminService;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Public", description = "Data for the landing page (no login needed)")
public class PublicController {

    private final IAdminService adminService;

    public PublicController(IAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Headline numbers: open jobs, verified companies, registered students")
    public PublicStatsResponse stats() {
        return adminService.publicStats();
    }
}
