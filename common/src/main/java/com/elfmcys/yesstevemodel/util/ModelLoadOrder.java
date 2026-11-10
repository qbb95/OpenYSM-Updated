package com.elfmcys.yesstevemodel.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Present visible-player models first while retaining every catalog entry. */
public final class ModelLoadOrder {
    private ModelLoadOrder() {}

    public static <T> List<T> prioritize(Map<String, T> catalog, Collection<String> visible,
                                       String fallback) {
        var ordered = new LinkedHashMap<String, T>();
        for (String id : visible) {
            T model = catalog.get(id);
            if (model != null) ordered.putIfAbsent(id, model);
        }
        T defaultModel = catalog.get(fallback);
        if (defaultModel != null) ordered.putIfAbsent(fallback, defaultModel);
        catalog.forEach(ordered::putIfAbsent);
        return List.copyOf(ordered.values());
    }
}
