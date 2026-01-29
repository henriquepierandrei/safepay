package tech.safepay.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AdminJwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public AdminJwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();


        // Rotas públicas de autenticação (não precisam de token)
        boolean isLoginRoute = path.equals("/admin/auth/login") || path.equals("/api/v1/admin/auth/login");
        boolean isRefreshRoute = path.equals("/admin/auth/refresh") || path.equals("/api/v1/admin/auth/refresh");


        if (isLoginRoute || isRefreshRoute) {
            return true;
        }

        // Se NÃO contém "/admin/" no path, é rota pública
        boolean containsAdmin = path.contains("/admin/");

        boolean shouldNotFilter = !containsAdmin;

        return shouldNotFilter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {


        String header = request.getHeader("Authorization");

        if (header != null) {
            System.out.println("Header Authorization: " + header.substring(0, Math.min(30, header.length())) + "...");
        } else {
            System.out.println("Header Authorization: NULL");
        }

        if (header == null || !header.startsWith("Bearer ")) {
            sendUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        String token = header.substring(7);

        try {
            Claims claims = jwtService.parse(token);

            String tokenType = (String) claims.get("type");

            if (!"ADMIN".equals(tokenType)) {
                sendForbidden(response, "Invalid token type");
                return;
            }

            request.setAttribute("adminId", claims.getSubject());
            request.setAttribute("adminEmail", claims.get("email"));

        } catch (Exception e) {
            e.printStackTrace();
            sendUnauthorized(response, "Invalid or expired token: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}