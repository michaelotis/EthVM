/*
 * This file is generated by jOOQ.
 */
package com.ethvm.db.tables.records;


import com.ethvm.db.tables.SyncStatusHistory;

import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.12"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SyncStatusHistoryRecord extends UpdatableRecordImpl<SyncStatusHistoryRecord> implements Record4<String, BigDecimal, Timestamp, Timestamp> {

    private static final long serialVersionUID = -1869472043;

    /**
     * Setter for <code>public.sync_status_history.component</code>.
     */
    public SyncStatusHistoryRecord setComponent(String value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>public.sync_status_history.component</code>.
     */
    public String getComponent() {
        return (String) get(0);
    }

    /**
     * Setter for <code>public.sync_status_history.block_number</code>.
     */
    public SyncStatusHistoryRecord setBlockNumber(BigDecimal value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>public.sync_status_history.block_number</code>.
     */
    public BigDecimal getBlockNumber() {
        return (BigDecimal) get(1);
    }

    /**
     * Setter for <code>public.sync_status_history.timestamp</code>.
     */
    public SyncStatusHistoryRecord setTimestamp(Timestamp value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>public.sync_status_history.timestamp</code>.
     */
    public Timestamp getTimestamp() {
        return (Timestamp) get(2);
    }

    /**
     * Setter for <code>public.sync_status_history.block_timestamp</code>.
     */
    public SyncStatusHistoryRecord setBlockTimestamp(Timestamp value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>public.sync_status_history.block_timestamp</code>.
     */
    public Timestamp getBlockTimestamp() {
        return (Timestamp) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record2<String, BigDecimal> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row4<String, BigDecimal, Timestamp, Timestamp> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row4<String, BigDecimal, Timestamp, Timestamp> valuesRow() {
        return (Row4) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field1() {
        return SyncStatusHistory.SYNC_STATUS_HISTORY.COMPONENT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<BigDecimal> field2() {
        return SyncStatusHistory.SYNC_STATUS_HISTORY.BLOCK_NUMBER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Timestamp> field3() {
        return SyncStatusHistory.SYNC_STATUS_HISTORY.TIMESTAMP;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Timestamp> field4() {
        return SyncStatusHistory.SYNC_STATUS_HISTORY.BLOCK_TIMESTAMP;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component1() {
        return getComponent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal component2() {
        return getBlockNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timestamp component3() {
        return getTimestamp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timestamp component4() {
        return getBlockTimestamp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value1() {
        return getComponent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal value2() {
        return getBlockNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timestamp value3() {
        return getTimestamp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timestamp value4() {
        return getBlockTimestamp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SyncStatusHistoryRecord value1(String value) {
        setComponent(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SyncStatusHistoryRecord value2(BigDecimal value) {
        setBlockNumber(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SyncStatusHistoryRecord value3(Timestamp value) {
        setTimestamp(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SyncStatusHistoryRecord value4(Timestamp value) {
        setBlockTimestamp(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SyncStatusHistoryRecord values(String value1, BigDecimal value2, Timestamp value3, Timestamp value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached SyncStatusHistoryRecord
     */
    public SyncStatusHistoryRecord() {
        super(SyncStatusHistory.SYNC_STATUS_HISTORY);
    }

    /**
     * Create a detached, initialised SyncStatusHistoryRecord
     */
    public SyncStatusHistoryRecord(String component, BigDecimal blockNumber, Timestamp timestamp, Timestamp blockTimestamp) {
        super(SyncStatusHistory.SYNC_STATUS_HISTORY);

        set(0, component);
        set(1, blockNumber);
        set(2, timestamp);
        set(3, blockTimestamp);
    }
}