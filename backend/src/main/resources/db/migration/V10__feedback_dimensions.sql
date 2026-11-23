ALTER TABLE garage_feedback
    ADD COLUMN customer_service_rating NUMERIC(3, 2),
    ADD COLUMN repair_quality_rating NUMERIC(3, 2);

ALTER TABLE garage_feedback
    ADD CONSTRAINT ck_garage_feedback_customer_service
        CHECK (customer_service_rating IS NULL OR customer_service_rating BETWEEN 1 AND 5),
    ADD CONSTRAINT ck_garage_feedback_repair_quality
        CHECK (repair_quality_rating IS NULL OR repair_quality_rating BETWEEN 1 AND 5),
    ADD CONSTRAINT ck_garage_feedback_new_answers
        CHECK (
            (customer_service_rating IS NULL AND repair_quality_rating IS NULL)
            OR (
                customer_service_rating IS NOT NULL
                AND repair_quality_rating IS NOT NULL
                AND recommend_again IS NOT NULL
            )
        );
