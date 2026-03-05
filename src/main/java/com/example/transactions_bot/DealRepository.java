package com.example.transactions_bot;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DealRepository extends JpaRepository<Deal, Long>{
    
}
