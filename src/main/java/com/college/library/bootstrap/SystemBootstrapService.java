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
    private final Environment environment;

    @Value("${app.bootstrap.initial-admin-password:}")
    private String initialAdminPassword;

    public SystemBootstrapService(TenantRepository tenantRepository,
                                  TenantSettingsRepository tenantSettingsRepository,
                                  UserRepository userRepository,
                                  BookRepository bookRepository,
                                  BookCopyService bookCopyService,
                                  PasswordEncoder passwordEncoder,
                                  Environment environment) {
        this.tenantRepository = tenantRepository;
        this.tenantSettingsRepository = tenantSettingsRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.bookCopyService = bookCopyService;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        bootstrapDefaultTenant();
        bootstrapAdminUser();
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

                log.info("=================================================================");
                log.info(" ONE-TIME INITIAL ADMIN BOOTSTRAP COMPLETED");
                log.info(" Username: admin@athenaeum.edu");
                log.info(" Password: {}", adminPassword);
                log.info(" Role:     SUPER_ADMIN");
                log.info(" PLEASE CHANGE THIS PASSWORD IMMEDIATELY AFTER INITIAL SIGN-IN!");
                log.info("=================================================================");
            }
        } finally {
            TenantContext.clear();
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
