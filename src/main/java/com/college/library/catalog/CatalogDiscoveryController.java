package com.college.library.catalog;

import com.college.library.entity.Book;
import com.college.library.entity.User;
import com.college.library.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/catalog")
public class CatalogDiscoveryController {

    private final CatalogSearchService catalogSearchService;
    private final WishlistService wishlistService;
    private final BookRequestService bookRequestService;
    private final UserService userService;

    public CatalogDiscoveryController(CatalogSearchService catalogSearchService,
                                      WishlistService wishlistService,
                                      BookRequestService bookRequestService,
                                      UserService userService) {
        this.catalogSearchService = catalogSearchService;
        this.wishlistService = wishlistService;
        this.bookRequestService = bookRequestService;
        this.userService = userService;
    }

    @GetMapping
    public String browseCatalog(@RequestParam(required = false) String q,
                                @RequestParam(required = false) String category,
                                @RequestParam(required = false) Boolean availableOnly,
                                @RequestParam(required = false, defaultValue = "newest") String sortBy,
                                Model model) {
        CatalogSearchService.SearchResult result = catalogSearchService.search(q, category, availableOnly, sortBy);
        List<Book> newArrivals = catalogSearchService.getNewArrivals(6);

        model.addAttribute("books", result.getBooks());
        model.addAttribute("totalResults", result.getTotalResults());
        model.addAttribute("facets", result.getCategoryFacets());
        model.addAttribute("newArrivals", newArrivals);
        model.addAttribute("query", q != null ? q : "");
        model.addAttribute("selectedCategory", category != null ? category : "ALL");
        model.addAttribute("availableOnly", Boolean.TRUE.equals(availableOnly));
        model.addAttribute("sortBy", sortBy);

        return "catalog";
    }

    @GetMapping("/wishlist")
    public String viewWishlist(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Wishlist> items = wishlistService.getUserWishlist(user.getId());
        model.addAttribute("wishlistItems", items);
        return "wishlist";
    }

    @PostMapping("/wishlist/add")
    public String addToWishlist(@RequestParam Long bookId,
                                @RequestParam(defaultValue = "true") boolean notify,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        wishlistService.addToWishlist(user, bookId, notify);
        redirectAttributes.addFlashAttribute("successMessage", "Book added to your wishlist!");
        return "redirect:/catalog";
    }

    @PostMapping("/wishlist/{bookId}/remove")
    public String removeFromWishlist(@PathVariable Long bookId,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        wishlistService.removeFromWishlist(user.getId(), bookId);
        redirectAttributes.addFlashAttribute("successMessage", "Removed from wishlist");
        return "redirect:/catalog/wishlist";
    }

    @GetMapping("/purchase-requests")
    public String purchaseRequests(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        boolean isStaffOrAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                               a.getAuthority().equals("ROLE_LIBRARIAN") ||
                               a.getAuthority().equals("ROLE_LIBRARY_ADMIN") ||
                               a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if (isStaffOrAdmin) {
            model.addAttribute("requests", bookRequestService.getAllRequests());
            model.addAttribute("isStaff", true);
        } else {
            model.addAttribute("requests", bookRequestService.getUserRequests(user.getId()));
            model.addAttribute("isStaff", false);
        }
        model.addAttribute("newRequest", new BookRequest());
        return "purchase-requests";
    }

    @PostMapping("/purchase-requests/submit")
    public String submitPurchaseRequest(BookRequest request,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        bookRequestService.submitRequest(user, request.getTitle(), request.getAuthor(),
                request.getIsbn(), request.getPublisher(), request.getReason());
        redirectAttributes.addFlashAttribute("successMessage", "Book acquisition request submitted for library review!");
        return "redirect:/catalog/purchase-requests";
    }

    @PostMapping("/purchase-requests/{id}/review")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN', 'LIBRARY_ADMIN', 'SUPER_ADMIN')")
    public String reviewPurchaseRequest(@PathVariable Long id,
                                        @RequestParam BookRequestStatus status,
                                        @RequestParam(required = false) String adminNotes,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        RedirectAttributes redirectAttributes) {
        User reviewer = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        bookRequestService.reviewRequest(id, reviewer, status, adminNotes);
        redirectAttributes.addFlashAttribute("successMessage", "Purchase request marked as " + status);
        return "redirect:/catalog/purchase-requests";
    }
}
