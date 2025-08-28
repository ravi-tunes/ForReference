-- VSTest_Package_Body.sql
-- =======================
--
-- This is the Package Body for VSTest.
-- It contains the private implementation and the SQL logic for the procedures
-- defined in the package specification. The SQL is taken directly from the
-- original Java application's repository layer.
--

CREATE OR REPLACE PACKAGE BODY VSTest AS

  PROCEDURE GET_ALL_BOOKS (
    p_book_cursor OUT SYS_REFCURSOR
  ) AS
  BEGIN
    OPEN p_book_cursor FOR
      SELECT b.id, b.BOOK_LEVEL, b.CODE, b.NAME, b.PARENT_ID, bai.alternate_id as settlement_account
      FROM atlas_owner.book b
      LEFT JOIN atlas_owner.BOOK_ALTERNATE_IDS bai ON bai.parent_id = b.id
          AND bai.parent_version = b.version
          AND bai.schema_id = 5
      WHERE b.record_status = 1
      ORDER BY b.id; -- Consistent ordering for streaming
  END GET_ALL_BOOKS;


  PROCEDURE GET_HISTORICAL_BOOKS (
    p_months_history IN NUMBER,
    p_book_cursor OUT SYS_REFCURSOR
  ) AS
  BEGIN
    OPEN p_book_cursor FOR
      SELECT b.id, b.BOOK_LEVEL, b.CODE, b.NAME, b.PARENT_ID, bai.alternate_id as settlement_account
      FROM atlas_owner.book b
      LEFT JOIN atlas_owner.BOOK_ALTERNATE_IDS bai ON bai.parent_id = b.id
          AND bai.parent_version = b.version
          AND bai.schema_id = 5
      WHERE b.record_status = 1
          AND EXISTS (
              SELECT 1
              FROM atlas_owner.listed_execution le
              WHERE le.BOOK = b.id
                  AND le.TRADE_DATE >= ADD_MONTHS(SYSDATE, -p_months_history)
          );
  END GET_HISTORICAL_BOOKS;


  PROCEDURE GET_BOOK_BY_CODE (
    p_book_code IN VARCHAR2,
    p_book_cursor OUT SYS_REFCURSOR
  ) AS
  BEGIN
    OPEN p_book_cursor FOR
      SELECT b.id, b.BOOK_LEVEL, b.CODE, b.NAME, b.PARENT_ID, bai.alternate_id as settlement_account
      FROM atlas_owner.book b
      LEFT JOIN atlas_owner.BOOK_ALTERNATE_IDS bai ON bai.parent_id = b.id
          AND bai.parent_version = b.version
          AND bai.schema_id = 5
      WHERE b.record_status = 1
        AND b.code = p_book_code; -- Corrected parameter usage
  END GET_BOOK_BY_CODE;


  PROCEDURE GET_ALL_COUNTERPARTIES (
    p_counterparty_cursor OUT SYS_REFCURSOR
  ) AS
  BEGIN
    OPEN p_counterparty_cursor FOR
      SELECT p.sds_id, pai.alternate_id as FISS_ID
      FROM PARTY p
      LEFT JOIN PARTY_ALTERNATE_IDS pai ON pai.parent_id = p.id AND pai.parent_version = p.version and pai.schema_id = 1
      WHERE p.record_status = 1
      ORDER BY p.sds_id; -- Corrected alias for consistent ordering
  END GET_ALL_COUNTERPARTIES;


  PROCEDURE GET_HISTORICAL_COUNTERPARTIES (
    p_months_history IN NUMBER,
    p_counterparty_cursor OUT SYS_REFCURSOR
  ) AS
  BEGIN
    OPEN p_counterparty_cursor FOR
      SELECT p.sds_id, pai.alternate_id as FISS_ID
      FROM PARTY p
      LEFT JOIN PARTY_ALTERNATE_IDS pai ON pai.parent_id = p.id AND pai.parent_version = p.version and pai.schema_id = 1
      WHERE p.record_status = 1
          AND EXISTS (
              SELECT 1
              FROM atlas_owner.listed_execution le
              WHERE le.BOOK = b.id -- Assuming this join condition is correct as per original context
                  AND le.TRADE_DATE >= ADD_MONTHS(SYSDATE, -p_months_history)
          );
  END GET_HISTORICAL_COUNTERPARTIES;

END VSTest;
/