package io.github.arthurhoch.kissjson.internal;

import io.github.arthurhoch.kissjson.FieldNaming;
import io.github.arthurhoch.kissjson.JsonIgnore;
import io.github.arthurhoch.kissjson.JsonMappingException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ClassModel {

    private final Class<?> type;
    private final Constructor<?> constructor;
    private final MethodHandle constructorHandle;
    private final FieldModel[] fields;
    private final Map<String, FieldModel> lookupMap;
    private final String[] fieldNames;
    private final boolean hasAliases;
    private final boolean hasRequiredFields;
    private final boolean hasDateFields;
    private final boolean hasPrimitiveFields;
    private final boolean hasNestedObjects;

    ClassModel(Class<?> clazz, FieldNaming strategy) {
        this.type = clazz;

        List<FieldModel> fieldList = new ArrayList<>();
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            hierarchy.add(current);
            current = current.getSuperclass();
        }

        for (int i = hierarchy.size() - 1; i >= 0; i--) {
            Class<?> level = hierarchy.get(i);
            Field[] declared = level.getDeclaredFields();
            for (Field f : declared) {
                int mod = f.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || f.isSynthetic()) {
                    continue;
                }
                if (f.isAnnotationPresent(JsonIgnore.class)) {
                    continue;
                }
                fieldList.add(new FieldModel(f, strategy, fieldList.size()));
            }
        }

        this.fields = fieldList.toArray(new FieldModel[0]);

        Map<String, FieldModel> map = new HashMap<>();
        String[] names = new String[this.fields.length];
        boolean aliasesPresent = false;
        boolean requiredPresent = false;
        boolean datePresent = false;
        boolean primitivePresent = false;
        boolean nestedPresent = false;
        for (int i = 0; i < this.fields.length; i++) {
            FieldModel fm = this.fields[i];
            names[i] = fm.primaryName();
            map.put(fm.primaryName(), fm);
            for (String alias : fm.aliases()) {
                aliasesPresent = true;
                map.putIfAbsent(alias, fm);
            }
            if (fm.isRequired()) requiredPresent = true;
            if (fm.isDateType() || fm.dateFormat() != null) datePresent = true;
            if (fm.type().isPrimitive()) primitivePresent = true;
            if (fm.fieldType() == FieldModel.FieldType.OBJECT) nestedPresent = true;
        }
        this.lookupMap = Collections.unmodifiableMap(map);
        this.fieldNames = names;
        this.hasAliases = aliasesPresent;
        this.hasRequiredFields = requiredPresent;
        this.hasDateFields = datePresent;
        this.hasPrimitiveFields = primitivePresent;
        this.hasNestedObjects = nestedPresent;

        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            this.constructor = ctor;
            MethodHandle mh = null;
            try {
                mh = MethodHandles.lookup().unreflectConstructor(ctor);
            } catch (IllegalAccessException ignored) {
            }
            this.constructorHandle = mh;
        } catch (NoSuchMethodException e) {
            throw new JsonMappingException(
                    "No no-arg constructor found for " + clazz.getName()
                            + ". KissJson requires a no-arg constructor (can be private).",
                    null, clazz, null, null, null
            );
        }
    }

    Class<?> type() {
        return type;
    }

    Constructor<?> constructor() {
        return constructor;
    }

    MethodHandle constructorHandle() {
        return constructorHandle;
    }

    FieldModel[] fields() {
        return fields;
    }

    Map<String, FieldModel> lookupMap() {
        return lookupMap;
    }

    String[] fieldNames() {
        return fieldNames;
    }

    boolean hasAliases() {
        return hasAliases;
    }

    boolean hasRequiredFields() {
        return hasRequiredFields;
    }

    boolean hasDateFields() {
        return hasDateFields;
    }

    boolean hasPrimitiveFields() {
        return hasPrimitiveFields;
    }

    boolean hasNestedObjects() {
        return hasNestedObjects;
    }
}
