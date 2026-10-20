package zw.co.petrotrade.workflow.transport.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

// returnDate asks to keep the vehicle until then; leave it out to only explain why the vehicle is late
public record ExtensionRequest(LocalDate returnDate, @NotBlank String reason) {
}
