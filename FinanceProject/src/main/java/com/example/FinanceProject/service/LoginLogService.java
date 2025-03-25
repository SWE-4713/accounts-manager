package com.example.FinanceProject.service;

import com.example.FinanceProject.entity.LoginLog;
import com.example.FinanceProject.entity.User;
import com.example.FinanceProject.repository.LoginLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class LoginLogService {

    @Autowired
    private LoginLogRepository loginLogRepository;
    
    /**
     * Records a login attempt to the database
     * 
     * @param user The user attempting to login (can be null for failed logins)
     * @param username The username used for login attempt
     * @param successful Whether the login attempt was successful
     */
    public void recordLogin(User user, String username, boolean successful) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
                
        String ipAddress = extractIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        LoginLog loginLog = new LoginLog(user, username, ipAddress, userAgent, successful);
        loginLogRepository.save(loginLog);
    }
    
    /**
     * Extracts the client IP address from the request, handling proxies
     */
    private String extractIpAddress(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("WL-Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }
        
        // If multiple IPs are provided (because of proxies), get the first one
        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }
        
        return clientIp;
    }
}