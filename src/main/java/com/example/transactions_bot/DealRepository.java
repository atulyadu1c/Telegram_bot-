package com.example.transactions_bot;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DealRepository extends JpaRepository<Deal, Long>{
    
    // Total stats ke liye
    long countByStatus(String status);
    
    @Query("SELECT SUM(d.amount) FROM Deal d")
    Double getTotalEscrowedAmount();

    @Query("SELECT SUM(d.feeAmount) FROM Deal d")
    Double getTotalFees();

    // Today's stats
    @Query("SELECT COUNT(d) FROM Deal d WHERE d.localDateTime >= :startOfDay")
    long countTodayDeals(LocalDateTime startOfDay);

    @Query("SELECT SUM(d.amount) FROM Deal d WHERE d.localDateTime >= :startOfDay")
    Double getTodayAmount(LocalDateTime startOfDay);

    // Leaderboard logic
    @Query("SELECT d.adminName, COUNT(d) as dealCount FROM Deal d GROUP BY d.adminName ORDER BY dealCount DESC")
    List<Object[]> getAdminLeaderboard();
}
