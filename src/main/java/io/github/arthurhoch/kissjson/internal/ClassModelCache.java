package io.github.arthurhoch.kissjson.internal;

import io.github.arthurhoch.kissjson.FieldNaming;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

final class ClassModelCache {

    private static final ConcurrentHashMap<CacheKey, ClassModel> CACHE = new ConcurrentHashMap<>();

    private ClassModelCache() {
    }

    public static ClassModel get(Class<?> clazz, FieldNaming naming) {
        CacheKey key = new CacheKey(clazz, naming);
        return CACHE.computeIfAbsent(key, k -> new ClassModel(k.clazz, k.naming));
    }

    private static final class CacheKey {
        final Class<?> clazz;
        final FieldNaming naming;
        final int hash;

        CacheKey(Class<?> clazz, FieldNaming naming) {
            this.clazz = clazz;
            this.naming = naming;
            this.hash = 31 * clazz.hashCode() + naming.hashCode();
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CacheKey other)) return false;
            return clazz == other.clazz && naming == other.naming;
        }
    }
}
