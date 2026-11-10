package com.elfmcys.yesstevemodel.util;

import java.util.concurrent.atomic.AtomicInteger;

/** IDs for GUI-only entities, which are never added to a world entity manager. */
public final class PreviewEntityIds {
    private static final AtomicInteger NEXT = new AtomicInteger(Integer.MIN_VALUE);

    private PreviewEntityIds() {}

    public static int next() {
        return NEXT.getAndUpdate(id -> id == -1 ? Integer.MIN_VALUE : id + 1);
    }
}
