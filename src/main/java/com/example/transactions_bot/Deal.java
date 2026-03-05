package com.example.transactions_bot;



import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Entity
@Table(name= "Transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Deal {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    // primary variable 
    private Long  id;
    private float amount;
    private float finalAmount;
    private float  feeAmount;

    //admin & user
    private Long adminId;
    private String adminName;
    private Long userId;
    private String userName;

    //Logistical variable
    private Long chatId;
    private  LocalDateTime  localDateTime;
    private String status;




}
