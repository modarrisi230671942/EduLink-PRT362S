-- The activity log is a history record, so it must not be tied to live user rows.
--
-- With the foreign key, writing an entry about a user created or updated in a transaction that has
-- not committed yet (registration, password change) had to wait for that transaction, while the
-- transaction was waiting for the entry: both hung until MySQL's lock timeout. Without it, entries
-- also keep the actor's ID after the user is deleted (the email is stored alongside it).
ALTER TABLE audit_log DROP FOREIGN KEY audit_log_ibfk_1;
