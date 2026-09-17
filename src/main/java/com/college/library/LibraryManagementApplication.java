package com.college.library;

import com.college.library.entity.*;
import com.college.library.repository.BookRepository;
import com.college.library.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@SpringBootApplication
public class LibraryManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryManagementApplication.class, args);
    }

    /**
     * Seeds the database with a demo admin, librarian, student and a small
     * book catalog on first run, so the system is immediately explorable.
     */
    @Bean
    CommandLineRunner seedData(UserRepository userRepo, BookRepository bookRepo, PasswordEncoder encoder) {
        return args -> {
            if (userRepo.count() == 0) {
                User admin = new User();
                admin.setName("Library Admin");
                admin.setEmail("admin@college.edu");
                admin.setPassword(encoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                admin.setPhone("9999999999");
                userRepo.save(admin);

                User librarian = new User();
                librarian.setName("Librarian Rao");
                librarian.setEmail("librarian@college.edu");
                librarian.setPassword(encoder.encode("lib123"));
                librarian.setRole(Role.LIBRARIAN);
                librarian.setPhone("9888888888");
                userRepo.save(librarian);

                User student = new User();
                student.setName("Asha Verma");
                student.setEmail("student@college.edu");
                student.setPassword(encoder.encode("student123"));
                student.setRole(Role.STUDENT);
                student.setPhone("9777777777");
                userRepo.save(student);

                System.out.println("=========================================================");
                System.out.println(" Demo accounts created:");
                System.out.println("   Admin:     admin@college.edu     / admin123");
                System.out.println("   Librarian: librarian@college.edu / lib123");
                System.out.println("   Student:   student@college.edu   / student123");
                System.out.println("=========================================================");
            }

            if (bookRepo.count() == 0) {
                bookRepo.save(book("Introduction to Algorithms", "Cormen, Leiserson, Rivest, Stein",
                        "9780262033848", "Computer Science", "MIT Press", 4));
                bookRepo.save(book("Clean Code", "Robert C. Martin",
                        "9780132350884", "Computer Science", "Prentice Hall", 3));
                bookRepo.save(book("Database System Concepts", "Silberschatz, Korth, Sudarshan",
                        "9780078022159", "Computer Science", "McGraw-Hill", 2));
                bookRepo.save(book("Physics for Scientists and Engineers", "Serway, Jewett",
                        "9781133947271", "Physics", "Cengage", 3));
                bookRepo.save(book("Organic Chemistry", "Paula Yurkanis Bruice",
                        "9780134042282", "Chemistry", "Pearson", 2));
                bookRepo.save(book("Principles of Economics", "N. Gregory Mankiw",
                        "9781305585126", "Economics", "Cengage", 3));
                bookRepo.save(book("The Elements of Style", "Strunk & White",
                        "9780205309023", "Literature", "Pearson", 5));
                bookRepo.save(book("Linear Algebra and Its Applications", "David C. Lay",
                        "9780321982384", "Mathematics", "Pearson", 4));
            }
        };
    }

    private Book book(String title, String author, String isbn, String category,
                       String publisher, int copies) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        b.setIsbn(isbn);
        b.setCategory(category);
        b.setPublisher(publisher);
        b.setTotalCopies(copies);
        b.setAvailableCopies(copies);
        b.setAddedOn(LocalDate.now());
        return b;
    }
}
