package za.ac.mycput.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import za.ac.mycput.domain.AuditLog;
import za.ac.mycput.domain.enums.AuditAction;
import za.ac.mycput.repository.AuditLogRepository;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Auditing is best-effort: a failure to write the entry must never fail the user's action. */
class AuditServiceImplTest {

    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final AuditServiceImpl audit = new AuditServiceImpl(repository, transactions);

    @Test
    @DisplayName("A database error while saving the entry is logged, not thrown")
    void saveFailureIsSwallowed() {
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(repository.save(any(AuditLog.class))).thenThrow(new PessimisticLockingFailureException("Lock wait timeout exceeded"));

        assertThatCode(() -> audit.record(1, "a@test.com", AuditAction.USER_REGISTERED, "STUDENT", 1, null))
                .doesNotThrowAnyException();
        verify(transactions).rollback(any());
    }

    @Test
    @DisplayName("A failure while committing the entry's own transaction is logged, not thrown")
    void commitFailureIsSwallowed() {
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        doThrow(new UnexpectedRollbackException("Transaction silently rolled back")).when(transactions).commit(any());

        assertThatCode(() -> audit.record(1, "a@test.com", AuditAction.PASSWORD_CHANGED, "USER", 1, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("The entry is written in a new transaction, separate from the caller's")
    void usesNewTransaction() {
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        audit.record(null, "unknown@test.com", AuditAction.LOGIN_FAILED, "USER", null, "Wrong password");

        verify(transactions).getTransaction(argThat(definition ->
                definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verify(repository).save(any(AuditLog.class));
        verify(transactions).commit(any());
    }
}
