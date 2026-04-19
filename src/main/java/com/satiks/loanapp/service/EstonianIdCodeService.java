package com.satiks.loanapp.service;

import com.satiks.loanapp.exception.BusinessException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import org.springframework.stereotype.Service;

/**
 * Validates Estonian personal ID codes and derives birth date/age metadata from them.
 */
@Service
public class EstonianIdCodeService {

    /**
     * Validates personal ID code structure and semantic correctness.
     *
     * @param personalIdCode personal ID code to validate
     * @throws BusinessException when format, checksum, or date parts are invalid
     */
    public void validate(String personalIdCode) {
        extractBirthDate(personalIdCode);
    }

    /**
     * Extracts the birth date encoded in an Estonian personal ID code.
     *
     * @param personalIdCode personal ID code
     * @return birth date parsed from the code
     * @throws BusinessException when code is invalid or contains impossible date values
     */
    public LocalDate extractBirthDate(String personalIdCode) {
        validateFormat(personalIdCode);
        validateChecksum(personalIdCode);

        // First digit encodes both sex and century according to the Estonian ID format.
        int century = switch (personalIdCode.charAt(0)) {
            case '1', '2' -> 1800;
            case '3', '4' -> 1900;
            case '5', '6' -> 2000;
            case '7', '8' -> 2100;
            default -> throw new BusinessException("Invalid Estonian ID code format");
        };

        int year = century + parseSegment(personalIdCode, 1, 3);
        int month = parseSegment(personalIdCode, 3, 5);
        int day = parseSegment(personalIdCode, 5, 7);

        try {
            LocalDate birthDate = LocalDate.of(year, month, day);
            if (birthDate.isAfter(LocalDate.now())) {
                throw new BusinessException("Invalid Estonian ID code birth date");
            }
            return birthDate;
        } catch (DateTimeException ex) {
            throw new BusinessException("Invalid Estonian ID code birth date");
        }
    }

    /**
     * Calculates applicant age in full years from an Estonian personal ID code.
     *
     * @param personalIdCode personal ID code
     * @return age in years
     * @throws BusinessException when personal ID code is invalid
     */
    public int calculateAgeInYears(String personalIdCode) {
        return Period.between(extractBirthDate(personalIdCode), LocalDate.now()).getYears();
    }

    private void validateFormat(String personalIdCode) {
        // Exactly 11 digits are required by the Estonian personal ID specification.
        if (personalIdCode == null || !personalIdCode.matches("\\d{11}")) {
            throw new BusinessException("Invalid Estonian ID code format");
        }
    }

    private void validateChecksum(String personalIdCode) {
        int expectedCheckDigit = calculateChecksumDigit(personalIdCode);
        int actualCheckDigit = Character.digit(personalIdCode.charAt(10), 10);

        if (expectedCheckDigit != actualCheckDigit) {
            throw new BusinessException("Invalid Estonian ID code checksum");
        }
    }

    private int calculateChecksumDigit(String personalIdCode) {
        int[] firstChecksumWeights = {1, 2, 3, 4, 5, 6, 7, 8, 9, 1};
        int[] secondChecksumWeights = {3, 4, 5, 6, 7, 8, 9, 1, 2, 3};

        int firstRemainder = weightedSum(personalIdCode, firstChecksumWeights) % 11;
        if (firstRemainder < 10) {
            return firstRemainder;
        }

        int secondRemainder = weightedSum(personalIdCode, secondChecksumWeights) % 11;
        return secondRemainder < 10 ? secondRemainder : 0;
    }

    private int weightedSum(String personalIdCode, int[] weights) {
        int sum = 0;
        for (int index = 0; index < weights.length; index++) {
            sum += Character.digit(personalIdCode.charAt(index), 10) * weights[index];
        }
        return sum;
    }

    private int parseSegment(String personalIdCode, int startInclusive, int endExclusive) {
        return Integer.parseInt(personalIdCode.substring(startInclusive, endExclusive));
    }
}