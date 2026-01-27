package tech.safepay.Enums;

/**
 * Classificação do tipo de estabelecimento comercial (MCC simplificado).
 *
 * <p>Utilizado para análise de risco, regras de negócio
 * e modelos antifraude.</p>
 */
public enum MerchantCategory {

    GROCERY,
    RESTAURANT,
    FAST_FOOD,
    BAR_CAFE,

    CLOTHING,
    ELECTRONICS,
    HOME_APPLIANCES,
    DEPARTMENT_STORE,
    FURNITURE,

    SUBSCRIPTION_SERVICE,
    TELECOM,
    UTILITIES,
    INSURANCE,

    BANKING,
    INVESTMENT,
    LOAN_CREDIT,
    CRYPTO_EXCHANGE,

    HEALTHCARE,
    PHARMACY,
    FITNESS,
    WELLNESS,

    TRANSPORTATION,
    RIDE_SHARING,
    AIRLINE,
    HOTEL,
    TRAVEL_AGENCY,
    CAR_RENTAL,

    ENTERTAINMENT,
    GAMING,
    STREAMING,
    SPORTS_EVENTS,

    EDUCATION,
    ONLINE_COURSES,
    BOOKS_MEDIA,

    ECOMMERCE,
    DIGITAL_GOODS,
    SOFTWARE_SERVICE,

    GAMBLING,
    ADULT_CONTENT,
    MONEY_TRANSFER,
    CHARITY_DONATION,

    /** Categoria não identificada. */
    UNKNOWN
}
