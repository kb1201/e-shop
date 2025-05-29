package hr.fer.dipl.db.repository;

import java.util.Collection;
import java.util.List;

import hr.fer.dipl.db.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Example of a query method that returns a page of products based on the query string
    Page<Product> findByNameContainingIgnoreCase(String query, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.rating IS NOT NULL ORDER BY p.rating DESC")
    Page<Product> findMostPopularByRating(Pageable pageable);

    List<Product> findByIdIn(Collection<Long> ids);
    // List<Product> findAllById(List<Integer> ids);
}
