package com.odc.common.constant;

public class PaymentConstant {
    // ========================================================================
    // DIRECTIONS (Money Flow Direction)
    // ========================================================================
    public static final String CREDIT = "CREDIT"; // Add money (+)
    public static final String DEBIT = "DEBIT";   // Deduct money (-)
    // ========================================================================
    // TRANSACTION TYPES (Business Logic)
    // ========================================================================
    // 1. Top Up (Client -> Wallet)
    public static final String DEPOSIT = "DEPOSIT";
    // 2. Milestone Payment (Client -> System Holding)
    public static final String MILESTONE_PAYMENT = "MILESTONE_PAYMENT";
    // 3. Disbursement (System Holding -> Team Wallet)
    // Used when System transfers money to Mentor/Talent wallet after milestone approval
    public static final String DISBURSEMENT = "DISBURSEMENT";
    // 4. Allocation (Team Wallet -> Member Wallet)
    // Used when Leader Mentor/Talent distributes money to team members
    public static final String ALLOCATION = "ALLOCATION";
    // 5. Withdrawal (Member/System Wallet -> Bank)
    public static final String WITHDRAWAL = "WITHDRAWAL";
    // 6. System Fee (Deducted from transaction)
    public static final String SYSTEM_FEE = "SYSTEM_FEE";
    // 7. Refund (System -> Client)
    public static final String REFUND = "REFUND";
    // ========================================================================
    // REFERENCE TYPES (Entity Linkage - refType)
    // ========================================================================
    public static final String PAYMENT_REQUEST = "PAYMENT_REQUEST";       // Link to payment_requests
    public static final String WITHDRAWAL_REQUEST = "WITHDRAWAL_REQUEST"; // Link to withdrawal_requests
    public static final String PROJECT = "PROJECT";                       // Link to projects
    public static final String MILESTONE = "MILESTONE";                   // Link to milestones
    // ========================================================================
    // SYSTEM CONFIGURATION KEYS
    // ========================================================================
    public static final String SYSTEM_CONFIG_FEE_DISTRIBUTION_NAME = "fee-distribution";
    public static final String SYSTEM_CONFIG_CRON_EXPRESSION_KEY = "cronExpression";
}
