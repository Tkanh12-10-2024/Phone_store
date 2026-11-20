package com.phonestore.dao;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Shared JDBC utilities for DAO classes to bind parameterized queries safely
 * and normalize BigDecimal / date-time values from ResultSets.
 */
public final class DaoJdbcHelper {

    private DaoJdbcHelper() {
    }

    public static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    public static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    public static void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object val = params.get(i);
            int idx = i + 1;
            if (val == null) {
                ps.setNull(idx, Types.VARCHAR);
            } else if (val instanceof BigDecimal bd) {
                ps.setBigDecimal(idx, bd);
            } else if (val instanceof Integer n) {
                ps.setInt(idx, n);
            } else if (val instanceof Long l) {
                ps.setLong(idx, l);
            } else if (val instanceof Timestamp ts) {
                ps.setTimestamp(idx, ts);
            } else if (val instanceof LocalDateTime ldt) {
                ps.setTimestamp(idx, Timestamp.valueOf(ldt));
            } else if (val instanceof Date d) {
                ps.setDate(idx, d);
            } else if (val instanceof LocalDate ld) {
                ps.setDate(idx, Date.valueOf(ld));
            } else {
                ps.setString(idx, String.valueOf(val));
            }
        }
    }
}

