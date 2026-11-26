UPDATE kyc_verifications
SET status = 'PENDING',
    resubmission_allowed = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'REJECTED'
  AND verified_by IS NULL
  AND verified_at IS NULL
  AND resubmission_allowed = TRUE
  AND confidence_score = 0
  AND failure_reason IN (
      'The national identity card number is not a valid format',
      'The passport number is not a valid format',
      'The driving licence number is not a valid format',
      'The policyholder must be at least 18 years old'
  );
