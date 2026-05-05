package io.github.arthurhoch.kissjson.internal;

import io.github.arthurhoch.kissjson.FieldNaming;
import io.github.arthurhoch.kissjson.JsonMappingException;
import io.github.arthurhoch.kissjson.JsonName;
import io.github.arthurhoch.kissjson.JsonAliases;
import io.github.arthurhoch.kissjson.JsonRequired;
import io.github.arthurhoch.kissjson.JsonIncludeNull;
import io.github.arthurhoch.kissjson.JsonExcludeNull;
import io.github.arthurhoch.kissjson.JsonDateFormat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

final class FieldModel {

    enum FieldType {
        STRING, CHAR, BOOLEAN, INT, LONG, DOUBLE, FLOAT, SHORT, BYTE,
        BIG_DECIMAL, BIG_INTEGER, ENUM, ARRAY, LIST, MAP, DATE, OBJECT
    }

    private final Field field;
    private final MethodHandle getter;
    private final MethodHandle setter;
    private final String primaryName;
    private final String quotedNameColon;
    private final String[] aliases;
    private final Class<?> type;
    private final boolean primitive;
    private final boolean required;
    private final boolean includeNull;
    private final boolean excludeNull;
    private final String dateFormat;
    private final Type genericType;
    private final boolean dateType;
    private final FieldType fieldType;
    private final int index;

    FieldModel(Field field, FieldNaming strategy, int index) {
        this.field = field;
        this.type = field.getType();
        this.primitive = type.isPrimitive();
        this.genericType = field.getGenericType();
        this.dateType = DateCodec.isDateType(type);
        this.index = index;

        field.setAccessible(true);

        MethodHandle getterHandle = null;
        MethodHandle setterHandle = null;
        try {
            getterHandle = MethodHandles.lookup().unreflectGetter(field);
            setterHandle = MethodHandles.lookup().unreflectSetter(field);
        } catch (IllegalAccessException ignored) {
        }
        this.getter = getterHandle;
        this.setter = setterHandle;

        JsonName nameAnn = field.getAnnotation(JsonName.class);
        if (nameAnn != null) {
            this.primaryName = nameAnn.value();
        } else {
            this.primaryName = NamingStrategy.apply(strategy, field.getName());
        }
        StringBuilder quoted = new StringBuilder(this.primaryName.length() + 3);
        JsonWriter.escapeString(this.primaryName, quoted);
        quoted.append(':');
        this.quotedNameColon = quoted.toString();

        JsonAliases aliasesAnn = field.getAnnotation(JsonAliases.class);
        this.aliases = aliasesAnn != null ? aliasesAnn.value() : new String[0];

        this.required = field.isAnnotationPresent(JsonRequired.class);
        this.includeNull = field.isAnnotationPresent(JsonIncludeNull.class);
        this.excludeNull = field.isAnnotationPresent(JsonExcludeNull.class);

        if (this.includeNull && this.excludeNull) {
            throw new JsonMappingException(
                    "Field '" + field.getName() + "' in " + field.getDeclaringClass().getName()
                            + " has both @JsonIncludeNull and @JsonExcludeNull",
                    null, field.getDeclaringClass(), field.getName(), null, null
            );
        }

        JsonDateFormat dateAnn = field.getAnnotation(JsonDateFormat.class);
        this.dateFormat = dateAnn != null ? dateAnn.value() : null;
        this.fieldType = classifyType(type);
    }

    private static FieldType classifyType(Class<?> t) {
        if (t == String.class) return FieldType.STRING;
        if (t == char.class || t == Character.class) return FieldType.CHAR;
        if (t == boolean.class || t == Boolean.class) return FieldType.BOOLEAN;
        if (t == int.class || t == Integer.class) return FieldType.INT;
        if (t == long.class || t == Long.class) return FieldType.LONG;
        if (t == double.class || t == Double.class) return FieldType.DOUBLE;
        if (t == float.class || t == Float.class) return FieldType.FLOAT;
        if (t == short.class || t == Short.class) return FieldType.SHORT;
        if (t == byte.class || t == Byte.class) return FieldType.BYTE;
        if (t == BigDecimal.class) return FieldType.BIG_DECIMAL;
        if (t == BigInteger.class) return FieldType.BIG_INTEGER;
        if (t.isEnum()) return FieldType.ENUM;
        if (t.isArray()) return FieldType.ARRAY;
        if (List.class.isAssignableFrom(t)) return FieldType.LIST;
        if (Map.class.isAssignableFrom(t)) return FieldType.MAP;
        if (DateCodec.isDateType(t)) return FieldType.DATE;
        return FieldType.OBJECT;
    }

    Field field() {
        return field;
    }

    String primaryName() {
        return primaryName;
    }

    String quotedNameColon() {
        return quotedNameColon;
    }

    String[] aliases() {
        return aliases;
    }

    Class<?> type() {
        return type;
    }

    boolean isPrimitive() {
        return primitive;
    }

    boolean isRequired() {
        return required;
    }

    boolean includeNull() {
        return includeNull;
    }

    boolean excludeNull() {
        return excludeNull;
    }

    String dateFormat() {
        return dateFormat;
    }

    Type genericType() {
        return genericType;
    }

    boolean isDateType() {
        return dateType;
    }

    FieldType fieldType() {
        return fieldType;
    }

    Object getValue(Object obj) {
        if (getter != null) {
            try {
                return getter.invoke(obj);
            } catch (Throwable t) {
                return fallbackGet(obj);
            }
        }
        return fallbackGet(obj);
    }

    void setValue(Object obj, Object value) {
        if (setter != null) {
            try {
                setter.invoke(obj, value);
                return;
            } catch (Throwable t) {
                fallbackSet(obj, value);
                return;
            }
        }
        fallbackSet(obj, value);
    }

    private Object fallbackGet(Object obj) {
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot access field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, null
            );
        }
    }

    private void fallbackSet(Object obj, Object value) {
        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot set field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, value
            );
        } catch (IllegalArgumentException e) {
            throw new JsonMappingException(
                    "Type mismatch for field '" + field.getName() + "' on " + obj.getClass().getName()
                            + ": " + e.getMessage(),
                    null, obj.getClass(), field.getName(), type, value
            );
        }
    }

    int getInt(Object obj) {
        try {
            return field.getInt(obj);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot get field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, null
            );
        }
    }

    long getLong(Object obj) {
        try {
            return field.getLong(obj);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot get field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, null
            );
        }
    }

    boolean getBoolean(Object obj) {
        try {
            return field.getBoolean(obj);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot get field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, null
            );
        }
    }

    double getDouble(Object obj) {
        try {
            return field.getDouble(obj);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot get field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, null
            );
        }
    }

    float getFloat(Object obj) {
        try {
            return field.getFloat(obj);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot get field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, null
            );
        }
    }

    short getShort(Object obj) {
        try {
            return field.getShort(obj);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot get field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, null
            );
        }
    }

    byte getByte(Object obj) {
        try {
            return field.getByte(obj);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot get field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, null
            );
        }
    }

    char getChar(Object obj) {
        try {
            return field.getChar(obj);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot get field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, null
            );
        }
    }

    void setInt(Object obj, int value) {
        try {
            field.setInt(obj, value);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot set field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, value
            );
        }
    }

    void setLong(Object obj, long value) {
        try {
            field.setLong(obj, value);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot set field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, value
            );
        }
    }

    void setBoolean(Object obj, boolean value) {
        try {
            field.setBoolean(obj, value);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot set field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, value
            );
        }
    }

    void setDouble(Object obj, double value) {
        try {
            field.setDouble(obj, value);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot set field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, value
            );
        }
    }

    void setFloat(Object obj, float value) {
        try {
            field.setFloat(obj, value);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot set field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, value
            );
        }
    }

    void setShort(Object obj, short value) {
        try {
            field.setShort(obj, value);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot set field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, value
            );
        }
    }

    void setByte(Object obj, byte value) {
        try {
            field.setByte(obj, value);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot set field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, value
            );
        }
    }

    void setChar(Object obj, char value) {
        try {
            field.setChar(obj, value);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot set field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, value
            );
        }
    }

    void set(Object obj, Object value) {
        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new JsonMappingException(
                    "Cannot set field '" + field.getName() + "' on " + obj.getClass().getName(),
                    null, obj.getClass(), field.getName(), type, value
            );
        } catch (IllegalArgumentException e) {
            throw new JsonMappingException(
                    "Type mismatch for field '" + field.getName() + "' on " + obj.getClass().getName()
                            + ": " + e.getMessage(),
                    null, obj.getClass(), field.getName(), type, value
            );
        }
    }

    int index() {
        return index;
    }
}
