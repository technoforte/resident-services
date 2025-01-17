package io.mosip.resident.controller;

import java.io.ByteArrayInputStream;

import javax.validation.Valid;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.kernel.core.http.ResponseFilter;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.resident.dto.CredentialCancelRequestResponseDto;
import io.mosip.resident.dto.CredentialRequestStatusResponseDto;
import io.mosip.resident.dto.CredentialTypeResponse;
import io.mosip.resident.dto.PartnerCredentialTypePolicyDto;
import io.mosip.resident.dto.RequestWrapper;
import io.mosip.resident.dto.ResidentCredentialRequestDto;
import io.mosip.resident.dto.ResidentCredentialResponseDto;
import io.mosip.resident.exception.ResidentServiceCheckedException;
import io.mosip.resident.service.ResidentCredentialService;
import io.mosip.resident.util.AuditUtil;
import io.mosip.resident.util.EventEnum;
import io.mosip.resident.validator.RequestValidator;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "resident-credential-controller", description = "Resident Credential Controller")
public class ResidentCredentialController {

	@Autowired
	private RequestValidator validator;

	@Autowired
	private ResidentCredentialService residentCredentialService;

	@Autowired
	private AuditUtil audit;

	@ResponseFilter
	@PostMapping(value = "/req/credential")
	@Operation(summary = "reqCredential", description = "reqCredential", tags = { "resident-credential-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<Object> reqCredential(@RequestBody RequestWrapper<ResidentCredentialRequestDto> requestDTO)
			throws ResidentServiceCheckedException {
		audit.setAuditRequestDto(EventEnum.CREDENTIAL_REQ);
		ResponseWrapper<ResidentCredentialResponseDto> response = new ResponseWrapper<>();
		response.setResponse(residentCredentialService.reqCredential(requestDTO.getRequest()));
		audit.setAuditRequestDto(EventEnum.CREDENTIAL_REQ_SUCCESS);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	@GetMapping(value = "req/credential/status/{requestId}")
	@Operation(summary = "getCredentialStatus", description = "getCredentialStatus", tags = { "resident-credential-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<Object> getCredentialStatus(@PathVariable("requestId") String requestId)
			throws ResidentServiceCheckedException {
		audit.setAuditRequestDto(EventEnum.CREDENTIAL_REQ_STATUS);
		ResponseWrapper<CredentialRequestStatusResponseDto> response = new ResponseWrapper<>();
		response.setResponse(residentCredentialService.getStatus(requestId));
		audit.setAuditRequestDto(EventEnum.CREDENTIAL_REQ_STATUS_SUCCESS);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@GetMapping(value = "req/card/{requestId}")
	@Operation(summary = "getCard", description = "getCard", tags = { "resident-credential-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<Object> getCard(@PathVariable("requestId") String requestId)
			throws Exception {
		audit.setAuditRequestDto(EventEnum.REQ_CARD);
		byte[] pdfBytes = residentCredentialService.getCard(requestId);
		InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(pdfBytes));
		audit.setAuditRequestDto(EventEnum.REQ_CARD_SUCCESS);
		return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/pdf"))
				.header("Content-Disposition", "attachment; filename=\"" + requestId + ".pdf\"")
				.body((Object) resource);
	}
	
	@GetMapping(value = "credential/types")
	@Operation(summary = "getCredentialTypes", description = "getCredentialTypes", tags = { "resident-credential-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<Object> getCredentialTypes()
			throws ResidentServiceCheckedException {
		audit.setAuditRequestDto(EventEnum.CREDENTIAL_TYPES);
		ResponseWrapper<CredentialTypeResponse> response = new ResponseWrapper<>();
		response.setResponse(residentCredentialService.getCredentialTypes());
		audit.setAuditRequestDto(EventEnum.CREDENTIAL_TYPES_SUCCESS);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}


	@GetMapping(value = "req/credential/cancel/{requestId}")
	@Operation(summary = "cancelCredentialRequest", description = "cancelCredentialRequest", tags = { "resident-credential-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<Object> cancelCredentialRequest(@PathVariable("requestId") String requestId)
			throws ResidentServiceCheckedException {
		audit.setAuditRequestDto(EventEnum.CREDENTIAL_CANCEL_REQ);
		ResponseWrapper<CredentialCancelRequestResponseDto> response = new ResponseWrapper<>();
		response.setResponse(residentCredentialService.cancelCredentialRequest(requestId));
		audit.setAuditRequestDto(EventEnum.CREDENTIAL_CANCEL_REQ_SUCCESS);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@GetMapping(value = "req/policy/partnerId/{partnerId}/credentialType/{credentialType}")
	@Operation(summary = "getPolicyByCredentialType", description = "getPolicyByCredentialType", tags = { "resident-credential-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<Object> getPolicyByCredentialType(@PathVariable @Valid String partnerId,
			@PathVariable @Valid String credentialType) throws ResidentServiceCheckedException {
		audit.setAuditRequestDto(EventEnum.REQ_POLICY);
		io.mosip.resident.dto.ResponseWrapper<PartnerCredentialTypePolicyDto> response = residentCredentialService
				.getPolicyByCredentialType(partnerId, credentialType);
		audit.setAuditRequestDto(EventEnum.REQ_POLICY_SUCCESS);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

}
