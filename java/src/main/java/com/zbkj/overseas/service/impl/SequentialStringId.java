package com.zbkj.overseas.service.impl;

import java.util.Collection;

/**
 * Human-readable sequential ids for the String-id catalogue tables
 * (category / brand). The old generators produced UUID / timestamp
 * gibberish ("c" + UUID, "b" + base36 millis) that admins could not
 * read or dictate. New rule (bug #7/#8): one more than the largest
 * purely numeric id already in the table; when the table has no
 * numeric id at all, start from row count + 1. The result is a plain
 * numeric string, so it can never collide with a legacy non-numeric
 * id, and max+1 can never collide with an existing numeric one.
 * Single-admin back office: no locking, the caller loads the id set
 * and inserts within the same request.
 */
final class SequentialStringId {

    private SequentialStringId() {
    }

    static String next(Collection<String> existingIds) {
        long max = 0;
        boolean hasNumeric = false;
        if (existingIds != null) {
            for (String id : existingIds) {
                if (id != null && id.chars().allMatch(Character::isDigit) && !id.isEmpty()) {
                    try {
                        long v = Long.parseLong(id);
                        if (v > max) {
                            max = v;
                        }
                        hasNumeric = true;
                    } catch (NumberFormatException ignored) {
                        // digit string beyond long range: not one of ours, skip
                    }
                }
            }
        }
        long candidate = hasNumeric ? max + 1 : (existingIds == null ? 1 : existingIds.size() + 1L);
        return String.valueOf(candidate);
    }
}
