package com.college.library.bootstrap;

import com.college.library.entity.*;
import com.college.library.repository.BookRepository;
import com.college.library.repository.UserRepository;
import com.college.library.service.BookCopyService;
import com.college.library.tenant.Tenant;
import com.college.library.tenant.TenantContext;
import com.college.library.tenant.TenantRepository;
import com.college.library.tenant.TenantSettings;
import com.college.library.tenant.TenantSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.library.policy.MembershipType;
import com.college.library.policy.MembershipTypeRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Arrays;

@Service
public class SystemBootstrapService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SystemBootstrapService.class);

    private final TenantRepository tenantRepository;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookCopyService bookCopyService;
    private final PasswordEncoder passwordEncoder;
    private final MembershipTypeRepository membershipTypeRepository;
    private final Environment environment;

    @Value("${app.bootstrap.initial-admin-password:}")
    private String initialAdminPassword;

    public SystemBootstrapService(TenantRepository tenantRepository,
                                  TenantSettingsRepository tenantSettingsRepository,
                                  UserRepository userRepository,
                                  BookRepository bookRepository,
                                  BookCopyService bookCopyService,
                                  PasswordEncoder passwordEncoder,
                                  MembershipTypeRepository membershipTypeRepository,
                                  Environment environment) {
        this.tenantRepository = tenantRepository;
        this.tenantSettingsRepository = tenantSettingsRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.bookCopyService = bookCopyService;
        this.passwordEncoder = passwordEncoder;
        this.membershipTypeRepository = membershipTypeRepository;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        bootstrapDefaultTenant();
        bootstrapAdminUser();
        bootstrapPolicies();
        bootstrapDevSampleCatalog();
    }

    private void bootstrapDefaultTenant() {
        if (!tenantRepository.existsById(TenantContext.DEFAULT_TENANT_ID)) {
            Tenant defaultTenant = new Tenant(
                    TenantContext.DEFAULT_TENANT_ID,
                    "Athenaeum University Library",
                    "default",
                    "admin@libraryplatform.edu"
            );
            tenantRepository.save(defaultTenant);

            TenantSettings settings = new TenantSettings(TenantContext.DEFAULT_TENANT_ID, "Athenaeum University Library");
            tenantSettingsRepository.save(settings);
            log.info("Initialized default library tenant: {}", defaultTenant.getName());
        }
    }

    private void bootstrapAdminUser() {
        TenantContext.setTenantId(TenantContext.DEFAULT_TENANT_ID);
        try {
            if (userRepository.countByTenantId(TenantContext.DEFAULT_TENANT_ID) == 0) {
                boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
                String adminPassword;

                if (initialAdminPassword != null && !initialAdminPassword.isBlank()) {
                    adminPassword = initialAdminPassword;
                } else if (!isProd) {
                    // Safe local developer credential
                    adminPassword = "AdminDev@2026!";
                } else {
                    // Production: generate a cryptographically strong single-use password
                    adminPassword = generateSecurePassword(16);
                }

                User superAdmin = new User();
                superAdmin.setTenantId(TenantContext.DEFAULT_TENANT_ID);
                superAdmin.setName("System Administrator");
                superAdmin.setEmail("admin@athenaeum.edu");
                superAdmin.setPassword(passwordEncoder.encode(adminPassword));
                superAdmin.setRole(Role.SUPER_ADMIN);
                superAdmin.setEmailVerified(true);
                userRepository.save(superAdmin);

                if (!isProd) {
                    if (!userRepository.existsByTenantIdAndEmail(TenantContext.DEFAULT_TENANT_ID, "librarian@athenaeum.edu")) {
                        User librarian = new User();
                        librarian.setTenantId(TenantContext.DEFAULT_TENANT_ID);
                        librarian.setName("Head Librarian");
                        librarian.setEmail("librarian@athenaeum.edu");
                        librarian.setPassword(passwordEncoder.encode("LibrarianDev@2026!"));
                        librarian.setRole(Role.LIBRARIAN);
                        librarian.setEmailVerified(true);
                        userRepository.save(librarian);
                    }

                    if (!userRepository.existsByTenantIdAndEmail(TenantContext.DEFAULT_TENANT_ID, "student@athenaeum.edu")) {
                        User student = new User();
                        student.setTenantId(TenantContext.DEFAULT_TENANT_ID);
                        student.setName("Alex Student");
                        student.setEmail("student@athenaeum.edu");
                        student.setPassword(passwordEncoder.encode("StudentDev@2026!"));
                        student.setRole(Role.STUDENT);
                        student.setEmailVerified(true);
                        userRepository.save(student);
                    }
                }

                log.info("=================================================================");
                log.info(" ONE-TIME INITIAL ADMIN BOOTSTRAP COMPLETED");
                log.info(" Username: admin@athenaeum.edu");
                log.info(" Password: {}", adminPassword);
                log.info(" Role:     SUPER_ADMIN");
                if (!isProd) {
                    log.info(" Dev Librarian: librarian@athenaeum.edu / LibrarianDev@2026!");
                    log.info(" Dev Student:   student@athenaeum.edu   / StudentDev@2026!");
                }
                log.info(" PLEASE CHANGE THIS PASSWORD IMMEDIATELY AFTER INITIAL SIGN-IN!");
                log.info("=================================================================");
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void bootstrapPolicies() {
        TenantContext.setTenantId(TenantContext.DEFAULT_TENANT_ID);
        try {
            seedPolicyIfNotExists("UG", "Undergraduate Student", 4, 14, 2, new BigDecimal("5.00"));
            seedPolicyIfNotExists("PG", "Postgraduate Student", 6, 21, 3, new BigDecimal("5.00"));
            seedPolicyIfNotExists("FACULTY", "Faculty Member", 10, 60, 5, new BigDecimal("2.00"));
            seedPolicyIfNotExists("STUDENT", "General Student", 4, 14, 2, new BigDecimal("5.00"));
            seedPolicyIfNotExists("MEMBER", "General Member", 3, 14, 2, new BigDecimal("5.00"));
        } finally {
            TenantContext.clear();
        }
    }

    private void seedPolicyIfNotExists(String code, String displayName, int maxBooks, int duration, int renewals, BigDecimal fineRate) {
        if (!membershipTypeRepository.existsByTenantIdAndCode(TenantContext.DEFAULT_TENANT_ID, code)) {
            MembershipType p = new MembershipType();
            p.setTenantId(TenantContext.DEFAULT_TENANT_ID);
            p.setCode(code);
            p.setDisplayName(displayName);
            p.setMaxBooksAllowed(maxBooks);
            p.setLoanDurationDays(duration);
            p.setRenewalLimit(renewals);
            p.setFineRatePerDay(fineRate);
            p.setActive(true);
            membershipTypeRepository.save(p);
        }
    }

    private void bootstrapDevSampleCatalog() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (isProd) {
            return; // Production strictly starts with empty catalog
        }

        TenantContext.setTenantId(TenantContext.DEFAULT_TENANT_ID);
        try {
            if (bookRepository.countByTenantId(TenantContext.DEFAULT_TENANT_ID) == 0) {
                seedBook("Introduction to Algorithms", "Thomas H. Cormen, Charles E. Leiserson, Ronald L. Rivest, Clifford Stein",
                        "9780262033848", "Computer Science", "MIT Press", 2009, 4);
                seedBook("Clean Code: A Handbook of Agile Software Craftsmanship", "Robert C. Martin",
                        "9780132350884", "Computer Science", "Prentice Hall", 2008, 3);
                seedBook("Database System Concepts", "Abraham Silberschatz, Henry F. Korth, S. Sudarshan",
                        "9780078022159", "Computer Science", "McGraw-Hill", 2019, 2);
                seedBook("Physics for Scientists and Engineers", "Raymond A. Serway, John W. Jewett",
                        "9781133947271", "Physics", "Cengage Learning", 2013, 3);
                seedBook("Organic Chemistry", "Paula Yurkanis Bruice",
                        "9780134042282", "Chemistry", "Pearson", 2016, 2);
                seedBook("Principles of Economics", "N. Gregory Mankiw",
                        "9781305585126", "Economics", "Cengage Learning", 2017, 3);
                seedBook("The Elements of Style", "William Strunk Jr., E.B. White",
                        "9780205309023", "Literature", "Pearson", 1999, 5);
                seedBook("Linear Algebra and Its Applications", "David C. Lay, Steven R. Lay, Judi J. McDonald",
                        "9780321982384", "Mathematics", "Pearson", 2015, 4);
                log.info("Sample catalog seeded with copy-level barcodes for development.");
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void seedBook(String title, String author, String isbn, String category,
                          String publisher, int year, int copies) {
        Book b = new Book();
        b.setTenantId(TenantContext.DEFAULT_TENANT_ID);
        b.setTitle(title);
        b.setAuthor(author);
        b.setIsbn(isbn);
        b.setCategory(category);
        b.setPublisher(publisher);
        b.setPublicationYear(year);
        b.setAddedOn(LocalDate.now());
        Book saved = bookRepository.save(b);

        bookCopyService.createCopies(saved, copies, "Rack A" + (copies % 4 + 1), CopyCondition.GOOD);
    }

    private String generateSecurePassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
