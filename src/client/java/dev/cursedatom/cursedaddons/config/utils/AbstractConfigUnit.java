package dev.cursedatom.cursedaddons.config.utils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractConfigUnit {

    // Cache field arrays to avoid repeated reflection overhead
    private static final Map<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * Gets cached fields for a class, making them accessible.
     */
    private static Field[] getCachedFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, c -> {
            Field[] fields = c.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
            }
            return fields;
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends AbstractConfigUnit> T of(Object element, Class<T> clazz) {
        if (element instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) element;
            try {
                T instance = clazz.getDeclaredConstructor().newInstance();
                for (Field field : getCachedFields(clazz)) {
                    String fieldName = field.getName();
                    Object value = map.getOrDefault(fieldName, getDefaultValue(field));
                    if (value != null) {
                        if (field.getType().isEnum() && value instanceof String) {
                            // Handle enum deserialization
                            Enum<?> enumValue = (Enum<?>) Enum.valueOf((Class<? extends Enum>) field.getType(), (String) value);
                            field.set(instance, enumValue);
                        } else {
                            field.set(instance, value);
                        }
                    }
                }
                return instance;
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize " + clazz.getSimpleName(), e);
            }
        } else if (clazz.isInstance(element)) {
            return (T) element;
        } else {
            throw new IllegalArgumentException("Unexpected element type of Object: " + element);
        }
    }

    public static <T extends AbstractConfigUnit> List<T> fromList(List<Object> list, Class<T> clazz) {
        List<T> arr = new ArrayList<>();
        for (Object ele : list) {
            arr.add(of(ele, clazz));
        }
        return arr;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        for (Field field : getCachedFields(this.getClass())) {
            try {
                Object value = field.get(this);
                if (value != null && field.getType().isEnum()) {
                    map.put(field.getName(), ((Enum<?>) value).name());
                } else {
                    map.put(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to serialize field " + field.getName(), e);
            }
        }
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        try {
            for (Field field : getCachedFields(getClass())) {
                Object thisValue = field.get(this);
                Object otherValue = field.get(o);
                if (!Objects.equals(thisValue, otherValue)) {
                    return false;
                }
            }
            return true;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to compare objects", e);
        }
    }

    @Override
    public int hashCode() {
        List<Object> values = new ArrayList<>();
        try {
            for (Field field : getCachedFields(getClass())) {
                values.add(field.get(this));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to compute hashCode", e);
        }
        return Objects.hash(values.toArray());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append("{");
        try {
            boolean first = true;
            for (Field field : getCachedFields(getClass())) {
                if (!first) sb.append(", ");
                sb.append(field.getName()).append("='").append(field.get(this)).append("'");
                first = false;
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to generate toString", e);
        }
        sb.append("}");
        return sb.toString();
    }

    private static Object getDefaultValue(Field field) {
        Class<?> type = field.getType();
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == String.class) return "";
        return null;
    }
}
