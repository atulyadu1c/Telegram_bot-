package com.example.transactions_bot;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class MyForceSubBot extends TelegramLongPollingBot {

    @Autowired
    private DealRepository dealRepository;
    
    @Override
    public String getBotUsername() { return "Atul_Transactions_bot"; }

    @Override
    public String getBotToken() { return "8750243799:AAGFqPj-cr22UEOsLYXpoHIk5pg8kWRbDbE"; }

   @Override
public void onUpdateReceived(Update update) {
    if (update.hasMessage() && update.getMessage().hasText()) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId(); // Bhejne wale ki ID

        if (messageText.startsWith("/add")) {
            // CHECK: Kya user admin hai?
            if (isAdmin(chatId, userId)) {
                handleTransaction(update, chatId, messageText);
            } else {
                sendSimpleMessage(chatId, "❌ <b>Access Denied:</b> Sirf Admins hi deals add kar sakte hain.");
            }
        }

        if (messageText.startsWith("/done")) {
            
                sendSimpleMessage(chatId, "✅  Deal Completed And Logged Successfully");
            
        }
    }
}

   private void handleTransaction(Update update, long chatId, String messageText) {
    try {
        if (update.getMessage().getReplyToMessage() == null) {
            sendSimpleMessage(chatId, "⚠️ <b>Error:</b> Pehle deal message ko <b>Reply</b> karein!");
            return;
        }

        String dealMessage = update.getMessage().getReplyToMessage().getText();
        
        // Amount parsing (/add 120)
        String[] parts = messageText.split(" ");
        if (parts.length < 2) {
            sendSimpleMessage(chatId, "⚠️ <b>Format:</b> <code>/add 100</code>");
            return;
        }
        
        float baseAmount = Float.parseFloat(parts[1]);
        float feePercentage = (baseAmount <= 200) ? 2.0f : 3.0f;
        float feeAmount = (baseAmount * feePercentage) / 100.0f;
        float Total_Amount = (baseAmount - feeAmount);

        String buyer = "Not Found";
        String seller = "Not Found";
        String dealInfo = "Not Found";

        String[] lines = dealMessage.split("\n");
        for (String line : lines) {
            String upperLine = line.toUpperCase().trim();
            
            // 1. DEAL INFO extraction (Priority check)
            if (upperLine.startsWith("DEAL INFO:") || upperLine.startsWith("DEAL:")) {
                dealInfo = line.split(":", 2)[1].trim();
            } 
            // 2. Buyer extraction
            else if (upperLine.contains("BUYER")) {
                buyer = extractUsername(line);
            } 
            // 3. Seller extraction
            else if (upperLine.contains("SELLER")) {
                seller = extractUsername(line);
            }
        }

        // Database Save
        Deal deal = new Deal();
        deal.setAmount(baseAmount);
        deal.setFeeAmount(feeAmount);
        deal.setFinalAmount(Total_Amount);
        deal.setUserName(buyer); 
        deal.setAdminName(update.getMessage().getFrom().getFirstName());
        deal.setChatId(chatId);
        deal.setLocalDateTime(LocalDateTime.now());
        deal.setStatus("SUCCESS");
        dealRepository.save(deal);

        // Professional Format Output
        String responseText = "🏛️ <b>PAYMENT DETAILS</b>\n\n" +
                             "<b>DEAL INFO:</b> " + dealInfo + "\n" +
                             "<b>BUYER:</b> " + buyer + "\n" +
                             "<b>SELLER:</b> " + seller + "\n" +
                             "<b>DEAL AMOUNT:</b> ₹" + baseAmount + "\n" +
                             "<b>ESCROW FEES:</b> " + feePercentage + "% (₹" + String.format("%.2f", feeAmount) + ")\n\n" +
                             "──────────────────\n" +
                             "👤 <b>SELLER:</b> " + seller + "\n" +
                             "💰 <b>Total Amount to Pay:</b> ₹" + String.format("%.2f", Total_Amount) + "\n" +
                             "🏗️ <b>Escrow Admin:</b> @" + update.getMessage().getFrom().getUserName() + "\n\n";

        sendSimpleMessage(chatId, responseText);

    } catch (Exception e) {
        e.printStackTrace();
        sendSimpleMessage(chatId, "❌ <b>Error:</b> Data sahi se read nahi ho paya.");
    }
}

// Ye Helper Method hai jo line mein se @username nikalega
private String extractUsername(String line) {
    if (line.contains("@")) {
        String[] words = line.split(" ");
        for (String word : words) {
            if (word.startsWith("@")) return word;
        }
    }
    if (line.contains(":")) {
        // Colon ke baad ka poora text uthao aur extra spaces hatao
        String value = line.split(":", 2)[1].trim(); 
        return value.isEmpty() ? "Not Found" : value;
    }
    return "Not Found";
}

    private void sendSimpleMessage(long chatId, String text) {
        SendMessage sm = new SendMessage();
        sm.setChatId(String.valueOf(chatId));
        sm.setText(text);
        sm.setParseMode("HTML"); 
        try {
            execute(sm); 
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private boolean isAdmin(long chatId, long userId) {
    try {
        org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember getChatMember = 
            new org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember();
        getChatMember.setChatId(String.valueOf(chatId));
        getChatMember.setUserId(userId);

        org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember member = execute(getChatMember);
        String status = member.getStatus();

        // Sirf 'creator' (owner) aur 'administrator' ko allow karein
        return status.equals("creator") || status.equals("administrator");
    } catch (Exception e) {
        return false; // Error aaye toh safe side ke liye deny kar dein
    }
}
}