package io.mosip.registration.validator;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_FINGERPRINT_FACADE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.bioapi.model.Score;
import io.mosip.kernel.core.bioapi.spi.IBioApi;
import io.mosip.kernel.core.cbeffutil.entity.BDBInfo;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.entity.BIR.BIRBuilder;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.biometric.FingerprintDetailsDTO;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.service.security.impl.AuthenticationServiceImpl;

/**
 * This class is for validating Fingerprint Authentication
 * 
 * @author SaravanaKumar G
 *
 */
@Service("fingerprintValidator")
public class FingerprintValidatorImpl extends AuthenticationBaseValidator {

	@Autowired
	private UserDetailDAO userDetailDAO;

	@Autowired
	@Qualifier("finger")	
	IBioApi ibioApi;

	/**
	 * Instance of LOGGER
	 */
	private static final Logger LOGGER = AppConfig.getLogger(AuthenticationServiceImpl.class);

	/**
	 * Validate the Fingerprint with the AuthenticationValidatorDTO as input
	 */
	@Override
	public boolean validate(AuthenticationValidatorDTO authenticationValidatorDTO) {
		LOGGER.info(LoggerConstants.FINGER_PRINT_AUTHENTICATION, APPLICATION_NAME, APPLICATION_ID,
				"Validating Scanned Finger");

		if (RegistrationConstants.SINGLE.equals(authenticationValidatorDTO.getAuthValidationType())) {
			return validateOneToManyFP(authenticationValidatorDTO.getUserId(),
					authenticationValidatorDTO.getFingerPrintDetails().get(0));
		} else if (RegistrationConstants.MULTIPLE
				.equals(authenticationValidatorDTO.getAuthValidationType())) {
			return validateManyToManyFP(authenticationValidatorDTO.getFingerPrintDetails());
		}
		return false;
	}

	/**
	 * Validate one finger print values with all the fingerprints from the table.
	 * 
	 * @param userId
	 * @param capturedFingerPrintDto
	 * @return
	 */
	private boolean validateOneToManyFP(String userId, FingerprintDetailsDTO capturedFingerPrintDto) {
		List<UserBiometric> userFingerprintDetails = userDetailDAO.getUserSpecificBioDetails(userId,
				RegistrationConstants.FIN);
		return validateFpWithBioApi(capturedFingerPrintDto, userFingerprintDetails);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.bio.BioService#validateFpWithBioApi(io.mosip.
	 * registration.dto.biometric.FingerprintDetailsDTO, java.util.List)
	 */
	private boolean validateFpWithBioApi(FingerprintDetailsDTO capturedFingerPrintDto,
			List<UserBiometric> userFingerprintDetails) {

		BIR capturedBir = new BIRBuilder().withBdb(capturedFingerPrintDto.getFingerPrintISOImage()).withBdbInfo(new BDBInfo.BDBInfoBuilder().withType(Collections.singletonList(SingleType.FINGER)).build()).build();
		BIR[] registeredBir = new BIR[userFingerprintDetails.size()];
		Score[] scores = null;
		boolean flag = false;
		int i = 0;
		ApplicationContext.map().remove("IDENTY_SDK");
		for (UserBiometric userBiometric : userFingerprintDetails) {
			registeredBir[i] = new BIRBuilder().withBdb(userBiometric.getBioIsoImage()).withBdbInfo(new BDBInfo.BDBInfoBuilder().withType(Collections.singletonList(SingleType.FINGER)).build()).build();
			i++;
		}
		try {
			scores = ibioApi.match(capturedBir,registeredBir, null);
			int faceScore = 80;
			for (Score score : scores) {
				if (score.getInternalScore() >= faceScore) {
					flag = true;
				}
			}
		} catch (BiometricException exception) {
			LOGGER.error(LOG_REG_FINGERPRINT_FACADE, APPLICATION_NAME, APPLICATION_ID, String.format(
					"Exception while validating the finger print with bio api: %s caused by %s",
					exception.getMessage(), exception.getCause()));
			ApplicationContext.map().put("IDENTY_SDK", "FAILED");
			return false;
		}
		return flag;
		
	
	}

	/**
	 * Validate all the user input finger details with all the finger details form
	 * the DB.
	 * 
	 * @param capturedFingerPrintDetails
	 * @return
	 */
	private boolean validateManyToManyFP(List<FingerprintDetailsDTO> capturedFingerPrintDetails) {
		Boolean isMatchFound = false;
		for (FingerprintDetailsDTO fingerprintDetailsDTO : capturedFingerPrintDetails) {
			isMatchFound = validateFpWithBioApi(fingerprintDetailsDTO,
					userDetailDAO.getAllActiveUsers("FingerPrint "+ fingerprintDetailsDTO.getFingerType()));
			if (isMatchFound) {
				SessionContext.map().put(RegistrationConstants.DUPLICATE_FINGER, fingerprintDetailsDTO);
				break;
			}
		}
		return isMatchFound;

	}

	@Override
	public AuthTokenDTO validate(String userId, String otp, boolean haveToSaveAuthToken) {
		return null;
	}	
}
