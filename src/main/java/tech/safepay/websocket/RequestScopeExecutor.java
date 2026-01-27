package tech.safepay.websocket;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.function.Supplier;

/**
 * Utilitário responsável por simular um contexto de requisição HTTP
 * para execução de código que depende de beans anotados com {@code @RequestScope}.
 *
 * <p>Este executor é especialmente útil em cenários onde a execução ocorre
 * fora do ciclo de vida tradicional de uma requisição HTTP, como:</p>
 * <ul>
 *   <li>Processamentos assíncronos</li>
 *   <li>Execuções via WebSocket</li>
 *   <li>Schedulers e jobs em background</li>
 *   <li>Testes automatizados e pipelines internos</li>
 * </ul>
 *
 * <p>A classe cria programaticamente um {@link MockHttpServletRequest}
 * e o associa ao {@link RequestContextHolder}, garantindo que dependências
 * com escopo de requisição funcionem de forma previsível e segura.</p>
 *
 * <p><strong>Observação:</strong> o contexto é sempre limpo ao final da execução,
 * prevenindo vazamento de estado entre threads.</p>
 *
 * <p>Classe utilitária — não deve ser instanciada.</p>
 */
public final class RequestScopeExecutor {

    private RequestScopeExecutor() {}

    /**
     * Executa uma ação que retorna valor dentro de um contexto
     * simulado de {@code @RequestScope}.
     *
     * <p>Ideal para chamadas que dependem de informações normalmente
     * providas pelo contexto HTTP, mesmo quando não há uma requisição real.</p>
     *
     * @param action função a ser executada dentro do escopo de requisição
     * @param <T> tipo do valor retornado
     * @return resultado da execução da ação fornecida
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
     * Executa uma ação sem retorno dentro de um contexto
     * simulado de {@code @RequestScope}.
     *
     * <p>Usado quando o objetivo é apenas garantir a presença
     * do contexto de requisição durante a execução.</p>
     *
     * @param action ação a ser executada
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
