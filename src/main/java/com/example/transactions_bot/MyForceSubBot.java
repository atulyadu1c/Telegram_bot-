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
        long userId = update.getMessage().getFrom().getId();

        if (messageText.startsWith("/add")) {
            if (isAdmin(chatId, userId)) {
                handleTransaction(update, chatId, messageText);
            } else {
                sendSimpleMessage(chatId, "❌ <b>Access Denied:</b> Sirf Admins hi deals add kar sakte hain.");
            }
        } 
        else if (messageText.equalsIgnoreCase("/status")) {
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
        float baseAmount = 0;

        // 1. Amount Detection
        String[] parts = messageText.split("\\s+");
        if (parts.length >= 2) {
            baseAmount = Float.parseFloat(parts[1]);
        } else {
            String[] lines = dealMessage.split("\n");
            boolean found = false;
            for (String line : lines) {
                String upperLine = line.toUpperCase().trim();
                if (upperLine.contains("AMOUNT") || upperLine.contains("DEAL AMOUNT")) {
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)");
                    java.util.regex.Matcher m = p.matcher(line);
                    if (m.find()) {
                        baseAmount = Float.parseFloat(m.group(1));
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                sendSimpleMessage(chatId, "❌ <b>Error:</b> Amount nahi mila. Example: <code>/add 25</code>");
                return;
            }
        }

        // 2. Fees & Details logic
        float feePercentage = 1.0f;
        float feeAmount = (baseAmount * feePercentage) / 100.0f;
        float totalAmountToPay = (baseAmount - feeAmount);

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

        // 3. Response Text (Success Status)
        String responseText = "🏛️ <b>PAYMENT DETAILS (SMART-SCAN)</b>\n\n" +
                             "<b>DEAL INFO:</b> " + dealInfo + "\n" +
                             "<b>BUYER:</b> " + buyer + "\n" +
                             "<b>SELLER:</b> " + seller + "\n" +
                             "<b>DEAL AMOUNT:</b> ₹" + baseAmount + "\n" +
                             "<b>ESCROW FEES:</b> 1.0% (₹" + String.format("%.1f", feeAmount) + ")\n\n" +
                             "──────────────────\n" +
                             "👤 <b>SELLER:</b> " + seller + "\n" +
                             "💰 <b>Total Amount to Pay:</b> ₹" + String.format("%.1f", totalAmountToPay) + "\n" +
                             "🏗️ <b>Escrow Admin:</b> @" + update.getMessage().getFrom().getUserName() + "\n\n" +
                             "✅ <b>Status:</b> SUCCESS (Logged to Database)";

        // 4. Database Save directly as SUCCESS
        Deal deal = new Deal();
        deal.setAmount(baseAmount);
        deal.setFeeAmount(feeAmount);
        deal.setFinalAmount(totalAmountToPay);
        deal.setUserName(buyer);
        deal.setAdminName(update.getMessage().getFrom().getFirstName());
        deal.setChatId(chatId);
        deal.setLocalDateTime(LocalDateTime.now());
        deal.setStatus("SUCCESS"); 
        dealRepository.save(deal);
         
       
        // 5. Send Message
        sendSimpleMessage(chatId, responseText);

    } catch (Exception e) {
        e.printStackTrace();
        sendSimpleMessage(chatId, "❌ <b>Error:</b> Data process nahi ho paya.");
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
    
    long totalSuccess = dealRepository.countByStatus("SUCCESS");
    Double totalAmount = dealRepository.getTotalEscrowedAmount();
    Double totalFees = dealRepository.getTotalFees();
    
    long todayDeals = dealRepository.countTodayDeals(startOfDay);
    Double todayAmount = dealRepository.getTodayAmount(startOfDay);

    String statsMessage = "📊 <b>ESCROW DASHBOARD</b>\n" +
            "──────────────────\n" +
            "✅ <b>Total Success Deals:</b> " + totalSuccess + "\n" +
            "💰 <b>Total Volume:</b> ₹" + String.format("%.2f", totalAmount != null ? totalAmount : 0) + "\n" +
            "💵 <b>Total Profit:</b> ₹" + String.format("%.2f", totalFees != null ? totalFees : 0) + "\n" +
            "──────────────────\n" +
            "📅 <b>TODAY'S WORK</b>\n" +
            "✅ <b>Deals:</b> " + todayDeals + "\n" +
            "💰 <b>Amount:</b> ₹" + String.format("%.2f", todayAmount != null ? todayAmount : 0) + "\n" +
            "──────────────────\n" +
            "🚀 @TRADEGO_MARKET";

    sendSimpleMessage(chatId, statsMessage);
}

    }
