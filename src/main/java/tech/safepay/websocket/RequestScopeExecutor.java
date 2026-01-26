package tech.safepay.websocket;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.function.Supplier;

public final class RequestScopeExecutor {

    private RequestScopeExecutor() {}

    /**
     * Executa código que precisa de @RequestScope fora de um contexto HTTP real.
     */
    public static <T> T executeInRequestScope(Supplier<T> action) {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        try {
            return action.get();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    /**
     * Versão para Runnable (sem retorno)
     */
    public static void executeInRequestScope(Runnable action) {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        try {
            action.run();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}