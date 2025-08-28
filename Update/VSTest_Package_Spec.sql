-- VSTest_Package_Spec.sql
-- =======================
--
-- This is the Package Specification for VSTest.
-- It defines the public API for loading reference data from the database
-- for the application's caching layer. Each procedure is designed to
-- efficiently return a large dataset via a SYS_REFCURSOR.
--

CREATE OR REPLACE PACKAGE VSTest AS

  /**
   * Fetches all book records marked as active.
   * Corresponds to BookRepository.findAll()
   * @param p_book_cursor OUT A cursor containing the result set.
   */
  PROCEDURE GET_ALL_BOOKS (
    p_book_cursor OUT SYS_REFCURSOR
  );

  /**
   * Fetches book records that have been used in trades within a specified
   * number of months from the current date.
   * Corresponds to BookRepository.findByHistoricUsageWithCutoff()
   * @param p_months_history IN The look-back period in months.
   * @param p_book_cursor OUT A cursor containing the result set.
   */
  PROCEDURE GET_HISTORICAL_BOOKS (
    p_months_history IN NUMBER,
    p_book_cursor OUT SYS_REFCURSOR
  );

  /**
   * Fetches a single active book record by its unique code.
   * Corresponds to BookRepository.findByCodeId()
   * @param p_book_code IN The code of the book to find.
   * @param p_book_cursor OUT A cursor containing the single result, or empty if not found.
   */
  PROCEDURE GET_BOOK_BY_CODE (
    p_book_code IN VARCHAR2,
    p_book_cursor OUT SYS_REFCURSOR
  );

  /**
   * Fetches all counterparty records marked as active.
   * Corresponds to CounterpartyRepository.findAll()
   * @param p_counterparty_cursor OUT A cursor containing the result set.
   */
  PROCEDURE GET_ALL_COUNTERPARTIES (
    p_counterparty_cursor OUT SYS_REFCURSOR
  );

  /**
   * Fetches counterparty records that have been used in trades within a specified
   * number of months from the current date.
   * Corresponds to CounterpartyRepository.findByHistoricUsageWithCutoff()
   * @param p_months_history IN The look-back period in months.
   * @param p_counterparty_cursor OUT A cursor containing the result set.
   */
  PROCEDURE GET_HISTORICAL_COUNTERPARTIES (
    p_months_history IN NUMBER,
    p_counterparty_cursor OUT SYS_REFCURSOR
  );

END VSTest;
/