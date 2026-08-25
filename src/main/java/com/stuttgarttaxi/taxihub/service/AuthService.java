package com.stuttgarttaxi.taxihub.service;

import com.stuttgarttaxi.taxihub.dto.RegisterForm;
import com.stuttgarttaxi.taxihub.entity.Employee;
import com.stuttgarttaxi.taxihub.entity.Role;
import com.stuttgarttaxi.taxihub.exception.EmailAlreadyRegisteredException;
import com.stuttgarttaxi.taxihub.exception.EmailDomainNotAllowedException;
import com.stuttgarttaxi.taxihub.exception.InvalidResetTokenException;
import com.stuttgarttaxi.taxihub.exception.InvalidVerificationCodeException;
import com.stuttgarttaxi.taxihub.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResendEmailService resendEmailService;

    @Value("${company.email-domain}")
    private String companyEmailDomain;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Value("${app.reset-token-validity-minutes}")
    private long resetTokenValidityMinutes;

    @Value("${app.verification-code-validity-minutes}")
    private long verificationCodeValidityMinutes;

    private static final SecureRandom RANDOM = new SecureRandom();

    public boolean isCompanyEmail(String email) {
        return email != null && email.toLowerCase().endsWith("@" + companyEmailDomain.toLowerCase());
    }

    public String companyEmailDomain() {
        return companyEmailDomain;
    }

    public long verificationCodeValidityMinutes() {
        return verificationCodeValidityMinutes;
    }

    /**
     * Settings > My Profile. Only name fields - email is the login identity
     * and password changes already have their own dedicated flow (Forgot
     * Password), so neither belongs on this form.
     */
    @Transactional
    public Employee updateOwnProfile(Long employeeId, String firstName, String lastName) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalStateException("Oturum açan kullanıcı bulunamadı"));
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee register(RegisterForm form) {
        String email = form.getEmail().trim().toLowerCase();

        if (!isCompanyEmail(email)) {
            throw new EmailDomainNotAllowedException(
                    "Sadece Stuttgart Taksi iş e-postası ile kayıt olabilirsiniz");
        }

        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException("Bu e-posta adresi zaten kayıtlı");
        }

        String code = generateVerificationCode();

        Employee employee = Employee.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(form.getPassword()))
                .firstName(form.getFirstName())
                .lastName(form.getLastName())
                .role(Role.EMPLOYEE)
                .verified(false)
                .verificationCode(code)
                .verificationCodeExpiry(LocalDateTime.now().plusMinutes(verificationCodeValidityMinutes))
                .build();

        employee = employeeRepository.save(employee);

        resendEmailService.sendVerificationEmail(employee.getEmail(), code, verificationCodeValidityMinutes);

        return employee;
    }

    private String generateVerificationCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    @Transactional
    public void resendVerificationCode(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();

        Employee employee = employeeRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidVerificationCodeException("Bu e-posta ile eşleşen bir kayıt bulunamadı"));

        if (employee.isVerified()) {
            return;
        }

        String code = generateVerificationCode();
        employee.setVerificationCode(code);
        employee.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(verificationCodeValidityMinutes));
        employeeRepository.save(employee);

        resendEmailService.sendVerificationEmail(employee.getEmail(), code, verificationCodeValidityMinutes);
    }

    @Transactional
    public void verifyEmail(String rawEmail, String code) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();

        Employee employee = employeeRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidVerificationCodeException("Doğrulama kodu geçersiz veya süresi dolmuş"));

        if (employee.isVerified()) {
            return;
        }

        boolean codeMatches = employee.getVerificationCode() != null
                && employee.getVerificationCode().equals(code);
        boolean notExpired = employee.getVerificationCodeExpiry() != null
                && employee.getVerificationCodeExpiry().isAfter(LocalDateTime.now());

        if (!codeMatches || !notExpired) {
            throw new InvalidVerificationCodeException("Doğrulama kodu geçersiz veya süresi dolmuş");
        }

        employee.setVerified(true);
        employee.setVerificationCode(null);
        employee.setVerificationCodeExpiry(null);
        employeeRepository.save(employee);
    }

    @Transactional
    public void initiatePasswordReset(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();

        if (!isCompanyEmail(email)) {
            throw new EmailDomainNotAllowedException(
                    "Sadece Stuttgart Taksi iş e-postası ile şifre sıfırlama talebinde bulunabilirsiniz");
        }

        employeeRepository.findByEmailIgnoreCase(email).ifPresent(employee -> {
            String token = UUID.randomUUID().toString();
            employee.setResetToken(token);
            employee.setResetTokenExpiry(LocalDateTime.now().plusMinutes(resetTokenValidityMinutes));
            employeeRepository.save(employee);

            String resetLink = appBaseUrl + "/reset-password?token=" + token;
            resendEmailService.sendPasswordResetEmail(employee.getEmail(), resetLink);
        });
        // E-posta bulunamasa bile burada sessizce çıkılır (kullanıcı varlığı ifşa edilmez).
    }

    @Transactional(readOnly = true)
    public Employee validateResetToken(String token) {
        Employee employee = employeeRepository.findByResetToken(token)
                .orElseThrow(() -> new InvalidResetTokenException("Bu link geçersiz veya süresi dolmuş"));

        if (employee.getResetTokenExpiry() == null || employee.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidResetTokenException("Bu link geçersiz veya süresi dolmuş");
        }

        return employee;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        Employee employee = validateResetToken(token);

        employee.setPasswordHash(passwordEncoder.encode(newPassword));
        employee.setResetToken(null);
        employee.setResetTokenExpiry(null);
        employeeRepository.save(employee);
    }
}
