package com.example.transactions_bot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    public String getBotUsername() { return "TRADEGO_INRBOT"; }

    @Override
    public String getBotToken() { return "8703836614:AAG-Z1rqKSAT38RQYbNaqWAJfRhVfdV-BZo"; }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            long userId = update.getMessage().getFrom().getId();

            if (messageText.startsWith("/add")) {
                if (isAdmin(chatId, userId)) {
                    handleTransaction(update, chatId, messageText);
                } else {
                    sendSimpleMessage(chatId, "❌ <b>Access Denied:</b> Sirf Admins hi deals add kar sakte hain.");
                }
            }

            if (messageText.startsWith("/done")) {
                if (isAdmin(chatId, userId)) {
                    // Yahan aap apna deal complete karne ka logic likh sakte hain
                    sendSimpleMessage(chatId, "✅ <b>Deal Completed:</b> Transaction has been logged successfully by Admin.");
                } else {
                    sendSimpleMessage(chatId, "❌ <b>Access Denied:</b> Sirf Admins hi deals complete kar sakte hain.");
                }
            }

             if (messageText.equalsIgnoreCase("/status")) {
            showAdvancedStats(chatId);
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
            String[] parts = messageText.split(" ");
            if (parts.length < 2) {
                sendSimpleMessage(chatId, "⚠️ <b>Format:</b> <code>/add 100</code>");
                return;
            }

            
            
            // --- FEES CHANGE START ---
            float baseAmount = Float.parseFloat(parts[1]);
            float feePercentage = 1.0f; // Set to exactly 1%
            float feeAmount = (baseAmount * feePercentage) / 100.0f;
            float Total_Amount = (baseAmount - feeAmount);
            // --- FEES CHANGE END ---

            String buyer = "Not Found";
            String seller = "Not Found";
            String dealInfo = "Not Found";

            String[] lines = dealMessage.split("\n");
            for (String line : lines) {
                String upperLine = line.toUpperCase().trim();
                if (upperLine.startsWith("DEAL INFO:") || upperLine.startsWith("DEAL:")) {
                    dealInfo = line.split(":", 2)[1].trim();
                } else if (upperLine.contains("BUYER")) {
                    buyer = extractUsername(line);
                } else if (upperLine.contains("SELLER")) {
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

            // Output Format
            String responseText = "🏛️ <b>PAYMENT DETAILS</b>\n\n" +
                                 "<b>DEAL INFO:</b> " + dealInfo + "\n" +
                                 "<b>BUYER:</b> " + buyer + "\n" +
                                 "<b>SELLER:</b> " + seller + "\n" +
                                 "<b>DEAL AMOUNT:</b> ₹" + baseAmount + "\n" +
                                 "<b>ESCROW FEES:</b> " + feePercentage + "% (₹" + String.format("%.1f", feeAmount) + ")\n\n" +
                                 "──────────────────\n" +
                                 "👤 <b>SELLER:</b> " + seller + "\n" +
                                 "💰 <b>Total Amount to Pay:</b> ₹" + String.format("%.1f", Total_Amount) + "\n" +
                                 "🏗️ <b>Escrow Admin:</b> @" + update.getMessage().getFrom().getUserName() + "\n\n";

            sendSimpleMessage(chatId, responseText);

        } catch (Exception e) {
            e.printStackTrace();
            sendSimpleMessage(chatId, "❌ <b>Error:</b> Data sahi se read nahi ho paya.");
        }
    }

    private String extractUsername(String line) {
        if (line.contains("@")) {
            String[] words = line.split(" ");
            for (String word : words) {
                if (word.startsWith("@")) return word;
            }
        }
        if (line.contains(":")) {
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
            return status.equals("creator") || status.equals("administrator");
        } catch (Exception e) {
            return false;
        }
    }

        private void showAdvancedStats(long chatId) {
    LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
    // LocalDate totalDate = LocalDate.now();
    
    long totalDeals = dealRepository.count();
    long completedDeals = dealRepository.countByStatus("SUCCESS");
    Double totalAmount = dealRepository.getTotalEscrowedAmount();
    Double totalFees = dealRepository.getTotalFees();
    // LocalDate nowdate = totalDate;
    
    long todayDeals = dealRepository.countTodayDeals(startOfDay);
    Double todayAmount = dealRepository.getTodayAmount(startOfDay);
    if (todayAmount == null) todayAmount = 0.0;

    // Success Rate Calculation
    double successRate = (totalDeals > 0) ? ((double) completedDeals / totalDeals) * 100 : 0;

    // Leaderboard logic (Top Admin)
    List<Object[]> leaderboard = dealRepository.getAdminLeaderboard();
    String topAdmin = (leaderboard != null && !leaderboard.isEmpty()) ? (String) leaderboard.get(0)[0] : "N/A";

    String statsMessage = "📊 <b>ESCROW DASHBOARD (PRO)</b>\n" +
            "──────────────────\n" +
            "📈 <b>Success Rate:</b> " + String.format("%.1f", successRate) + "%\n" +
            "💼 <b>Total Deals:</b> " + totalDeals + "\n" +
            "💰 <b>Total Volume:</b> ₹" + String.format("%.2f", totalAmount != null ? totalAmount : 0) + "\n" +
            "💵 <b>Total Profit (1% Fees):</b> ₹" + String.format("%.2f", totalFees != null ? totalFees : 0) + "\n" +
            "──────────────────\n" +
            "📅 <b>TODAY'S STATS</b>\n" +
            "✅ <b>Deals:</b> " + todayDeals + "\n" +
            "💰 <b>Amount:</b> ₹" + String.format("%.2f", todayAmount) + "\n" +
            "──────────────────\n" +
            "🏆 <b>Top Admin:</b> " + topAdmin + "\n\n" +
            "📊 Data Recorded: " + LocalDate.now() +"\n"+
            "🚀 @TRADEGO_MARKET";

    sendSimpleMessage(chatId, statsMessage);
}
    }
