package io.github.arthurhoch.kissjson.internal;

import io.github.arthurhoch.kissjson.EnumMode;
import io.github.arthurhoch.kissjson.JsonConfig;
import io.github.arthurhoch.kissjson.JsonException;
import io.github.arthurhoch.kissjson.JsonMappingException;
import io.github.arthurhoch.kissjson.JsonParseException;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ObjectReader {

    private ObjectReader() {
    }

    @SuppressWarnings("unchecked")
    static <T> T parseAndRead(String json, Class<T> type, JsonConfig config) {
        JsonReader reader = new JsonReader(json, config);
        reader.nextToken();
        T result = (T) readValue(reader, type, null, config, new JsonPath());
        reader.ensureFullyConsumed();
        return result;
    }

    @SuppressWarnings("unchecked")
    static <T> List<T> parseAndReadList(String json, Class<T> elementType, JsonConfig config) {
        JsonReader reader = new JsonReader(json, config);
        reader.nextToken();
        if (reader.currentToken() == JsonTokenType.NULL) {
            reader.nextToken();
            reader.ensureFullyConsumed();
            return null;
        }
        if (reader.currentToken() != JsonTokenType.ARRAY_START) {
            throw typeMismatch(reader, List.class, new JsonPath());
        }

        JsonPath path = new JsonPath();
        reader.readArrayStart();
        boolean directObject = isDirectObjectType(elementType);

        if (directObject && !config.failOnUnknownProperties() && !config.failOnMissingRequiredFields()
                && !config.failOnDuplicateKeys() && !config.failOnNullForPrimitives()) {
            ClassModel model = ClassModelCache.get(elementType, config.fieldNaming());
            EnumMode enumMode = config.enumMode();
            boolean isSimple = !model.hasAliases() && !model.hasRequiredFields() && !model.hasDateFields() && !model.hasNestedObjects();
            int nonSimple = 0;
            if (isSimple) {
                for (FieldModel f : model.fields()) {
                    switch (f.fieldType()) {
                        case STRING, CHAR, BOOLEAN, INT, LONG, DOUBLE, FLOAT, SHORT, BYTE,
                             BIG_DECIMAL, BIG_INTEGER, ENUM -> {}
                        default -> { nonSimple++; }
                    }
                }
            }
            if (nonSimple == 0 && isSimple) {
                List<T> result = readSimplePojoList(reader, model, enumMode, config, path);
                reader.readArrayEnd();
                reader.ensureFullyConsumed();
                return result;
            }
        }

        List<T> result = (List<T>) readListRemaining(reader, elementType, config, path);
        reader.readArrayEnd();
        reader.ensureFullyConsumed();
        return result;
    }

    private static <T> List<T> readSimplePojoList(JsonReader reader, ClassModel model, EnumMode enumMode,
                                                  JsonConfig config, JsonPath path) {
        MethodHandle ctor = model.constructorHandle();
        FieldModel[] fields = model.fields();
        Map<String, FieldModel> lookupMap = model.lookupMap();
        int fieldCount = fields.length;
        int capacity = 128;
        List<T> result = new ArrayList<>(capacity);
        int index = 0;

        while (reader.hasNextElement()) {
            Object instance;
            try {
                if (ctor != null) {
                    instance = ctor.invoke();
                } else {
                    instance = model.constructor().newInstance();
                }
            } catch (Throwable e) {
                throw new JsonMappingException(
                        "Failed to create instance of " + model.type().getName() + ": " + e.getMessage(),
                        path.toString(), model.type(), null, null, null
                );
            }

            reader.readObjectStart();

            while (reader.hasNextEntry()) {
                String key = reader.readKey();

                FieldModel fm = lookupMap.get(key);
                if (fm != null) {
                    setFieldFromToken(reader, instance, fm, enumMode, config, path);
                } else {
                    reader.skipValue();
                }

                reader.nextEntryOrEnd();
            }

            reader.readObjectEnd();
            @SuppressWarnings("unchecked")
            T typed = (T) instance;
            result.add(typed);
            reader.nextElementOrEnd();
            index++;
        }
        return result;
    }

    private static void setFieldFromToken(JsonReader r, Object instance, FieldModel fm, EnumMode enumMode,
                                          JsonConfig config, JsonPath path) {
        if (r.currentToken() == JsonTokenType.NULL) {
            r.nextToken();
            if (fm.type().isPrimitive()) {
                setPrimitiveDefault(instance, fm);
            }
            return;
        }

        boolean prim = fm.isPrimitive();
        switch (fm.fieldType()) {
            case STRING: {
                if (r.currentToken() != JsonTokenType.STRING) throw typeMismatch(r, fm.type(), path);
                fm.set(instance, r.stringValue());
                r.nextToken();
                return;
            }
            case CHAR: {
                if (r.currentToken() != JsonTokenType.STRING) throw typeMismatch(r, fm.type(), path);
                String s = r.stringValue();
                r.nextToken();
                char cv = readCharValue(s, fm.type(), path);
                if (prim) fm.setChar(instance, cv); else fm.set(instance, cv);
                return;
            }
            case BOOLEAN: {
                if (r.currentToken() != JsonTokenType.BOOLEAN) throw typeMismatch(r, fm.type(), path);
                boolean bv = r.boolValue();
                r.nextToken();
                if (prim) fm.setBoolean(instance, bv); else fm.set(instance, bv);
                return;
            }
            case INT: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                long lv = readIntegralLong(r, fm.type(), path);
                if (lv < Integer.MIN_VALUE || lv > Integer.MAX_VALUE) {
                    throw new JsonMappingException("Numeric overflow: " + lv + " is out of range for int",
                            path.toString(), fm.type(), null, fm.type(), lv);
                }
                int iv = (int) lv;
                r.nextToken();
                if (prim) fm.setInt(instance, iv); else fm.set(instance, iv);
                return;
            }
            case LONG: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                long lv = readIntegralLong(r, fm.type(), path);
                r.nextToken();
                if (prim) fm.setLong(instance, lv); else fm.set(instance, lv);
                return;
            }
            case DOUBLE: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                double dv = r.doubleValue();
                r.nextToken();
                if (prim) fm.setDouble(instance, dv); else fm.set(instance, dv);
                return;
            }
            case FLOAT: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                float fv = r.isIntegral() ? (float) r.longValue() : r.decimalValue().floatValue();
                r.nextToken();
                if (prim) fm.setFloat(instance, fv); else fm.set(instance, fv);
                return;
            }
            case SHORT: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                long lv = readIntegralLong(r, fm.type(), path);
                if (lv < Short.MIN_VALUE || lv > Short.MAX_VALUE) {
                    throw new JsonMappingException("Numeric overflow: " + lv + " is out of range for short",
                            path.toString(), fm.type(), null, fm.type(), lv);
                }
                short sv = (short) lv;
                r.nextToken();
                if (prim) fm.setShort(instance, sv); else fm.set(instance, sv);
                return;
            }
            case BYTE: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                long lv = readIntegralLong(r, fm.type(), path);
                if (lv < Byte.MIN_VALUE || lv > Byte.MAX_VALUE) {
                    throw new JsonMappingException("Numeric overflow: " + lv + " is out of range for byte",
                            path.toString(), fm.type(), null, fm.type(), lv);
                }
                byte bv = (byte) lv;
                r.nextToken();
                if (prim) fm.setByte(instance, bv); else fm.set(instance, bv);
                return;
            }
            case BIG_DECIMAL: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                fm.set(instance, r.decimalValue());
                r.nextToken();
                return;
            }
            case BIG_INTEGER: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                fm.set(instance, readBigIntegerValue(r, fm.type(), path));
                r.nextToken();
                return;
            }
            case ENUM:
                fm.set(instance, readEnum(r, fm.type(), enumMode, path));
                return;
            case ARRAY:
                fm.set(instance, readArray(r, fm.type().getComponentType(), config, path));
                return;
            case LIST:
                fm.set(instance, readListValue(r, extractElementType(fm.genericType()), config, path));
                return;
            case MAP:
                fm.set(instance, readMapValue(r, extractValueType(fm.genericType()), config, path));
                return;
            case DATE:
                fm.set(instance, readDate(r, fm.type(), config, null, path));
                return;
            case OBJECT:
            default:
                fm.set(instance, readObject(r, fm.type(), config, path));
                return;
        }
    }

    private static void setPrimitiveDefault(Object instance, FieldModel fm) {
        switch (fm.fieldType()) {
            case INT -> fm.setInt(instance, 0);
            case LONG -> fm.setLong(instance, 0L);
            case DOUBLE -> fm.setDouble(instance, 0.0);
            case FLOAT -> fm.setFloat(instance, 0.0f);
            case SHORT -> fm.setShort(instance, (short) 0);
            case BYTE -> fm.setByte(instance, (byte) 0);
            case BOOLEAN -> fm.setBoolean(instance, false);
            case CHAR -> fm.setChar(instance, '\0');
            default -> fm.set(instance, null);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> Map<String, T> parseAndReadMap(String json, Class<T> valueType, JsonConfig config) {
        JsonReader reader = new JsonReader(json, config);
        reader.nextToken();
        Map<String, T> result = (Map<String, T>) readMapValue(reader, valueType, config, new JsonPath());
        reader.ensureFullyConsumed();
        return result;
    }

    static Map<String, Object> parseAndReadMapUntyped(String json, JsonConfig config) {
        JsonReader reader = new JsonReader(json, config);
        reader.nextToken();
        Map<String, Object> result = readMapUntyped(reader, config, new JsonPath());
        reader.ensureFullyConsumed();
        return result;
    }

    private static Object readValue(JsonReader r, Class<?> type, Type genericType, JsonConfig config, JsonPath path) {
        if (r.currentToken() == JsonTokenType.NULL) {
            r.nextToken();
            return handleNull(type, config.failOnNullForPrimitives(), path);
        }

        if (r.currentToken() == JsonTokenType.END) {
            throw r.parseError("Unexpected end of input");
        }

        if ((r.currentToken() == JsonTokenType.OBJECT_START || r.currentToken() == JsonTokenType.ARRAY_START)
                && !isContainerType(type)) {
            r.incrementAndCheckDepth();
            r.decrementDepth();
        }

        if (type == String.class) {
            expectToken(r, JsonTokenType.STRING, type, path);
            String val = r.stringValue();
            r.nextToken();
            return val;
        }

        if (type == char.class || type == Character.class) {
            if (r.currentToken() != JsonTokenType.STRING) {
                throw typeMismatch(r, type, path);
            }
            String s = r.stringValue();
            r.nextToken();
            return readCharValue(s, type, path);
        }

        if (type == boolean.class || type == Boolean.class) {
            expectToken(r, JsonTokenType.BOOLEAN, type, path);
            boolean val = r.boolValue();
            r.nextToken();
            return val;
        }

        if (type == int.class || type == Integer.class) {
            expectToken(r, JsonTokenType.NUMBER, type, path);
            long lv = readIntegralLong(r, type, path);
            if (lv < Integer.MIN_VALUE || lv > Integer.MAX_VALUE) {
                throw new JsonMappingException(
                        "Numeric overflow: " + lv + " is out of range for int",
                        path.toString(), type, null, type, lv);
            }
            int val = (int) lv;
            r.nextToken();
            return val;
        }

        if (type == long.class || type == Long.class) {
            expectToken(r, JsonTokenType.NUMBER, type, path);
            long val = readIntegralLong(r, type, path);
            r.nextToken();
            return val;
        }

        if (type == double.class || type == Double.class) {
            expectToken(r, JsonTokenType.NUMBER, type, path);
            double val = r.doubleValue();
            r.nextToken();
            return val;
        }

        if (type == float.class || type == Float.class) {
            expectToken(r, JsonTokenType.NUMBER, type, path);
            float val;
            if (r.isIntegral()) {
                val = (float) r.longValue();
            } else {
                val = r.decimalValue().floatValue();
            }
            r.nextToken();
            return val;
        }

        if (type == short.class || type == Short.class) {
            expectToken(r, JsonTokenType.NUMBER, type, path);
            long lv = readIntegralLong(r, type, path);
            if (lv < Short.MIN_VALUE || lv > Short.MAX_VALUE) {
                throw new JsonMappingException(
                        "Numeric overflow: " + lv + " is out of range for short",
                        path.toString(), type, null, type, lv);
            }
            short val = (short) lv;
            r.nextToken();
            return val;
        }

        if (type == byte.class || type == Byte.class) {
            expectToken(r, JsonTokenType.NUMBER, type, path);
            long lv = readIntegralLong(r, type, path);
            if (lv < Byte.MIN_VALUE || lv > Byte.MAX_VALUE) {
                throw new JsonMappingException(
                        "Numeric overflow: " + lv + " is out of range for byte",
                        path.toString(), type, null, type, lv);
            }
            byte val = (byte) lv;
            r.nextToken();
            return val;
        }

        if (type == BigDecimal.class) {
            expectToken(r, JsonTokenType.NUMBER, type, path);
            BigDecimal val = r.decimalValue();
            r.nextToken();
            return val;
        }

        if (type == BigInteger.class) {
            expectToken(r, JsonTokenType.NUMBER, type, path);
            BigInteger val = readBigIntegerValue(r, type, path);
            r.nextToken();
            return val;
        }

        if (type.isEnum()) {
            return readEnum(r, type, config, path);
        }

        if (type.isArray()) {
            return readArray(r, type.getComponentType(), config, path);
        }

        if (List.class.isAssignableFrom(type)) {
            Class<?> elementType = extractElementType(genericType);
            return readListValue(r, elementType, config, path);
        }

        if (Map.class.isAssignableFrom(type)) {
            Class<?> mapValueType = extractValueType(genericType);
            return readMapValue(r, mapValueType, config, path);
        }

        if (DateCodec.isDateType(type)) {
            return readDate(r, type, config, null, path);
        }

        if (type == Object.class) {
            return readUntyped(r, config, path);
        }

        return readObject(r, type, config, path);
    }

    private static Object readObject(JsonReader r, Class<?> type, JsonConfig config, JsonPath path) {
        ClassModel model = ClassModelCache.get(type, config.fieldNaming());
        return readObject(r, type, config, path, model);
    }

    private static Object readObject(JsonReader r, Class<?> type, JsonConfig config, JsonPath path, ClassModel model) {
        if (r.currentToken() != JsonTokenType.OBJECT_START) {
            throw typeMismatch(r, type, path);
        }

        Object instance = newInstance(model, path);

        r.readObjectStart();

        boolean failOnUnknown = config.failOnUnknownProperties();
        boolean failOnMissing = config.failOnMissingRequiredFields();
        boolean checkDuplicates = config.failOnDuplicateKeys();
        boolean needMatchedKeys = failOnUnknown || failOnMissing || checkDuplicates;
        boolean hasAliases = model.hasAliases();
        Set<String> matchedKeys = needMatchedKeys ? new LinkedHashSet<>() : null;
        boolean[] matchedIndices = (failOnMissing && model.hasRequiredFields()) ? new boolean[model.fields().length] : null;
        Set<FieldModel> matchedFields = hasAliases ? new HashSet<>() : null;
        Map<String, FieldModel> lookupMap = model.lookupMap();
        EnumMode enumMode = config.enumMode();
        boolean failOnNullPrimitives = config.failOnNullForPrimitives();
        FieldModel[] modelFields = model.fields();

        while (r.hasNextEntry()) {
            String key = r.readKey();

            if (checkDuplicates && matchedKeys.contains(key)) {
                throw r.parseError("Duplicate key '" + key + "'");
            }
            if (needMatchedKeys) {
                matchedKeys.add(key);
            }

            FieldModel fm = lookupMap.get(key);
            if (fm != null && hasAliases && matchedFields.contains(fm) && !fm.primaryName().equals(key)) {
                fm = null;
            }

            if (fm != null) {
                if (hasAliases) {
                    matchedFields.add(fm);
                }
                if (matchedIndices != null) {
                    matchedIndices[fm.index()] = true;
                }

                Object result;
                int mark = path.pushField(fm.primaryName());
                try {
                    if (r.currentToken() == JsonTokenType.NULL) {
                        r.nextToken();
                        result = handleNull(fm.type(), failOnNullPrimitives, path);
                    } else {
                        result = readFieldValue(r, fm, config, enumMode, path);
                    }
                } finally {
                    path.restore(mark);
                }

                fm.setValue(instance, result);
            } else {
                if (failOnUnknown) {
                    throw new JsonMappingException(
                            "Unknown property '" + key + "'",
                            path.toString(), type, key, null, null
                    );
                }
                r.skipValue();
            }

            r.nextEntryOrEnd();
        }

        r.readObjectEnd();

        if (failOnMissing) {
            checkRequiredFields(modelFields, matchedKeys, matchedIndices, path, type);
        }

        return instance;
    }

    private static Object newInstance(ClassModel model, JsonPath path) {
        MethodHandle ctor = model.constructorHandle();
        if (ctor != null) {
            try {
                return ctor.invoke();
            } catch (Throwable e) {
                throw new JsonMappingException(
                        "Failed to create instance of " + model.type().getName() + ": " + e.getMessage(),
                        path.toString(), model.type(), null, null, null
                );
            }
        }
        try {
            return model.constructor().newInstance();
        } catch (Exception e) {
            throw new JsonMappingException(
                    "Failed to create instance of " + model.type().getName() + ": " + e.getMessage(),
                    path.toString(), model.type(), null, null, null
            );
        }
    }

    private static void checkRequiredFields(FieldModel[] modelFields, Set<String> matchedKeys, boolean[] matchedIndices, JsonPath path, Class<?> type) {
        for (FieldModel f : modelFields) {
            if (f.isRequired()) {
                boolean found = matchedIndices != null && matchedIndices[f.index()];
                if (!found && matchedKeys != null) {
                    found = matchedKeys.contains(f.primaryName());
                    if (!found) {
                        for (String alias : f.aliases()) {
                            if (matchedKeys.contains(alias)) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
                if (!found) {
                    throw new JsonMappingException(
                            "Required field '" + f.primaryName() + "' is missing",
                            path.toString(), type, f.primaryName(), f.type(), null
                    );
                }
            }
        }
    }

    private static Object readFieldValue(JsonReader r, FieldModel fm, JsonConfig config, EnumMode enumMode, JsonPath path) {
        if (fm.dateFormat() != null && fm.isDateType()) {
            return readDate(r, fm.type(), config, fm.dateFormat(), path);
        }

        if (r.currentToken() == JsonTokenType.END) {
            throw r.parseError("Unexpected end of input");
        }

        if ((r.currentToken() == JsonTokenType.OBJECT_START || r.currentToken() == JsonTokenType.ARRAY_START)
                && fm.fieldType() != FieldModel.FieldType.OBJECT && fm.fieldType() != FieldModel.FieldType.ARRAY
                && fm.fieldType() != FieldModel.FieldType.LIST && fm.fieldType() != FieldModel.FieldType.MAP) {
            r.incrementAndCheckDepth();
            r.decrementDepth();
        }

        switch (fm.fieldType()) {
            case STRING: {
                if (r.currentToken() != JsonTokenType.STRING) throw typeMismatch(r, fm.type(), path);
                String val = r.stringValue();
                r.nextToken();
                return val;
            }
            case CHAR: {
                if (r.currentToken() != JsonTokenType.STRING) throw typeMismatch(r, fm.type(), path);
                String s = r.stringValue();
                r.nextToken();
                return readCharValue(s, fm.type(), path);
            }
            case BOOLEAN: {
                if (r.currentToken() != JsonTokenType.BOOLEAN) throw typeMismatch(r, fm.type(), path);
                boolean val = r.boolValue();
                r.nextToken();
                return val;
            }
            case INT: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                long lv = readIntegralLong(r, fm.type(), path);
                if (lv < Integer.MIN_VALUE || lv > Integer.MAX_VALUE) {
                    throw new JsonMappingException("Numeric overflow: " + lv + " is out of range for int",
                            path.toString(), fm.type(), null, fm.type(), lv);
                }
                r.nextToken();
                return (int) lv;
            }
            case LONG: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                long val = readIntegralLong(r, fm.type(), path);
                r.nextToken();
                return val;
            }
            case DOUBLE: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                double val = r.doubleValue();
                r.nextToken();
                return val;
            }
            case FLOAT: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                float val = r.isIntegral() ? (float) r.longValue() : r.decimalValue().floatValue();
                r.nextToken();
                return val;
            }
            case SHORT: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                long lv = readIntegralLong(r, fm.type(), path);
                if (lv < Short.MIN_VALUE || lv > Short.MAX_VALUE) {
                    throw new JsonMappingException("Numeric overflow: " + lv + " is out of range for short",
                            path.toString(), fm.type(), null, fm.type(), lv);
                }
                r.nextToken();
                return (short) lv;
            }
            case BYTE: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                long lv = readIntegralLong(r, fm.type(), path);
                if (lv < Byte.MIN_VALUE || lv > Byte.MAX_VALUE) {
                    throw new JsonMappingException("Numeric overflow: " + lv + " is out of range for byte",
                            path.toString(), fm.type(), null, fm.type(), lv);
                }
                r.nextToken();
                return (byte) lv;
            }
            case BIG_DECIMAL: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                BigDecimal val = r.decimalValue();
                r.nextToken();
                return val;
            }
            case BIG_INTEGER: {
                if (r.currentToken() != JsonTokenType.NUMBER) throw typeMismatch(r, fm.type(), path);
                BigInteger val = readBigIntegerValue(r, fm.type(), path);
                r.nextToken();
                return val;
            }
            case ENUM:
                return readEnum(r, fm.type(), enumMode, path);
            case ARRAY:
                return readArray(r, fm.type().getComponentType(), config, path);
            case LIST:
                return readListValue(r, extractElementType(fm.genericType()), config, path);
            case MAP:
                return readMapValue(r, extractValueType(fm.genericType()), config, path);
            case DATE:
                return readDate(r, fm.type(), config, null, path);
            case OBJECT:
            default:
                return readObject(r, fm.type(), config, path);
        }
    }

    private static List<?> readListValue(JsonReader r, Class<?> elementType, JsonConfig config, JsonPath path) {
        if (r.currentToken() == JsonTokenType.NULL) {
            r.nextToken();
            return null;
        }
        if (r.currentToken() != JsonTokenType.ARRAY_START) {
            throw typeMismatch(r, List.class, path);
        }

        r.readArrayStart();
        List<Object> result = new ArrayList<>();
        int index = 0;
        boolean directObjectElement = isDirectObjectType(elementType);
        ClassModel elementModel = null;

        while (r.hasNextElement()) {
            int mark = path.pushIndex(index);
            try {
                if (directObjectElement && r.currentToken() != JsonTokenType.NULL) {
                    if (elementModel == null) {
                        elementModel = ClassModelCache.get(elementType, config.fieldNaming());
                    }
                    result.add(readObject(r, elementType, config, path, elementModel));
                } else {
                    result.add(readValue(r, elementType, null, config, path));
                }
            } finally {
                path.restore(mark);
            }
            r.nextElementOrEnd();
            index++;
        }

        r.readArrayEnd();
        return result;
    }

    private static List<?> readListRemaining(JsonReader r, Class<?> elementType, JsonConfig config, JsonPath path) {
        List<Object> result = new ArrayList<>();
        int index = 0;
        boolean directObjectElement = isDirectObjectType(elementType);
        ClassModel elementModel = directObjectElement ? ClassModelCache.get(elementType, config.fieldNaming()) : null;

        while (r.hasNextElement()) {
            int mark = path.pushIndex(index);
            try {
                if (directObjectElement && r.currentToken() != JsonTokenType.NULL) {
                    result.add(readObject(r, elementType, config, path, elementModel));
                } else {
                    result.add(readValue(r, elementType, null, config, path));
                }
            } finally {
                path.restore(mark);
            }
            r.nextElementOrEnd();
            index++;
        }
        return result;
    }

    private static Map<String, ?> readMapValue(JsonReader r, Class<?> valueType, JsonConfig config, JsonPath path) {
        if (r.currentToken() == JsonTokenType.NULL) {
            r.nextToken();
            return null;
        }
        if (r.currentToken() != JsonTokenType.OBJECT_START) {
            throw typeMismatch(r, Map.class, path);
        }

        r.readObjectStart();
        Map<String, Object> result = new LinkedHashMap<>();
        Set<String> seenKeys = r.failOnDuplicateKeys() ? new LinkedHashSet<>() : null;
        boolean directObjectValue = isDirectObjectType(valueType);
        ClassModel valueModel = null;

        while (r.hasNextEntry()) {
            String key = r.readKey();
            checkDuplicateKey(r, seenKeys, key);
            int mark = path.pushField(key);
            try {
                if (directObjectValue && r.currentToken() != JsonTokenType.NULL) {
                    if (valueModel == null) {
                        valueModel = ClassModelCache.get(valueType, config.fieldNaming());
                    }
                    result.put(key, readObject(r, valueType, config, path, valueModel));
                } else {
                    result.put(key, readValue(r, valueType, null, config, path));
                }
            } finally {
                path.restore(mark);
            }
            r.nextEntryOrEnd();
        }

        r.readObjectEnd();
        return result;
    }

    private static Map<String, Object> readMapUntyped(JsonReader r, JsonConfig config, JsonPath path) {
        if (r.currentToken() == JsonTokenType.NULL) {
            r.nextToken();
            return null;
        }
        if (r.currentToken() != JsonTokenType.OBJECT_START) {
            throw typeMismatch(r, Map.class, path);
        }

        r.readObjectStart();
        Map<String, Object> result = new LinkedHashMap<>();
        Set<String> seenKeys = r.failOnDuplicateKeys() ? new LinkedHashSet<>() : null;

        while (r.hasNextEntry()) {
            String key = r.readKey();
            checkDuplicateKey(r, seenKeys, key);
            int mark = path.pushField(key);
            try {
                result.put(key, readUntyped(r, config, path));
            } finally {
                path.restore(mark);
            }
            r.nextEntryOrEnd();
        }

        r.readObjectEnd();
        return result;
    }

    private static Object readUntyped(JsonReader r, JsonConfig config, JsonPath path) {
        switch (r.currentToken()) {
            case NULL:
                r.nextToken();
                return null;
            case BOOLEAN:
                boolean b = r.boolValue();
                r.nextToken();
                return b;
            case NUMBER:
                if (r.isIntegral()) {
                    long lv = r.longValue();
                    r.nextToken();
                    return lv;
                }
                double dv = r.doubleValue();
                r.nextToken();
                return dv;
            case STRING:
                String s = r.stringValue();
                r.nextToken();
                return s;
            case ARRAY_START:
                r.readArrayStart();
                List<Object> list = new ArrayList<>();
                int idx = 0;
                while (r.hasNextElement()) {
                    int mark = path.pushIndex(idx);
                    try {
                        list.add(readUntyped(r, config, path));
                    } finally {
                        path.restore(mark);
                    }
                    r.nextElementOrEnd();
                    idx++;
                }
                r.readArrayEnd();
                return list;
            case OBJECT_START:
                return readMapUntyped(r, config, path);
            default:
                throw new JsonException("Unexpected token in untyped value: " + r.currentToken() + " at " + path);
        }
    }

    private static Object readEnum(JsonReader r, Class<?> type, JsonConfig config, JsonPath path) {
        return readEnum(r, type, config.enumMode(), path);
    }

    private static Object readEnum(JsonReader r, Class<?> type, EnumMode enumMode, JsonPath path) {
        if (r.currentToken() != JsonTokenType.STRING) {
            throw typeMismatch(r, type, path);
        }
        String str = r.stringValue();
        r.nextToken();

        Object[] constants = type.getEnumConstants();
        if (constants == null) {
            throw new JsonMappingException("Not an enum type: " + type.getName(), path.toString(), type, null, type, str);
        }

        if (enumMode == EnumMode.TO_STRING) {
            for (Object c : constants) {
                if (c.toString().equals(str)) return c;
            }
        } else {
            for (Object c : constants) {
                if (((Enum<?>) c).name().equals(str)) return c;
            }
        }

        throw new JsonMappingException(
                "Unknown enum value '" + str + "' for " + type.getName(),
                path.toString(), type, null, type, str
        );
    }

    private static Object readArray(JsonReader r, Class<?> componentType, JsonConfig config, JsonPath path) {
        if (r.currentToken() != JsonTokenType.ARRAY_START) {
            throw typeMismatch(r, componentType, path);
        }

        r.readArrayStart();
        List<Object> elements = new ArrayList<>();
        int index = 0;

        while (r.hasNextElement()) {
            int mark = path.pushIndex(index);
            try {
                elements.add(readValue(r, componentType, null, config, path));
            } finally {
                path.restore(mark);
            }
            r.nextElementOrEnd();
            index++;
        }

        r.readArrayEnd();

        Object array = Array.newInstance(componentType, elements.size());
        for (int i = 0; i < elements.size(); i++) {
            Array.set(array, i, elements.get(i));
        }
        return array;
    }

    private static Object readDate(JsonReader r, Class<?> type, JsonConfig config, String pattern, JsonPath path) {
        Object rawValue;
        if (r.currentToken() == JsonTokenType.NUMBER) {
            rawValue = r.isIntegral() ? r.longValue() : r.doubleValue();
            r.nextToken();
        } else if (r.currentToken() == JsonTokenType.STRING) {
            rawValue = r.stringValue();
            r.nextToken();
        } else {
            throw new JsonMappingException(
                    "Expected number or string for date/time type " + type.getName(),
                    path.toString(), type, null, type, r.currentToken().name()
            );
        }
        try {
            return DateCodec.deserialize(rawValue, type, config.dateFormat(), config.zoneId(), pattern);
        } catch (JsonMappingException e) {
            throw new JsonMappingException(
                    e.getMessage(),
                    path.toString(), type, null, type, rawValue, e
            );
        }
    }

    private static Object handleNull(Class<?> type, boolean failOnNullPrimitives, JsonPath path) {
        if (!type.isPrimitive()) return null;
        if (failOnNullPrimitives) {
            throw new JsonMappingException(
                    "Cannot assign null to primitive " + type.getName(),
                    path.toString(), type, null, type, null
            );
        }
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        return 0;
    }

    private static char readCharValue(String value, Class<?> targetType, JsonPath path) {
        if (value.length() != 1) {
            throw new JsonMappingException(
                    "Expected single-character string for " + targetType.getName(),
                    path.toString(), targetType, null, targetType, value
            );
        }
        return value.charAt(0);
    }

    private static long readIntegralLong(JsonReader r, Class<?> targetType, JsonPath path) {
        BigDecimal value = r.decimalValue();
        try {
            value.toBigIntegerExact();
        } catch (ArithmeticException e) {
            throw new JsonMappingException(
                    "Cannot map decimal number to integer type " + targetType.getName(),
                    path.toString(), targetType, null, targetType, value, e
            );
        }
        try {
            return value.longValueExact();
        } catch (ArithmeticException e) {
            throw new JsonMappingException(
                    "Numeric overflow: " + value + " is out of range for long",
                    path.toString(), targetType, null, targetType, value, e
            );
        }
    }

    private static BigInteger readBigIntegerValue(JsonReader r, Class<?> targetType, JsonPath path) {
        try {
            return r.decimalValue().toBigIntegerExact();
        } catch (ArithmeticException e) {
            throw new JsonMappingException(
                    "Cannot map decimal number to BigInteger",
                    path.toString(), targetType, null, targetType, r.decimalValue(), e
            );
        }
    }

    private static Class<?> extractElementType(Type genericType) {
        if (genericType instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0) {
                if (args[0] instanceof Class<?> c) return c;
                if (args[0] instanceof ParameterizedType inner) {
                    Type raw = inner.getRawType();
                    if (raw instanceof Class<?> c) return c;
                }
            }
        }
        return Object.class;
    }

    private static Class<?> extractValueType(Type genericType) {
        if (genericType instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 1) {
                if (args[1] instanceof Class<?> c) return c;
                if (args[1] instanceof ParameterizedType inner) {
                    Type raw = inner.getRawType();
                    if (raw instanceof Class<?> c) return c;
                }
            }
        }
        return Object.class;
    }

    private static boolean isContainerType(Class<?> type) {
        return !type.isPrimitive() && type != Object.class
                && (type.isArray()
                    || List.class.isAssignableFrom(type)
                    || Map.class.isAssignableFrom(type)
                    || !type.getName().startsWith("java."));
    }

    private static void checkDuplicateKey(JsonReader r, Set<String> seenKeys, String key) {
        if (seenKeys != null && !seenKeys.add(key)) {
            throw r.parseError("Duplicate key '" + key + "'");
        }
    }

    private static boolean isDirectObjectType(Class<?> type) {
        return type != Object.class
                && !type.isPrimitive()
                && !type.isArray()
                && type != String.class
                && type != Character.class
                && type != Boolean.class
                && !Number.class.isAssignableFrom(type)
                && !type.isEnum()
                && !DateCodec.isDateType(type)
                && !List.class.isAssignableFrom(type)
                && !Map.class.isAssignableFrom(type);
    }

    private static void expectToken(JsonReader r, JsonTokenType expected, Class<?> targetType, JsonPath path) {
        if (r.currentToken() != expected) {
            throw typeMismatch(r, targetType, path);
        }
    }

    private static JsonMappingException typeMismatch(JsonReader r, Class<?> targetType, JsonPath path) {
        String pathString = path.toString();
        return new JsonMappingException(
                "Cannot map " + r.currentToken() + " to " + targetType.getName() + " at " + pathString,
                pathString, targetType, null, targetType, r.currentToken().name()
        );
    }

    private static final class JsonPath {
        private Object[] stack = new Object[16];
        private int size;

        int pushField(String key) {
            int mark = size;
            if (size >= stack.length) {
                stack = java.util.Arrays.copyOf(stack, stack.length * 2);
            }
            stack[size++] = key;
            return mark;
        }

        int pushIndex(int index) {
            int mark = size;
            if (size >= stack.length) {
                stack = java.util.Arrays.copyOf(stack, stack.length * 2);
            }
            stack[size++] = index;
            return mark;
        }

        void restore(int mark) {
            size = mark;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('$');
            for (int i = 0; i < size; i++) {
                Object seg = stack[i];
                if (seg instanceof Integer idx) {
                    sb.append('[').append(idx).append(']');
                } else {
                    sb.append('.').append(seg);
                }
            }
            return sb.toString();
        }
    }
}
