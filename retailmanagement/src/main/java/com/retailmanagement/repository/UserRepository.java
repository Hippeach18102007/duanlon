package com.retailmanagement.repository;

import com.retailmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, Integer id);
    boolean existsByEmailAndIdNot(String email, Integer id);

    @Query("""
    SELECT u FROM User u
    JOIN FETCH u.role r
    JOIN FETCH r.permissions
    WHERE u.username = :username
    """)
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    // login bằng username hoặc email
    Optional<User> findByUsernameOrEmail(String username, String email);

    @Query(value = """
    SELECT email 
    FROM users 
    WHERE role_id = 1 
      AND is_active = 1 
      AND email IS NOT NULL
    """, nativeQuery = true)
    List<String> findAdminEmails();
}
