CREATE TABLE STUDENT_PROFILE_SAGA
(
    SAGA_ID                  UUID                                NOT NULL,
    SAGA_NAME                VARCHAR(50)                         NOT NULL,
    SAGA_STATE               VARCHAR(100)                        NOT NULL,
    PAYLOAD                  VARCHAR(4000)                       NOT NULL,
    STATUS                   VARCHAR(20)                         NOT NULL,
    SAGA_COMPENSATED         BOOLEAN                             NOT NULL,
    CREATE_USER              VARCHAR(32)                         NOT NULL,
    CREATE_DATE              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER              VARCHAR(32)                         NOT NULL,
    UPDATE_DATE              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT STUDENT_PROFILE_SAGA_PK PRIMARY KEY (SAGA_ID)
);
CREATE INDEX STUDENT_PROFILE_SAGA_STATUS_IDX ON STUDENT_PROFILE_SAGA (STATUS);

CREATE TABLE STUDENT_PROFILE_SAGA_EVENT_STATES
(
    SAGA_EVENT_ID       UUID                                NOT NULL,
    SAGA_ID             UUID                                NOT NULL,
    SAGA_EVENT_STATE    VARCHAR(100)                        NOT NULL,
    SAGA_EVENT_OUTCOME  VARCHAR(100)                        NOT NULL,
    SAGA_STEP_NUMBER    INTEGER                             NOT NULL,
    SAGA_EVENT_RESPONSE VARCHAR(4000)                       NOT NULL,
    CREATE_USER         VARCHAR(32)                         NOT NULL,
    CREATE_DATE         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER         VARCHAR(32)                         NOT NULL,
    UPDATE_DATE         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT STUDENT_PROFILE_SAGA_EVENT_STATES_PK PRIMARY KEY (SAGA_EVENT_ID)
);
ALTER TABLE STUDENT_PROFILE_SAGA_EVENT_STATES
    ADD CONSTRAINT STUDENT_PROFILE_SAGA_EVENT_STATES_SAGA_ID_FK FOREIGN KEY (SAGA_ID) REFERENCES STUDENT_PROFILE_SAGA (SAGA_ID);

CREATE TABLE STUDENT_PROFILE_SAGA_SHEDLOCK
(
    NAME       VARCHAR(64),
    LOCK_UNTIL TIMESTAMP(3) NULL,
    LOCKED_AT  TIMESTAMP(3) NULL,
    LOCKED_BY  VARCHAR(255),
    PRIMARY KEY (NAME)
);
COMMENT ON TABLE STUDENT_PROFILE_SAGA_SHEDLOCK IS 'This table is used to achieve distributed lock between pods, for schedulers.';

